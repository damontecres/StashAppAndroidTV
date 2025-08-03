package com.github.damontecres.stashapp.util

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.CreateGroupMutation
import com.github.damontecres.stashapp.api.CreateMarkerMutation
import com.github.damontecres.stashapp.api.CreatePerformerMutation
import com.github.damontecres.stashapp.api.CreateStudioMutation
import com.github.damontecres.stashapp.api.CreateTagMutation
import com.github.damontecres.stashapp.api.DeleteMarkerMutation
import com.github.damontecres.stashapp.api.ImageDecrementOMutation
import com.github.damontecres.stashapp.api.ImageIncrementOMutation
import com.github.damontecres.stashapp.api.ImageResetOMutation
import com.github.damontecres.stashapp.api.InstallPackagesMutation
import com.github.damontecres.stashapp.api.MetadataGenerateMutation
import com.github.damontecres.stashapp.api.MetadataScanMutation
import com.github.damontecres.stashapp.api.SaveFilterMutation
import com.github.damontecres.stashapp.api.SceneAddOMutation
import com.github.damontecres.stashapp.api.SceneAddPlayCountMutation
import com.github.damontecres.stashapp.api.SceneDeleteOMutation
import com.github.damontecres.stashapp.api.SceneResetOMutation
import com.github.damontecres.stashapp.api.SceneSaveActivityMutation
import com.github.damontecres.stashapp.api.SceneUpdateMutation
import com.github.damontecres.stashapp.api.UpdateGalleryMutation
import com.github.damontecres.stashapp.api.UpdateGroupMutation
import com.github.damontecres.stashapp.api.UpdateImageMutation
import com.github.damontecres.stashapp.api.UpdateMarkerMutation
import com.github.damontecres.stashapp.api.UpdatePerformerMutation
import com.github.damontecres.stashapp.api.UpdateStudioMutation
import com.github.damontecres.stashapp.api.UpdateTagMutation
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SavedFilter
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.GalleryUpdateInput
import com.github.damontecres.stashapp.api.type.GenerateMetadataInput
import com.github.damontecres.stashapp.api.type.GroupCreateInput
import com.github.damontecres.stashapp.api.type.GroupUpdateInput
import com.github.damontecres.stashapp.api.type.ImageUpdateInput
import com.github.damontecres.stashapp.api.type.PackageSpecInput
import com.github.damontecres.stashapp.api.type.PackageType
import com.github.damontecres.stashapp.api.type.PerformerCreateInput
import com.github.damontecres.stashapp.api.type.PerformerUpdateInput
import com.github.damontecres.stashapp.api.type.SaveFilterInput
import com.github.damontecres.stashapp.api.type.ScanMetadataInput
import com.github.damontecres.stashapp.api.type.SceneGroupInput
import com.github.damontecres.stashapp.api.type.SceneMarkerCreateInput
import com.github.damontecres.stashapp.api.type.SceneMarkerUpdateInput
import com.github.damontecres.stashapp.api.type.SceneUpdateInput
import com.github.damontecres.stashapp.api.type.StudioCreateInput
import com.github.damontecres.stashapp.api.type.StudioUpdateInput
import com.github.damontecres.stashapp.api.type.TagCreateInput
import com.github.damontecres.stashapp.api.type.TagUpdateInput
import com.github.damontecres.stashapp.data.OCounter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Class for sending graphql mutations
 */
