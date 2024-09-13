package com.github.damontecres.stashapp.util

import android.content.Context
import android.util.Log
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.ConfigurationQuery
import com.github.damontecres.stashapp.api.FindDefaultFilterQuery
import com.github.damontecres.stashapp.api.FindGalleriesQuery
import com.github.damontecres.stashapp.api.FindImageQuery
import com.github.damontecres.stashapp.api.FindImagesQuery
import com.github.damontecres.stashapp.api.FindJobQuery
import com.github.damontecres.stashapp.api.FindMarkersQuery
import com.github.damontecres.stashapp.api.FindMovieQuery
import com.github.damontecres.stashapp.api.FindMoviesQuery
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.FindSavedFilterQuery
import com.github.damontecres.stashapp.api.FindSavedFiltersQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.FindStudiosQuery
import com.github.damontecres.stashapp.api.FindTagsQuery
import com.github.damontecres.stashapp.api.GetExtraImageQuery
import com.github.damontecres.stashapp.api.GetSceneQuery
import com.github.damontecres.stashapp.api.fragment.ExtraImageData
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.FindJobInput
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.JobStatus
import com.github.damontecres.stashapp.api.type.MovieFilterType
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
 *
 * @param context
 * @param showToasts show a toast when errors occur
 */
class QueryEngine(
    context: Context,
    server: StashServer,
    showToasts: Boolean = false,
) : StashEngine(context, server, showToasts) {
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
                throw createException(id, queryName, response.exception!!) { id, msg, ex ->
                    QueryException(id, msg, ex)
                }
            } else {
                val errorMsgs = response.errors!!.joinToString("\n") { it.message }
                showToast("${response.errors!!.size} errors in response ($queryName)\n$errorMsgs")
                Log.e(TAG, "Errors in $id $queryName: ${response.errors}")
                throw QueryException(
                    id,
                    "($queryName), ${response.errors!!.size} errors in graphql response",
                )
            }
        }

    suspend fun <D : Query.Data> executeQuery(query: Query<D>): ApolloResponse<D> {
        return executeQuery(client.query(query))
    }

    suspend fun findScenes(
        findFilter: FindFilterType? = null,
        sceneFilter: SceneFilterType? = null,
        useRandom: Boolean = true,
    ): List<SlimSceneData> {
        val query =
            client.query(
                FindScenesQuery(
                    filter = updateFilter(findFilter, useRandom),
                    scene_filter = sceneFilter,
                    ids = null,
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
        return findPerformers(performerIds = listOf(performerId)).firstOrNull()
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
            executeQuery(query).data?.findTags?.tags?.map { it.tagData }
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
            executeQuery(query).data?.findTags?.tags?.map { it.tagData }
        return tags.orEmpty()
    }

    suspend fun findMovies(
        findFilter: FindFilterType? = null,
        movieFilter: MovieFilterType? = null,
        useRandom: Boolean = true,
    ): List<MovieData> {
        val query =
            client.query(
                FindMoviesQuery(
                    filter = updateFilter(findFilter, useRandom),
                    movie_filter = movieFilter,
                ),
            )
        val tags = executeQuery(query).data?.findMovies?.movies?.map { it.movieData }
        return tags.orEmpty()
    }

    suspend fun getMovie(movieId: String): MovieData? {
        val query = client.query(FindMovieQuery(movieId))
        return executeQuery(query).data?.findMovie?.movieData
    }

    suspend fun findMarkers(
        findFilter: FindFilterType? = null,
        markerFilter: SceneMarkerFilterType? = null,
        useRandom: Boolean = true,
    ): List<MarkerData> {
        val query =
            client.query(
                FindMarkersQuery(
                    filter = updateFilter(findFilter, useRandom),
                    scene_marker_filter = markerFilter,
                ),
            )
        return executeQuery(query).data?.findSceneMarkers?.scene_markers?.map { it.markerData }
            .orEmpty()
    }

    suspend fun findImages(
        findFilter: FindFilterType? = null,
        imageFilter: ImageFilterType? = null,
        useRandom: Boolean = true,
    ): List<ImageData> {
        val query =
            client.query(
                FindImagesQuery(
                    updateFilter(findFilter, useRandom),
                    imageFilter,
                ),
            )
        return executeQuery(query).data?.findImages?.images?.map { it.imageData }.orEmpty()
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
        return executeQuery(query).data?.findGalleries?.galleries?.map { it.galleryData }.orEmpty()
    }

    suspend fun getGalleries(galleryIds: List<String>): List<GalleryData> {
        return if (galleryIds.isEmpty()) {
            listOf()
        } else {
            val query = client.query(FindGalleriesQuery(null, null, galleryIds))
            executeQuery(query).data?.findGalleries?.galleries?.map { it.galleryData }
                .orEmpty()
        }
    }

    /**
     * Search for a type of data with the given query. Users will need to cast the returned List.
     */
    suspend fun find(
        type: DataType,
        findFilter: FindFilterType,
        useRandom: Boolean = true,
    ): List<*> {
        return when (type) {
            DataType.SCENE -> findScenes(findFilter, useRandom = useRandom)
            DataType.PERFORMER -> findPerformers(findFilter, useRandom = useRandom)
            DataType.TAG -> findTags(findFilter, useRandom = useRandom)
            DataType.STUDIO -> findStudios(findFilter, useRandom = useRandom)
            DataType.MOVIE -> findMovies(findFilter, useRandom = useRandom)
            DataType.MARKER -> findMarkers(findFilter, useRandom = useRandom)
            DataType.IMAGE -> findImages(findFilter, useRandom = useRandom)
            DataType.GALLERY -> findGalleries(findFilter, useRandom = useRandom)
        }
    }

    suspend fun getSavedFilter(filterId: String): SavedFilterData? {
        val query = FindSavedFilterQuery(filterId)
        return executeQuery(query).data?.findSavedFilter?.savedFilterData
    }

    suspend fun getSavedFilters(dataType: DataType): List<SavedFilterData> {
        val query = FindSavedFiltersQuery(dataType.filterMode)
        return executeQuery(query).data?.findSavedFilters?.map { it.savedFilterData }.orEmpty()
    }

    suspend fun getDefaultFilter(type: DataType): SavedFilterData? {
        val query = FindDefaultFilterQuery(type.filterMode)
        return executeQuery(query).data?.findDefaultFilter?.savedFilterData
    }

    suspend fun getServerConfiguration(): ConfigurationQuery.Data {
        val query = ConfigurationQuery()
        return executeQuery(query).data!!
    }

    suspend fun waitForJob(
        jobId: String,
        delay: Duration = 1.toDuration(DurationUnit.SECONDS),
    ): JobResult {
        val query = FindJobQuery(FindJobInput((jobId)))
        var job: FindJobQuery.FindJob? =
            executeQuery(query).data?.findJob ?: return JobResult.NotFound
        while (job?.status !in
            setOf(
                JobStatus.FINISHED,
                JobStatus.FAILED,
            )
        ) {
            delay(delay)
            job = executeQuery(query).data?.findJob ?: return JobResult.NotFound
        }
        if (job?.status == JobStatus.FAILED) {
            return JobResult.Failure(job.error)
        } else {
            return JobResult.Success
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
    ): FindFilterType? {
        return if (filter != null) {
            if (useRandom && filter.sort.getOrNull()?.startsWith("random") == true) {
                Log.v(TAG, "Updating random filter")
                filter.copy(sort = Optional.present(getRandomSort()))
            } else {
                filter
            }
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "QueryEngine"

        private val QUERY_ID = AtomicInteger(0)
    }

    open class QueryException(val id: Int, msg: String? = null, cause: Exception? = null) :
        RuntimeException(msg, cause)

    class StashNotConfiguredException : RuntimeException()
}
