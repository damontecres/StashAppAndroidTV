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
import com.github.damontecres.stashapp.api.FindMoviesQuery
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.FindSavedFilterQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.FindStudiosQuery
import com.github.damontecres.stashapp.api.FindTagsQuery
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Tag
import kotlin.random.Random

class QueryEngine(private val context: Context, private val showToasts: Boolean = false) {
    private val client = createApolloClient(context) ?: throw StashNotConfiguredException()

    private suspend fun <D : Operation.Data> executeQuery(query: ApolloCall<D>): ApolloResponse<D> {
        val queryName = query.operation.name()
        Log.d(TAG, "executeQuery $queryName: ${query.operation}")
        try {
            val response = query.execute()
            if (response.errors.isNullOrEmpty()) {
                Log.d(TAG, "executeQuery $queryName successful")
                return response
            } else {
                val errorMsgs = response.errors!!.map { it.message }.joinToString("\n")
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
        sceneIds: List<Int>? = null,
    ): List<SlimSceneData> {
        val query =
            client.query(
                FindScenesQuery(
                    filter = updateFilter(findFilter),
                    scene_filter = sceneFilter,
                    scene_ids = sceneIds,
                ),
            )
        val scenes =
            executeQuery(query).data?.findScenes?.scenes?.map {
                it.slimSceneData
            }
        return scenes.orEmpty()
    }

    suspend fun findPerformers(
        findFilter: FindFilterType? = null,
        performerFilter: PerformerFilterType? = null,
        performerIds: List<Int>? = null,
    ): List<PerformerData> {
        val query =
            client.query(
                FindPerformersQuery(
                    filter = updateFilter(findFilter),
                    performer_filter = performerFilter,
                    performer_ids = performerIds,
                ),
            )
        val performers =
            executeQuery(query).data?.findPerformers?.performers?.map {
                it.performerData
            }
        return performers.orEmpty()
    }

    // TODO Add studioIds?
    suspend fun findStudios(
        findFilter: FindFilterType? = null,
        studioFilter: StudioFilterType? = null,
    ): List<StudioData> {
        val query =
            client.query(
                FindStudiosQuery(
                    filter = updateFilter(findFilter),
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
    ): List<Tag> {
        val query =
            client.query(
                FindTagsQuery(
                    filter = updateFilter(findFilter),
                    tag_filter = tagFilter,
                ),
            )
        val tags =
            executeQuery(query).data?.findTags?.tags?.map { Tag(it.tagData) }
        return tags.orEmpty()
    }

    suspend fun findMovies(
        findFilter: FindFilterType? = null,
        movieFilter: MovieFilterType? = null,
    ): List<MovieData> {
        val query =
            client.query(
                FindMoviesQuery(
                    filter = updateFilter(findFilter),
                    movie_filter = movieFilter,
                ),
            )
        val tags = executeQuery(query).data?.findMovies?.movies?.map { it.movieData }
        return tags.orEmpty()
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
        }
    }

    suspend fun getSavedFilter(filterId: String): SavedFilterData? {
        val query = FindSavedFilterQuery(filterId)
        return executeQuery(query).data?.findSavedFilter?.savedFilterData
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
    private fun updateFilter(filter: FindFilterType?): FindFilterType? {
        return if (filter != null) {
            if (filter.sort.getOrNull()?.startsWith("random_") == true) {
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