class MutationEngine(
    apolloClient: ApolloClient,
    server: StashServer?,
) : StashEngine(apolloClient, server) {
    constructor(server: StashServer) : this(server.apolloClient, server)

    private val readOnlyMode = readOnlyModeEnabled()

    suspend fun <D : Mutation.Data> executeMutation(
        mutation: Mutation<D>,
        overrideReadOnly: Boolean = false,
    ): ApolloResponse<D> =
        withContext(Dispatchers.IO) {
            if (!overrideReadOnly && readOnlyMode) {
                throw IllegalStateException("Read only mode enabled!")
            }
            val mutationName = mutation.name()
            val id = MUTATION_ID.getAndIncrement()

            Log.v(TAG, "executeMutation $id $mutationName start")
            val response = client.mutation(mutation).execute()
            if (response.data != null) {
                Log.v(TAG, "executeMutation $id $mutationName successful")
                return@withContext response
            } else if (response.exception != null) {
                throw createException(id, mutationName, response.exception!!) { msg, ex ->
                    MutationException(id, mutationName, msg, ex)
                }
            } else {
                val errorMsgs = response.errors!!.joinToString("\n") { it.message }
                Log.e(TAG, "Errors in $id $mutationName: ${response.errors}")
                throw MutationException(id, mutationName, "Error in $mutationName: $errorMsgs")
            }
        }

    /**
     * Saves the resume time for a given scene
     *
     * @param sceneId the scene ID
     * @param position the video playback position in milliseconds
     * @param duration how long has the playback been in milliseconds
     */
    suspend fun saveSceneActivity(
        sceneId: String,
        position: Long,
        duration: Long? = null,
    ): Boolean {
        val resumeTime = position / 1000.0
        val playDuration = if (duration != null && duration >= 1) duration / 1000.0 else null
        Log.v(
            TAG,
            "SceneSaveActivity sceneId=$sceneId, position=$position, playDuration=$playDuration",
        )
        val mutation =
            SceneSaveActivityMutation(
                scene_id = sceneId,
                resume_time = resumeTime,
                play_duration = playDuration,
            )
        val result = executeMutation(mutation, true)
        return result.data!!.sceneSaveActivity
    }

    suspend fun incrementPlayCount(sceneId: String): Int {
        Log.v(TAG, "incrementPlayCount on $sceneId")
        val mutation = SceneAddPlayCountMutation(sceneId, emptyList())
        val result = executeMutation(mutation, true)
        return result.data!!.sceneAddPlay.count
    }

    private fun getServerBoolean(
        preferenceKey: String,
        defValue: Boolean = false,
    ): Optional<Boolean> =
        Optional.presentIfNotNull(
            serverPreferences?.preferences?.getBoolean(
                preferenceKey,
                defValue,
            ),
        )

    suspend fun triggerScan(): String {
        val mutation =
            MetadataScanMutation(
                ScanMetadataInput(
                    scanGenerateClipPreviews = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_CLIP_PREVIEWS),
                    scanGenerateCovers =
                        getServerBoolean(
                            ServerPreferences.PREF_SCAN_GENERATE_COVERS,
                            true,
                        ),
                    scanGeneratePhashes = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_PHASHES),
                    scanGeneratePreviews = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_PREVIEWS),
                    scanGenerateSprites = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_SPRITES),
                    scanGenerateThumbnails = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_THUMBNAILS),
                    scanGenerateImagePreviews = getServerBoolean(ServerPreferences.PREF_SCAN_GENERATE_IMAGE_PREVIEWS),
                ),
            )
        return executeMutation(mutation).data!!.metadataScan
    }

    suspend fun triggerGenerate(): String {
        val mutation =
            MetadataGenerateMutation(
                GenerateMetadataInput(
                    clipPreviews = getServerBoolean(ServerPreferences.PREF_GEN_CLIP_PREVIEWS),
                    covers = getServerBoolean(ServerPreferences.PREF_GEN_COVERS, true),
                    imagePreviews = getServerBoolean(ServerPreferences.PREF_GEN_IMAGE_PREVIEWS),
                    interactiveHeatmapsSpeeds = getServerBoolean(ServerPreferences.PREF_GEN_INTERACTIVE_HEATMAPS_SPEEDS),
                    markerImagePreviews = getServerBoolean(ServerPreferences.PREF_GEN_MARKER_IMAGE_PREVIEWS),
                    markers = getServerBoolean(ServerPreferences.PREF_GEN_MARKERS, true),
                    markerScreenshots = getServerBoolean(ServerPreferences.PREF_GEN_MARKER_SCREENSHOTS),
                    phashes = getServerBoolean(ServerPreferences.PREF_GEN_PHASHES, true),
                    previews = getServerBoolean(ServerPreferences.PREF_GEN_PREVIEWS, true),
                    sprites = getServerBoolean(ServerPreferences.PREF_GEN_SPRITES, true),
                    transcodes = getServerBoolean(ServerPreferences.PREF_GEN_TRANSCODES),
                    imageThumbnails = getServerBoolean(ServerPreferences.PREF_GEN_IMAGE_THUMBNAILS),
                ),
            )
        return executeMutation(mutation).data!!.metadataGenerate
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

    suspend fun setGroupsOnScene(
        sceneId: String,
        groupIds: List<String>,
    ): SceneUpdateMutation.SceneUpdate? {
        Log.v(TAG, "setGroupsOnScene sceneId=$sceneId, tagIds=$groupIds")
        val mutation =
            SceneUpdateMutation(
                input =
                    SceneUpdateInput(
                        id = sceneId,
                        groups =
                            Optional.present(
                                groupIds.map {
                                    SceneGroupInput(
                                        group_id = it,
                                        scene_index = Optional.absent(),
                                    )
                                },
                            ),
                    ),
            )
        val result = executeMutation(mutation)
        return result.data?.sceneUpdate
    }

    suspend fun updateMarker(input: SceneMarkerUpdateInput): FullMarkerData? {
        val mutation = UpdateMarkerMutation(input = input)
        val result = executeMutation(mutation)
        return result.data?.sceneMarkerUpdate?.fullMarkerData
    }

    suspend fun setTagsOnMarker(
        markerId: String,
        primaryTagId: String,
        tagIds: List<String>,
    ): FullMarkerData? {
        Log.v(TAG, "setTagsOnMarker markerId=$markerId, primaryTagId=$primaryTagId, tagIds=$tagIds")
        return updateMarker(
            SceneMarkerUpdateInput(
                id = markerId,
                primary_tag_id = Optional.present(primaryTagId),
                tag_ids = Optional.present(tagIds),
            ),
        )
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

    suspend fun setStudioOnScene(
        sceneId: String,
        studioId: String?,
    ): SceneUpdateMutation.SceneUpdate? {
        Log.v(TAG, "setStudioOnScene sceneId=$sceneId, studioId=$studioId")
        val mutation =
            SceneUpdateMutation(
                input =
                    SceneUpdateInput(
                        id = sceneId,
                        studio_id = Optional.present(studioId),
                    ),
            )
        val result = executeMutation(mutation)
        return result.data?.sceneUpdate
    }

    suspend fun setGalleriesOnScene(
        sceneId: String,
        galleryIds: List<String>,
    ): SceneUpdateMutation.SceneUpdate? {
        Log.v(TAG, "setGalleriesOnScene sceneId=$sceneId, galleryIds=$galleryIds")
        val mutation =
            SceneUpdateMutation(
                input =
                    SceneUpdateInput(
                        id = sceneId,
                        gallery_ids = Optional.present(galleryIds),
                    ),
            )
        val result = executeMutation(mutation)
        return result.data?.sceneUpdate
    }

    suspend fun incrementOCounter(sceneId: String): OCounter {
        val mutation = SceneAddOMutation(sceneId, emptyList())
        val result = executeMutation(mutation)
        return OCounter(sceneId, result.data!!.sceneAddO.count)
    }

    suspend fun decrementOCounter(sceneId: String): OCounter {
        val mutation = SceneDeleteOMutation(sceneId, emptyList())
        val result = executeMutation(mutation)
        return OCounter(sceneId, result.data!!.sceneDeleteO.count)
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
    ): FullMarkerData? {
        val input =
            SceneMarkerCreateInput(
                title = "",
                seconds = position.toSeconds,
                scene_id = sceneId,
                primary_tag_id = primaryTagId,
                tag_ids = Optional.absent(),
            )
        val mutation = CreateMarkerMutation(input)
        val result = executeMutation(mutation)
        return result.data?.sceneMarkerCreate?.fullMarkerData
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
        studioId: String? = null,
        performerIds: List<String>? = null,
        tagIds: List<String>? = null,
        galleryIds: List<String>? = null,
        rating100: Int? = null,
    ): UpdateImageMutation.ImageUpdate? {
        val mutation =
            UpdateImageMutation(
                ImageUpdateInput(
                    id = imageId,
                    studio_id = Optional.presentIfNotNull(studioId),
                    performer_ids = Optional.presentIfNotNull(performerIds),
                    tag_ids = Optional.presentIfNotNull(tagIds),
                    gallery_ids = Optional.presentIfNotNull(galleryIds),
                    rating100 = Optional.presentIfNotNull(rating100),
                ),
            )
        val result = executeMutation(mutation)
        return result.data?.imageUpdate
    }

    suspend fun updatePerformer(
        performerId: String,
        favorite: Boolean? = null,
        rating100: Int? = null,
        tagIds: List<String>? = null,
    ): PerformerData? {
        val input =
            PerformerUpdateInput(
                id = performerId,
                favorite = Optional.presentIfNotNull(favorite),
                rating100 = Optional.presentIfNotNull(rating100),
                tag_ids = Optional.presentIfNotNull(tagIds),
            )
        return updatePerformer(input)
    }

    suspend fun updatePerformer(input: PerformerUpdateInput): PerformerData? {
        val mutation = UpdatePerformerMutation(input)
        val result = executeMutation(mutation)
        return result.data?.performerUpdate?.performerData
    }

    suspend fun createTag(input: TagCreateInput): TagData? {
        val mutation = CreateTagMutation(input)
        val result = executeMutation(mutation)
        return result.data?.tagCreate?.tagData
    }

    suspend fun setTagFavorite(
        tagId: String,
        favorite: Boolean,
    ): TagData? {
        val input =
            TagUpdateInput(
                id = tagId,
                favorite = Optional.present(favorite),
            )
        val mutation = UpdateTagMutation(input)
        return executeMutation(mutation).data?.tagUpdate?.tagData
    }

    suspend fun createPerformer(input: PerformerCreateInput): PerformerData? {
        val mutation = CreatePerformerMutation(input)
        val result = executeMutation(mutation)
        return result.data?.performerCreate?.performerData
    }

    suspend fun createGroup(input: GroupCreateInput): GroupData {
        val mutation = CreateGroupMutation(input)
        val result = executeMutation(mutation)
        return result.data?.groupCreate?.groupData!!
    }

    suspend fun updateGallery(
        galleryId: String,
        rating100: Int,
    ): GalleryData? {
        val mutation =
            UpdateGalleryMutation(
                GalleryUpdateInput(
                    id = galleryId,
                    rating100 = Optional.present(rating100),
                ),
            )
        val result = executeMutation(mutation)
        return result.data?.galleryUpdate?.galleryData
    }

    suspend fun updateGallery(
        galleryId: String,
        rating100: Int? = null,
        tagIds: List<String>? = null,
        performerIds: List<String>? = null,
        studioId: String? = null,
    ): GalleryData? {
        val mutation =
            UpdateGalleryMutation(
                GalleryUpdateInput(
                    id = galleryId,
                    rating100 = Optional.presentIfNotNull(rating100),
                    tag_ids = Optional.presentIfNotNull(tagIds),
                    performer_ids = Optional.presentIfNotNull(performerIds),
                    studio_id = Optional.presentIfNotNull(studioId),
                ),
            )
        val result = executeMutation(mutation)
        return result.data?.galleryUpdate?.galleryData
    }

    suspend fun installPackage(
        type: PackageType,
        input: PackageSpecInput,
    ): String {
        val mutation = InstallPackagesMutation(type, listOf(input))
        val result = executeMutation(mutation)
        return result.data!!.installPackages
    }

    suspend fun saveFilter(input: SaveFilterInput): SavedFilter {
        val mutation = SaveFilterMutation(input)
        return executeMutation(mutation).data!!.saveFilter.savedFilter
    }

    suspend fun updateStudio(
        studioId: String,
        favorite: Boolean? = null,
        rating100: Int? = null,
        parentStudioId: String? = null,
        tagIds: List<String>? = null,
    ): StudioData? {
        val input =
            StudioUpdateInput(
                id = studioId,
                favorite = Optional.presentIfNotNull(favorite),
                rating100 = Optional.presentIfNotNull(rating100),
                parent_id = Optional.presentIfNotNull(parentStudioId),
                tag_ids = Optional.presentIfNotNull(tagIds),
            )
        val mutation = UpdateStudioMutation(input)
        return executeMutation(mutation).data?.studioUpdate?.studioData
    }

    suspend fun createStudio(name: String): StudioData? {
        val input = StudioCreateInput(name = name)
        val mutation = CreateStudioMutation(input)
        return executeMutation(mutation).data?.studioCreate?.studioData
    }

    suspend fun updateGroup(
        groupId: String,
        rating100: Int? = null,
    ): GroupData? {
        val input =
            GroupUpdateInput(
                id = groupId,
                rating100 = Optional.present(rating100),
            )
        val mutation = UpdateGroupMutation(input)
        return executeMutation(mutation).data?.groupUpdate?.groupData
    }

    companion object {
        const val TAG = "MutationEngine"

        private val MUTATION_ID = AtomicInteger(0)
    }

    open class MutationException(
        id: Int,
        mutationName: String,
        msg: String? = null,
        cause: Exception? = null,
    ) : ServerCommunicationException(id, mutationName, msg, cause)
}
