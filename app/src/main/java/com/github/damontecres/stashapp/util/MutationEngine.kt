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
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.type.GenerateMetadataInput
import com.github.damontecres.stashapp.api.type.ScanMetadataInput
import com.github.damontecres.stashapp.api.type.SceneMarkerCreateInput
import com.github.damontecres.stashapp.api.type.SceneUpdateInput
import com.github.damontecres.stashapp.data.OCounter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Class for sending graphql mutations
 */
class MutationEngine(private val context: Context, private val showToasts: Boolean = false) {
    private val client =
        createApolloClient(context) ?: throw QueryEngine.StashNotConfiguredException()

    private val serverPreferences = ServerPreferences(context)

    private suspend fun <D : Mutation.Data> executeMutation(mutation: Mutation<D>): ApolloResponse<D> {
        val mutationName = mutation.name()
        try {
            val response =
                withContext(Dispatchers.IO) {
                    client.mutation(mutation).execute()
                }
            if (response.errors.isNullOrEmpty()) {
                Log.d(TAG, "executeMutation $mutationName successful")
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
                Log.e(TAG, "Errors in $mutationName: ${response.errors}")
                throw MutationException("($mutationName), ${response.errors!!.size} errors in graphql response")
            }
        } catch (ex: ApolloNetworkException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "Network error ($mutationName). Message: ${ex.message}, ${ex.cause?.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
            Log.e(TAG, "Network error in $mutationName", ex)
            throw MutationException("Network error ($mutationName)", ex)
        } catch (ex: ApolloHttpException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "HTTP error ($mutationName). Status=${ex.statusCode}, Msg=${ex.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
            Log.e(TAG, "HTTP ${ex.statusCode} error in $mutationName", ex)
            throw MutationException("HTTP ${ex.statusCode} ($mutationName)", ex)
        } catch (ex: ApolloException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "Server query error ($mutationName). Msg=${ex.message}, ${ex.cause?.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
            Log.e(TAG, "ApolloException in $mutationName", ex)
            throw MutationException("Apollo exception ($mutationName)", ex)
        }
    }

    /**
     * Saves the resume time for a given scene
     *
     * @param sceneId the scene ID
     * @param position the video playback position in milliseconds
     */
    suspend fun saveSceneActivity(
        sceneId: Long,
        position: Long,
        duration: Long? = null,
    ): Boolean {
        Log.v(
            TAG,
            "SceneSaveActivity sceneId=$sceneId, position=$position, playDuration=$duration",
        )
        val resumeTime = position / 1000.0
        val playDuration = if (duration != null && duration >= 1000.0) duration / 1000.0 else null
        val mutation =
            SceneSaveActivityMutation(
                scene_id = sceneId.toString(),
                resume_time = resumeTime,
                play_duration = playDuration,
            )
        val result = executeMutation(mutation)
        return result.data!!.sceneSaveActivity
    }

    suspend fun incrementPlayCount(sceneId: Long): Int {
        Log.v(TAG, "incrementPlayCount on $sceneId")
        return if (ServerPreferences(context).serverVersion.isGreaterThan(Version.V0_24_3)) {
            val mutation = SceneAddPlayCountMutation(sceneId.toString(), emptyList())
            val result = executeMutation(mutation)
            result.data!!.sceneAddPlay.count
        } else {
            val mutation = SceneIncrementPlayCountMutation(sceneId.toString())
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
        sceneId: Long,
        tagIds: List<Int>,
    ): SceneUpdateMutation.SceneUpdate? {
        Log.v(TAG, "setTagsOnScene sceneId=$sceneId, tagIds=$tagIds")
        val mutation =
            SceneUpdateMutation(
                input =
                    SceneUpdateInput(
                        id = sceneId.toString(),
                        tag_ids = Optional.present(tagIds.map { it.toString() }),
                    ),
            )
        val result = executeMutation(mutation)
        return result.data?.sceneUpdate
    }

    suspend fun setPerformersOnScene(
        sceneId: Long,
        performerIds: List<Int>,
    ): SceneUpdateMutation.SceneUpdate? {
        Log.v(TAG, "setTagsOnScene sceneId=$sceneId, performerIds=$performerIds")
        val mutation =
            SceneUpdateMutation(
                input =
                    SceneUpdateInput(
                        id = sceneId.toString(),
                        performer_ids = Optional.present(performerIds.map { it.toString() }),
                    ),
            )
        val result = executeMutation(mutation)
        return result.data?.sceneUpdate
    }

    suspend fun incrementOCounter(sceneId: Int): OCounter {
        return if (ServerPreferences(context).serverVersion.isGreaterThan(Version.V0_24_3)) {
            val mutation = SceneAddOMutation(sceneId.toString(), emptyList())
            val result = executeMutation(mutation)
            OCounter(sceneId.toString(), result.data!!.sceneAddO.count)
        } else {
            val mutation = SceneIncrementOMutation(sceneId.toString())
            val result = executeMutation(mutation)
            OCounter(sceneId.toString(), result.data!!.sceneIncrementO)
        }
    }

    suspend fun decrementOCounter(sceneId: Int): OCounter {
        return if (ServerPreferences(context).serverVersion.isGreaterThan(Version.V0_24_3)) {
            val mutation = SceneDeleteOMutation(sceneId.toString(), emptyList())
            val result = executeMutation(mutation)
            OCounter(sceneId.toString(), result.data!!.sceneDeleteO.count)
        } else {
            val mutation = SceneDecrementOMutation(sceneId.toString())
            val result = executeMutation(mutation)
            OCounter(sceneId.toString(), result.data!!.sceneDecrementO)
        }
    }

    suspend fun resetOCounter(sceneId: Int): OCounter {
        val mutation = SceneResetOMutation(sceneId.toString())
        val result = executeMutation(mutation)
        return OCounter(sceneId.toString(), result.data!!.sceneResetO)
    }

    suspend fun createMarker(
        sceneId: String,
        position: Long,
        primaryTagId: String,
    ): MarkerData? {
        val input =
            SceneMarkerCreateInput(
                title = "",
                seconds = position / 1000.0,
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
        sceneId: Int,
        rating100: Int,
    ) {
        Log.v(TAG, "setRating sceneId=$sceneId, rating=$rating100")
        val mutation =
            SceneUpdateMutation(
                input =
                    SceneUpdateInput(
                        id = sceneId.toString(),
                        rating100 = Optional.present(rating100),
                    ),
            )
        executeMutation(mutation)
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

    companion object {
        const val TAG = "MutationEngine"
    }

    open class MutationException(msg: String? = null, cause: ApolloException? = null) :
        RuntimeException(msg, cause)
}
