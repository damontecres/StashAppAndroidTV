package com.github.damontecres.stashapp.util

import android.util.Log
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.ConfigurationQuery
import com.github.damontecres.stashapp.api.FindGalleriesQuery
import com.github.damontecres.stashapp.api.FindGroupQuery
import com.github.damontecres.stashapp.api.FindGroupsQuery
import com.github.damontecres.stashapp.api.FindImageQuery
import com.github.damontecres.stashapp.api.FindImagesQuery
import com.github.damontecres.stashapp.api.FindJobQuery
import com.github.damontecres.stashapp.api.FindMarkersQuery
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.FindSavedFilterQuery
import com.github.damontecres.stashapp.api.FindSavedFiltersQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.FindSlimImagesQuery
import com.github.damontecres.stashapp.api.FindStudiosQuery
import com.github.damontecres.stashapp.api.FindTagsQuery
import com.github.damontecres.stashapp.api.GetExtraImageQuery
import com.github.damontecres.stashapp.api.GetGalleryQuery
import com.github.damontecres.stashapp.api.GetMarkerQuery
import com.github.damontecres.stashapp.api.GetPerformerQuery
import com.github.damontecres.stashapp.api.GetSceneQuery
import com.github.damontecres.stashapp.api.GetStudioQuery
import com.github.damontecres.stashapp.api.GetTagQuery
import com.github.damontecres.stashapp.api.GetVideoSceneQuery
import com.github.damontecres.stashapp.api.fragment.ExtraImageData
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SavedFilter
import com.github.damontecres.stashapp.api.fragment.SlimImageData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.StashJob
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.FindJobInput
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.JobStatus
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.JobResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Handles making graphql queries to the server
 */
