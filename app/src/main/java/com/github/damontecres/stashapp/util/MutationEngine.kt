package com.github.damontecres.stashapp.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.github.damontecres.stashapp.api.CreateMarkerMutation
import com.github.damontecres.stashapp.api.DeleteMarkerMutation
import com.github.damontecres.stashapp.api.ImageDecrementOMutation
import com.github.damontecres.stashapp.api.ImageIncrementOMutation
import com.github.damontecres.stashapp.api.ImageResetOMutation
import com.github.damontecres.stashapp.api.MetadataGenerateMutation
import com.github.damontecres.stashapp.api.MetadataScanMutation
import com.github.damontecres.stashapp.api.SceneAddOMutation
import com.github.damontecres.stashapp.api.SceneAddPlayCountMutation
import com.github.damontecres.stashapp.api.SceneDecrementOMutation
import com.github.damontecres.stashapp.api.SceneDeleteOMutation
import com.github.damontecres.stashapp.api.SceneIncrementOMutation
import com.github.damontecres.stashapp.api.SceneIncrementPlayCountMutation
import com.github.damontecres.stashapp.api.SceneResetOMutation
import com.github.damontecres.stashapp.api.SceneSaveActivityMutation
import com.github.damontecres.stashapp.api.SceneUpdateMutation
import com.github.damontecres.stashapp.api.UpdateImageMutation
import com.github.damontecres.stashapp.api.UpdateMarkerMutation
import com.github.damontecres.stashapp.api.UpdatePerformerMutation
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.GenerateMetadataInput
import com.github.damontecres.stashapp.api.type.ImageUpdateInput
import com.github.damontecres.stashapp.api.type.PerformerUpdateInput
import com.github.damontecres.stashapp.api.type.ScanMetadataInput
import com.github.damontecres.stashapp.api.type.SceneMarkerCreateInput
import com.github.damontecres.stashapp.api.type.SceneMarkerUpdateInput
import com.github.damontecres.stashapp.api.type.SceneUpdateInput
import com.github.damontecres.stashapp.data.OCounter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReadWriteLock

/**
 * Class for sending graphql mutations
 *
 * @param context
 * @param showToasts show a toast when errors occur
 * @param lock an optional lock, when if shared with [QueryEngine], can prevent race conditions
 */
