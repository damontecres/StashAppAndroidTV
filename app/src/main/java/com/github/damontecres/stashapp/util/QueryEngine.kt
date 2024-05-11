package com.github.damontecres.stashapp.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.github.damontecres.stashapp.api.FindDefaultFilterQuery
import com.github.damontecres.stashapp.api.FindGalleriesQuery
import com.github.damontecres.stashapp.api.FindGalleryQuery
import com.github.damontecres.stashapp.api.FindImageQuery
import com.github.damontecres.stashapp.api.FindImagesQuery
import com.github.damontecres.stashapp.api.FindMarkersQuery
import com.github.damontecres.stashapp.api.FindMovieQuery
import com.github.damontecres.stashapp.api.FindMoviesQuery
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.FindSavedFilterQuery
import com.github.damontecres.stashapp.api.FindSavedFiltersQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.FindStudiosQuery
import com.github.damontecres.stashapp.api.FindTagsQuery
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
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReadWriteLock
import kotlin.random.Random

/**
 * Handles making graphql queries to the server
 *
 * @param context
 * @param showToasts show a toast when errors occur
 * @param lock an optional lock, when if shared with [MutationEngine], can prevent race conditions
 */
class QueryEngine(
    private val context: Context,
    private val showToasts: Boolean = false,
    lock: ReadWriteLock? = null,
) {
    private val client = createApolloClient(context) ?: throw StashNotConfiguredException()

    private val readLock = lock?.readLock()

    private suspend fun <D : Operation.Data> executeQuery(query: ApolloCall<D>): ApolloResponse<D> {
        val queryName = query.operation.name()
        Log.v(
            TAG,
            "executeQuery $queryName",
        )
        try {
            val response =
                withContext(Dispatchers.IO) {
                    try {
                        readLock?.lock()
                        query.execute()
                    } finally {
                        readLock?.unlock()
                    }
                }
            if (response.errors.isNullOrEmpty()) {
                Log.v(TAG, "executeQuery $queryName successful")
                return response
            } else {
                val errorMsgs = response.errors!!.joinToString("\n") { it.message }
                if (showToasts) {
                    Toast.makeText(
                        context,
                        "${response.errors!!.size} errors in response ($queryName)\n$errorMsgs",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                Log.e(TAG, "Errors in $queryName: ${response.errors}")
                throw QueryException("($queryName), ${response.errors!!.size} errors in graphql response")
            }
        } catch (ex: ApolloNetworkException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "Network error ($queryName). Message: ${ex.message}, ${ex.cause?.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
            Log.e(TAG, "Network error in $queryName", ex)
            throw QueryException("Network error ($queryName)", ex)
        } catch (ex: ApolloHttpException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "HTTP error ($queryName). Status=${ex.statusCode}, Msg=${ex.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
            Log.e(TAG, "HTTP ${ex.statusCode} error in $queryName", ex)
            throw QueryException("HTTP ${ex.statusCode} ($queryName)", ex)
        } catch (ex: ApolloException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "Server query error ($queryName). Msg=${ex.message}, ${ex.cause?.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
            Log.e(TAG, "ApolloException in $queryName", ex)
            throw QueryException("Apollo exception ($queryName)", ex)
        }
    }

    suspend fun <D : Query.Data> executeQuery(query: Query<D>): ApolloResponse<D> {
        return executeQuery(client.query(query))
    }

    suspend fun findScenes(
        findFilter: FindFilterType? = null,
        sceneFilter: SceneFilterType? = null,
        sceneIds: List<String>? = null,
        useRandom: Boolean = true,
    ): List<SlimSceneData> {
        val query =
            client.query(
                FindScenesQuery(
                    filter = updateFilter(findFilter, useRandom),
                    scene_filter = sceneFilter,
                    scene_ids = sceneIds?.map { it.toInt() },
                ),
            )
        val scenes =
            executeQuery(query).data?.findScenes?.scenes?.map {
                it.slimSceneData
            }
        return scenes.orEmpty()
    }

    suspend fun getScene(sceneId: String): SlimSceneData? {
        return findScenes(sceneIds = listOf(sceneId)).firstOrNull()
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
                    performer_ids = performerIds?.map { it.toInt() },
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

    // TODO Add studioIds?
    suspend fun findStudios(
        findFilter: FindFilterType? = null,
        studioFilter: StudioFilterType? = null,
        useRandom: Boolean = true,
    ): List<StudioData> {
        val query =
            client.query(
                FindStudiosQuery(
                    filter = updateFilter(findFilter, useRandom),
                    studio_filter = studioFilter,
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
        return query.execute().data?.findMovie?.movieData
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

    suspend fun findGalleries(
        findFilter: FindFilterType? = null,
        galleryFilter: GalleryFilterType? = null,
        useRandom: Boolean = true,
    ): List<GalleryData> {
        val query =
            client.query(FindGalleriesQuery(updateFilter(findFilter, useRandom), galleryFilter))
        return executeQuery(query).data?.findGalleries?.galleries?.map { it.galleryData }.orEmpty()
    }

    suspend fun getGallery(galleryId: String): GalleryData? {
        val query = client.query(FindGalleryQuery(galleryId))
        return executeQuery(query).data?.findGallery?.galleryData
    }

    /**
     * Search for a type of data with the given query. Users will need to cast the returned List.
     */
    suspend fun find(
        type: DataType,
        findFilter: FindFilterType,
    ): List<*> {
        return when (type) {
            DataType.SCENE -> findScenes(findFilter)
            DataType.PERFORMER -> findPerformers(findFilter)
            DataType.TAG -> findTags(findFilter)
            DataType.STUDIO -> findStudios(findFilter)
            DataType.MOVIE -> findMovies(findFilter)
            DataType.MARKER -> findMarkers(findFilter)
            DataType.IMAGE -> findImages(findFilter)
            DataType.GALLERY -> findGalleries(findFilter)
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
            if (useRandom && filter.sort.getOrNull()?.startsWith("random_") == true) {
                Log.v(TAG, "Updating random filter")
                filter.copy(sort = Optional.present("random_" + Random.nextInt(1e8.toInt())))
            } else {
                filter
            }
        } else {
            null
        }
    }

    companion object {
        const val TAG = "QueryEngine"
    }

    open class QueryException(msg: String? = null, cause: ApolloException? = null) :
        RuntimeException(msg, cause)

    class StashNotConfiguredException : QueryException()
}