class QueryEngine(
    server: StashServer,
) : StashEngine(server) {
    private suspend fun <D : Operation.Data> executeQuery(query: ApolloCall<D>): ApolloResponse<D> =
        withContext(Dispatchers.IO) {
            val queryName = query.operation.name()
            val id = QUERY_ID.getAndIncrement()
            Log.v(TAG, "executeQuery $id $queryName")

            val response = query.execute()
            if (response.data != null) {
                Log.v(TAG, "executeQuery $id $queryName successful")
                return@withContext response
            } else if (response.exception != null) {
                throw createException(id, queryName, response.exception!!) { msg, ex ->
                    QueryException(id, queryName, msg, ex)
                }
            } else {
                val errorMessages = response.errors!!.joinToString("\n") { it.message }
                Log.e(TAG, "Errors in $id $queryName: ${response.errors}")
                throw QueryException(id, queryName, "Error in $queryName: $errorMessages")
            }
        }

    suspend fun <D : Query.Data> executeQuery(query: Query<D>): ApolloResponse<D> = executeQuery(client.query(query))

    suspend fun findScenes(
        findFilter: FindFilterType? = null,
        sceneFilter: SceneFilterType? = null,
        ids: List<String>? = null,
        useRandom: Boolean = true,
    ): List<SlimSceneData> {
        val query =
            client.query(
                FindScenesQuery(
                    filter = updateFilter(findFilter, useRandom),
                    scene_filter = sceneFilter,
                    ids = ids,
                ),
            )
        val scenes =
            executeQuery(query).data?.findScenes?.scenes?.map {
                it.slimSceneData
            }
        return scenes.orEmpty()
    }

    suspend fun getScene(sceneId: String): FullSceneData? {
        val query = client.query(GetSceneQuery(id = sceneId))
        return executeQuery(query).data?.findScene?.fullSceneData
    }

    suspend fun getVideoScene(sceneId: String): VideoSceneData? {
        val query = client.query(GetVideoSceneQuery(id = sceneId))
        return executeQuery(query).data?.findScene?.videoSceneData
    }

    suspend fun findPerformers(
        findFilter: FindFilterType? = null,
        performerFilter: PerformerFilterType? = null,
        performerIds: List<String>? = null,
        useRandom: Boolean = true,
    ): List<PerformerData> {
        val query =
            client.query(
                FindPerformersQuery(
                    filter = updateFilter(findFilter, useRandom),
                    performer_filter = performerFilter,
                    ids = performerIds,
                ),
            )
        val performers =
            executeQuery(query).data?.findPerformers?.performers?.map {
                it.performerData
            }
        return performers.orEmpty()
    }

    suspend fun getPerformer(performerId: String): PerformerData? {
        val query = client.query(GetPerformerQuery(id = performerId))
        return executeQuery(query).data?.findPerformer?.performerData
    }

    suspend fun findStudios(
        findFilter: FindFilterType? = null,
        studioFilter: StudioFilterType? = null,
        studioIds: List<String>? = null,
        useRandom: Boolean = true,
    ): List<StudioData> {
        val query =
            client.query(
                FindStudiosQuery(
                    filter = updateFilter(findFilter, useRandom),
                    studio_filter = studioFilter,
                    ids = studioIds,
                ),
            )
        val studios =
            executeQuery(query).data?.findStudios?.studios?.map {
                it.studioData
            }
        return studios.orEmpty()
    }

    suspend fun getStudio(studioId: String): StudioData? {
        val query = client.query(GetStudioQuery(id = studioId))
        return executeQuery(query).data?.findStudio?.studioData
    }

    suspend fun findTags(
        findFilter: FindFilterType? = null,
        tagFilter: TagFilterType? = null,
        useRandom: Boolean = true,
    ): List<TagData> {
        val query =
            client.query(
                FindTagsQuery(
                    filter = updateFilter(findFilter, useRandom),
                    tag_filter = tagFilter,
                    ids = null,
                ),
            )
        val tags =
            executeQuery(query)
                .data
                ?.findTags
                ?.tags
                ?.map { it.tagData }
        return tags.orEmpty()
    }

    suspend fun getTags(tagIds: List<String>): List<TagData> {
        if (tagIds.isEmpty()) {
            return listOf()
        }
        val query =
            client.query(
                FindTagsQuery(
                    filter = null,
                    tag_filter = null,
                    ids = tagIds,
                ),
            )
        val tags =
            executeQuery(query)
                .data
                ?.findTags
                ?.tags
                ?.map { it.tagData }
        return tags.orEmpty()
    }

    suspend fun getTag(tagId: String): TagData? {
        val query = client.query(GetTagQuery(id = tagId))
        return executeQuery(query).data?.findTag?.tagData
    }

    suspend fun findGroups(
        findFilter: FindFilterType? = null,
        groupFilter: GroupFilterType? = null,
        groupIds: List<String>? = null,
        useRandom: Boolean = true,
    ): List<GroupData> {
        val query =
            client.query(
                FindGroupsQuery(
                    filter = updateFilter(findFilter, useRandom),
                    group_filter = groupFilter,
                    ids = groupIds,
                ),
            )
        val tags =
            executeQuery(query)
                .data
                ?.findGroups
                ?.groups
                ?.map { it.groupData }
        return tags.orEmpty()
    }

    suspend fun getGroup(groupId: String): GroupData? {
        val query = client.query(FindGroupQuery(groupId))
        return executeQuery(query).data?.findGroup?.groupData
    }

    suspend fun findMarkers(
        findFilter: FindFilterType? = null,
        markerFilter: SceneMarkerFilterType? = null,
        markerIds: List<String>? = null,
        useRandom: Boolean = true,
    ): List<MarkerData> {
        val query =
            client.query(
                FindMarkersQuery(
                    filter = updateFilter(findFilter, useRandom),
                    scene_marker_filter = markerFilter,
                    ids = markerIds,
                ),
            )
        return executeQuery(query)
            .data
            ?.findSceneMarkers
            ?.scene_markers
            ?.map { it.markerData }
            .orEmpty()
    }

    suspend fun getMarker(markerId: String): FullMarkerData? {
        val query = client.query(GetMarkerQuery(listOf(markerId)))
        return executeQuery(query)
            .data
            ?.findSceneMarkers
            ?.scene_markers
            ?.firstOrNull()
            ?.fullMarkerData
    }

    suspend fun findMarkersInScene(sceneId: String): List<MarkerData> =
        findMarkers(
            markerFilter =
                SceneMarkerFilterType(
                    scenes =
                        Optional.present(
                            MultiCriterionInput(
                                value = Optional.present(listOf(sceneId)),
                                modifier = CriterionModifier.INCLUDES_ALL,
                            ),
                        ),
                ),
        ).sortedBy { it.seconds }

    suspend fun findImages(
        findFilter: FindFilterType? = null,
        imageFilter: ImageFilterType? = null,
        ids: List<String>? = null,
        useRandom: Boolean = true,
    ): List<ImageData> {
        val query =
            client.query(
                FindImagesQuery(
                    filter = updateFilter(findFilter, useRandom),
                    image_filter = imageFilter,
                    ids = ids,
                ),
            )
        return executeQuery(query)
            .data
            ?.findImages
            ?.images
            ?.map { it.imageData }
            .orEmpty()
    }

    suspend fun findSlimImages(
        findFilter: FindFilterType? = null,
        imageFilter: ImageFilterType? = null,
        ids: List<String>? = null,
        useRandom: Boolean = true,
    ): List<SlimImageData> {
        val query =
            client.query(
                FindSlimImagesQuery(
                    filter = updateFilter(findFilter, useRandom),
                    image_filter = imageFilter,
                    ids = ids,
                ),
            )
        return executeQuery(query)
            .data
            ?.findImages
            ?.images
            ?.map { it.slimImageData }
            .orEmpty()
    }

    suspend fun getImage(imageId: String): ImageData? {
        val query = client.query(FindImageQuery(imageId))
        return executeQuery(query).data?.findImage?.imageData
    }

    suspend fun getImageExtra(imageId: String): ExtraImageData? {
        val query = client.query(GetExtraImageQuery(imageId))
        return executeQuery(query).data?.findImage?.extraImageData
    }

    suspend fun findGalleries(
        findFilter: FindFilterType? = null,
        galleryFilter: GalleryFilterType? = null,
        galleryIds: List<String>? = null,
        useRandom: Boolean = true,
    ): List<GalleryData> {
        val query =
            client.query(
                FindGalleriesQuery(
                    updateFilter(findFilter, useRandom),
                    galleryFilter,
                    ids = galleryIds,
                ),
            )
        return executeQuery(query)
            .data
            ?.findGalleries
            ?.galleries
            ?.map { it.galleryData }
            .orEmpty()
    }

    suspend fun getGalleries(galleryIds: List<String>): List<GalleryData> =
        if (galleryIds.isEmpty()) {
            listOf()
        } else {
            val query = client.query(FindGalleriesQuery(null, null, galleryIds))
            executeQuery(query)
                .data
                ?.findGalleries
                ?.galleries
                ?.map { it.galleryData }
                .orEmpty()
        }

    suspend fun getGallery(galleryId: String): GalleryData? {
        val query = client.query(GetGalleryQuery(id = galleryId))
        return executeQuery(query).data?.findGallery?.galleryData
    }

    /**
     * Search for a type of data with the given query. Users will need to cast the returned List.
     */
    suspend fun find(
        type: DataType,
        findFilter: FindFilterType,
        useRandom: Boolean = true,
    ): List<StashData> =
        when (type) {
            DataType.SCENE -> findScenes(findFilter, useRandom = useRandom)
            DataType.PERFORMER -> findPerformers(findFilter, useRandom = useRandom)
            DataType.TAG -> findTags(findFilter, useRandom = useRandom)
            DataType.STUDIO -> findStudios(findFilter, useRandom = useRandom)
            DataType.GROUP -> findGroups(findFilter, useRandom = useRandom)
            DataType.MARKER -> findMarkers(findFilter, useRandom = useRandom)
            DataType.IMAGE -> findImages(findFilter, useRandom = useRandom)
            DataType.GALLERY -> findGalleries(findFilter, useRandom = useRandom)
        }

    /**
     * Search for a type of data with the given query. Users will need to cast the returned List.
     */
    suspend fun getByIds(
        type: DataType,
        ids: List<String>,
    ): List<StashData> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return when (type) {
            DataType.SCENE -> findScenes(ids = ids)
            DataType.PERFORMER -> findPerformers(performerIds = ids)
            DataType.TAG -> getTags(ids)
            DataType.STUDIO -> findStudios(studioIds = ids)
            DataType.GROUP -> findGroups(groupIds = ids)
            DataType.IMAGE -> findImages(ids = ids)
            DataType.GALLERY -> findGalleries(galleryIds = ids)
            DataType.MARKER -> throw UnsupportedOperationException("Cannot query markers by ID") // TODO: Unsupported by the server
        }
    }

    suspend fun getSavedFilter(filterId: String): SavedFilter? {
        val query = FindSavedFilterQuery(filterId)
        return executeQuery(query).data?.findSavedFilter?.savedFilter
    }

    suspend fun getSavedFilters(dataType: DataType): List<SavedFilter> {
        val query = FindSavedFiltersQuery(dataType.filterMode)
        return executeQuery(query)
            .data
            ?.findSavedFilters
            ?.map { it.savedFilter }
            .orEmpty()
    }

    suspend fun getServerConfiguration(): ConfigurationQuery.Data {
        val query = ConfigurationQuery()
        return executeQuery(query).data!!
    }

    suspend fun getJob(jobId: String): StashJob? {
        val query = FindJobQuery(FindJobInput(jobId))
        return executeQuery(query).data?.findJob?.stashJob
    }

    suspend fun waitForJob(
        jobId: String,
        delay: Duration = 1.toDuration(DurationUnit.SECONDS),
        callback: ((StashJob) -> Unit)? = null,
    ): JobResult {
        var job = getJob(jobId) ?: return JobResult.NotFound
        while (job.status !in
            setOf(
                JobStatus.FINISHED,
                JobStatus.FAILED,
                JobStatus.CANCELLED,
            )
        ) {
            delay(delay)
            job = getJob(jobId) ?: return JobResult.NotFound
            callback?.invoke(job)
        }
        return if (job.status == JobStatus.FAILED) {
            JobResult.Failure(job.error)
        } else {
            JobResult.Success
        }
    }

    /**
     * Updates a FindFilterType if needed
     *
     * Handles updating the random sort if requested
     */
    fun updateFilter(
        filter: FindFilterType?,
        useRandom: Boolean = true,
    ): FindFilterType? =
        if (filter != null) {
            if (useRandom && filter.sort.getOrNull()?.startsWith("random") == true) {
                Log.v(TAG, "Updating random filter")
                filter.copy(sort = Optional.present("random_" + getRandomSort()))
            } else {
                filter
            }
        } else {
            null
        }

    companion object {
        private const val TAG = "QueryEngine"

        private val QUERY_ID = AtomicInteger(0)
    }

    open class QueryException(
        id: Int,
        queryName: String,
        msg: String? = null,
        cause: Exception? = null,
    ) : ServerCommunicationException(id, queryName, msg, cause)

    class StashNotConfiguredException : RuntimeException()
}