class MutationEngine(
    private val context: Context,
    private val showToasts: Boolean = false,
    lock: ReadWriteLock? = null,
) {
    private val client = StashClient.getApolloClient(context)

    private val serverPreferences = ServerPreferences(context)

    private val writeLock = lock?.writeLock()

    private suspend fun <D : Mutation.Data> executeMutation(mutation: Mutation<D>): ApolloResponse<D> {
        val mutationName = mutation.name()
        val id = MUTATION_ID.getAndIncrement()
        try {
            Log.v(TAG, "executeMutation $id $mutationName start")
            val response =
                withContext(Dispatchers.IO) {
                    try {
                        writeLock?.lock()
                        client.mutation(mutation).execute()
                    } finally {
                        writeLock?.unlock()
                    }
                }
            if (response.errors.isNullOrEmpty()) {
                Log.v(TAG, "executeMutation $id $mutationName successful")
                return response
            } else {
                val errorMsgs = response.errors!!.joinToString("\n") { it.message }
                if (showToasts) {
                    Toast.makeText(
                        context,
                        "${response.errors!!.size} errors in response ($mutationName)\n$errorMsgs",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                Log.e(TAG, "Errors in $id $mutationName: ${response.errors}")
                throw MutationException(
                    id,
                    "($mutationName), ${response.errors!!.size} errors in graphql response",
                )
            }
        } catch (ex: ApolloNetworkException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "Network error ($id $mutationName). Message: ${ex.message}, ${ex.cause?.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
            Log.e(TAG, "Network error in $id $mutationName", ex)
            throw MutationException(id, "Network error ($mutationName)", ex)
        } catch (ex: ApolloHttpException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "HTTP error ($mutationName). Status=${ex.statusCode}, Msg=${ex.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
            Log.e(TAG, "HTTP ${ex.statusCode} error in $id $mutationName", ex)
            throw MutationException(id, "HTTP ${ex.statusCode} ($mutationName)", ex)
        } catch (ex: ApolloException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "Server query error ($id $mutationName). Msg=${ex.message}, ${ex.cause?.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
            Log.e(TAG, "ApolloException in $id $mutationName", ex)
            throw MutationException(id, "Apollo exception ($mutationName)", ex)
        }
    }

    /**
     * Saves the resume time for a given scene
     *
     * @param sceneId the scene ID
     * @param position the video playback position in milliseconds
     */
    suspend fun saveSceneActivity(
        sceneId: String,
        position: Long,
        duration: Int? = null,
    ): Boolean {
        Log.v(
            TAG,
            "SceneSaveActivity sceneId=$sceneId, position=$position, playDuration=$duration",
        )
        val resumeTime = position / 1000.0
        val playDuration = if (duration != null && duration >= 1) duration.toDouble() else null
        val mutation =
            SceneSaveActivityMutation(
                scene_id = sceneId,
                resume_time = resumeTime,
                play_duration = playDuration,
            )
        val result = executeMutation(mutation)
        return result.data!!.sceneSaveActivity
    }

    suspend fun incrementPlayCount(sceneId: String): Int {
        Log.v(TAG, "incrementPlayCount on $sceneId")
        return if (ServerPreferences(context).serverVersion.isGreaterThan(Version.V0_24_3)) {
            val mutation = SceneAddPlayCountMutation(sceneId, emptyList())
            val result = executeMutation(mutation)
            result.data!!.sceneAddPlay.count
        } else {
            val mutation = SceneIncrementPlayCountMutation(sceneId)
            val result = executeMutation(mutation)
            result.data!!.sceneIncrementPlayCount
        }
    }

    private fun getServerBoolean(preferenceKey: String): Optional<Boolean> {
        return Optional.present(
            serverPreferences.preferences.getBoolean(
                preferenceKey,
                false,
            ),
        )
    }

    suspend fun triggerScan() {
        val mutation =
            MetadataScanMutation(
                ScanMetadataInput(
                    scanGenerateClipPreviews = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_CLIP_PREVIEWS),
                    scanGenerateCovers = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_COVERS),
                    scanGeneratePhashes = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_PHASHES),
                    scanGeneratePreviews = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_PREVIEWS),
                    scanGenerateSprites = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_SPRITES),
                    scanGenerateThumbnails = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_THUMBNAILS),
                    scanGenerateImagePreviews = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_IMAGE_PREVIEWS),
                ),
            )
        executeMutation(mutation)
    }

    suspend fun triggerGenerate() {
        val mutation =
            MetadataGenerateMutation(
                GenerateMetadataInput(
                    clipPreviews = getServerBoolean(ServerPreferences.PREF_GEN_CLIP_PREVIEWS),
                    covers = getServerBoolean(ServerPreferences.PREF_GEN_COVERS),
                    imagePreviews = getServerBoolean(ServerPreferences.PREF_GEN_IMAGE_PREVIEWS),
                    interactiveHeatmapsSpeeds = getServerBoolean(ServerPreferences.PREF_GEN_INTERACTIVE_HEATMAPS_SPEEDS),
                    markerImagePreviews = getServerBoolean(ServerPreferences.PREF_GEN_MARKER_IMAGE_PREVIEWS),
                    markers = getServerBoolean(ServerPreferences.PREF_GEN_MARKERS),
                    markerScreenshots = getServerBoolean(ServerPreferences.PREF_GEN_MARKER_SCREENSHOTS),
                    phashes = getServerBoolean(ServerPreferences.PREF_GEN_PHASHES),
                    previews = getServerBoolean(ServerPreferences.PREF_GEN_PREVIEWS),
                    sprites = getServerBoolean(ServerPreferences.PREF_GEN_SPRITES),
                    transcodes = getServerBoolean(ServerPreferences.PREF_GEN_TRANSCODES),
                ),
            )
        executeMutation(mutation)
    }

    suspend fun setTagsOnScene(
        sceneId: String,
        tagIds: List<String>,
    ): SceneUpdateMutation.SceneUpdate? {
        Log.v(TAG, "setTagsOnScene sceneId=$sceneId, tagIds=$tagIds")
        val mutation =
            SceneUpdateMutation(
                input =
                    SceneUpdateInput(
                        id = sceneId,
                        tag_ids = Optional.present(tagIds),
                    ),
            )
        val result = executeMutation(mutation)
        return result.data?.sceneUpdate
    }

    suspend fun setTagsOnMarker(
        markerId: String,
        primaryTagId: String,
        tagIds: List<String>,
    ): MarkerData? {
        Log.v(TAG, "setTagsOnMarker markerId=$markerId, primaryTagId=$primaryTagId, tagIds=$tagIds")
        val mutation =
            UpdateMarkerMutation(
                input =
                    SceneMarkerUpdateInput(
                        id = markerId,
                        primary_tag_id = Optional.present(primaryTagId),
                        tag_ids = Optional.present(tagIds),
                    ),
            )
        val result = executeMutation(mutation)
        return result.data?.sceneMarkerUpdate?.markerData
    }

    suspend fun setPerformersOnScene(
        sceneId: String,
        performerIds: List<String>,
    ): SceneUpdateMutation.SceneUpdate? {
        Log.v(TAG, "setPerformersOnScene sceneId=$sceneId, performerIds=$performerIds")
        val mutation =
            SceneUpdateMutation(
                input =
                    SceneUpdateInput(
                        id = sceneId,
                        performer_ids = Optional.present(performerIds),
                    ),
            )
        val result = executeMutation(mutation)
        return result.data?.sceneUpdate
    }

    suspend fun incrementOCounter(sceneId: String): OCounter {
        return if (ServerPreferences(context).serverVersion.isGreaterThan(Version.V0_24_3)) {
            val mutation = SceneAddOMutation(sceneId, emptyList())
            val result = executeMutation(mutation)
            OCounter(sceneId, result.data!!.sceneAddO.count)
        } else {
            val mutation = SceneIncrementOMutation(sceneId)
            val result = executeMutation(mutation)
            OCounter(sceneId, result.data!!.sceneIncrementO)
        }
    }

    suspend fun decrementOCounter(sceneId: String): OCounter {
        return if (ServerPreferences(context).serverVersion.isGreaterThan(Version.V0_24_3)) {
            val mutation = SceneDeleteOMutation(sceneId, emptyList())
            val result = executeMutation(mutation)
            OCounter(sceneId, result.data!!.sceneDeleteO.count)
        } else {
            val mutation = SceneDecrementOMutation(sceneId)
            val result = executeMutation(mutation)
            OCounter(sceneId, result.data!!.sceneDecrementO)
        }
    }

    suspend fun resetOCounter(sceneId: String): OCounter {
        val mutation = SceneResetOMutation(sceneId)
        val result = executeMutation(mutation)
        return OCounter(sceneId, result.data!!.sceneResetO)
    }

    suspend fun createMarker(
        sceneId: String,
        position: Long,
        primaryTagId: String,
    ): MarkerData? {
        val input =
            SceneMarkerCreateInput(
                title = "",
                seconds = position.toMilliseconds,
                scene_id = sceneId,
                primary_tag_id = primaryTagId,
                tag_ids = Optional.absent(),
            )
        val mutation = CreateMarkerMutation(input)
        val result = executeMutation(mutation)
        return result.data?.sceneMarkerCreate?.markerData
    }

    suspend fun deleteMarker(id: String): Boolean {
        val mutation = DeleteMarkerMutation(id)
        val result = executeMutation(mutation)
        return result.data?.sceneMarkerDestroy ?: false
    }

    suspend fun setRating(
        sceneId: String,
        rating100: Int,
    ): SceneUpdateMutation.SceneUpdate? {
        Log.v(TAG, "setRating sceneId=$sceneId, rating=$rating100")
        val mutation =
            SceneUpdateMutation(
                input =
                    SceneUpdateInput(
                        id = sceneId,
                        rating100 = Optional.present(rating100),
                    ),
            )
        return executeMutation(mutation).data?.sceneUpdate
    }

    suspend fun incrementImageOCounter(imageId: String): OCounter {
        val mutation = ImageIncrementOMutation(imageId)
        val result = executeMutation(mutation)
        return OCounter(imageId, result.data!!.imageIncrementO)
    }

    suspend fun decrementImageOCounter(imageId: String): OCounter {
        val mutation = ImageDecrementOMutation(imageId)
        val result = executeMutation(mutation)
        return OCounter(imageId, result.data!!.imageDecrementO)
    }

    suspend fun resetImageOCounter(imageId: String): OCounter {
        val mutation = ImageResetOMutation(imageId)
        val result = executeMutation(mutation)
        return OCounter(imageId, result.data!!.imageResetO)
    }

    suspend fun updateImage(
        imageId: String,
        performerIds: List<String>? = null,
        tagIds: List<String>? = null,
        rating100: Int? = null,
    ): UpdateImageMutation.ImageUpdate? {
        val mutation =
            UpdateImageMutation(
                ImageUpdateInput(
                    id = imageId,
                    performer_ids = Optional.presentIfNotNull(performerIds),
                    tag_ids = Optional.presentIfNotNull(tagIds),
                    rating100 = Optional.presentIfNotNull(rating100),
                ),
            )
        val result = executeMutation(mutation)
        return result.data?.imageUpdate
    }

    suspend fun setPerformerFavorite(
        performerId: String,
        favorite: Boolean,
    ): PerformerData? {
        val input =
            PerformerUpdateInput(
                id = performerId,
                favorite = Optional.present(favorite),
            )
        return updatePerformer(input)
    }

    suspend fun updatePerformer(input: PerformerUpdateInput): PerformerData? {
        val mutation = UpdatePerformerMutation(input)
        val result = executeMutation(mutation)
        return result.data?.performerUpdate?.performerData
    }

    companion object {
        const val TAG = "MutationEngine"

        private val MUTATION_ID = AtomicInteger(0)
    }

    open class MutationException(val id: Int, msg: String? = null, cause: ApolloException? = null) :
        RuntimeException(msg, cause)
}
