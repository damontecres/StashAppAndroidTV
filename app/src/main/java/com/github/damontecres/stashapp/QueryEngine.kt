package com.github.damontecres.stashapp

import android.content.Context
import android.widget.Toast
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.FindStudiosQuery
import com.github.damontecres.stashapp.api.FindTagsQuery
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.Tag
import com.github.damontecres.stashapp.data.fromFindTag

class QueryEngine(private val context: Context, private val showToasts: Boolean = false) {

    private val client = createApolloClient(context) ?: throw StashNotConfiguredException()

    private suspend fun <D : Operation.Data> executeQuery(query: ApolloCall<D>): ApolloResponse<D> {
        val queryName = query.operation.name()
        try {
            val response = query.execute()
            if (response.errors.isNullOrEmpty()) {
                return response
            } else {
                val errorMsgs = response.errors!!.map { it.message }.joinToString("\n")
                if (showToasts) {
                    Toast.makeText(
                        context,
                        "${response.errors!!.size} errors in response ($queryName)\n$errorMsgs",
                        Toast.LENGTH_LONG
                    ).show()
                }
                throw QueryException("($queryName), ${response.errors!!.size} errors in graphql response")
            }
        } catch (ex: ApolloNetworkException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "Network error ($queryName). Message: ${ex.message}, ${ex.cause?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            throw QueryException("Network error ($queryName)", ex)
        } catch (ex: ApolloHttpException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "HTTP error ($queryName). Status=${ex.statusCode}, Msg=${ex.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            throw QueryException("HTTP ${ex.statusCode} ($queryName)", ex)
        } catch (ex: ApolloException) {
            if (showToasts) {
                Toast.makeText(
                    context,
                    "Server query error ($queryName). Msg=${ex.message}, ${ex.cause?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            throw QueryException("Apollo exception ($queryName)", ex)
        }
    }

    suspend fun <D : Query.Data> executeQuery(query: Query<D>): ApolloResponse<D> {
        return executeQuery(client.query(query))
    }

    suspend fun findScenes(
        findFilter: FindFilterType? = null,
        sceneFilter: SceneFilterType? = null,
        sceneIds: List<Int>? = null
    ): List<SlimSceneData> {
        val query = client.query(
            FindScenesQuery(
                filter = Optional.presentIfNotNull(findFilter),
                scene_filter = Optional.presentIfNotNull(sceneFilter),
                scene_ids = Optional.presentIfNotNull(sceneIds)
            )
        )
        val scenes = executeQuery(query).data?.findScenes?.scenes?.map {
            it.slimSceneData
        }
        return scenes.orEmpty()
    }

    suspend fun findPerformers(
        findFilter: FindFilterType? = null,
        performerFilter: PerformerFilterType? = null,
        performerIds: List<Int>? = null
    ): List<PerformerData> {
        val query = client.query(
            FindPerformersQuery(
                filter = Optional.presentIfNotNull(findFilter),
                performer_filter = Optional.presentIfNotNull(performerFilter),
                performer_ids = Optional.presentIfNotNull(performerIds)
            )
        )
        val performers = executeQuery(query).data?.findPerformers?.performers?.map {
            it.performerData
        }
        return performers.orEmpty()
    }

    // TODO Add studioIds?
    suspend fun findStudios(
        findFilter: FindFilterType? = null,
        studioFilter: StudioFilterType? = null
    ): List<StudioData> {
        val query = client.query(
            FindStudiosQuery(
                filter = Optional.presentIfNotNull(findFilter),
                studio_filter = Optional.presentIfNotNull(studioFilter),
            )
        )
        val studios = executeQuery(query).data?.findStudios?.studios?.map {
            it.studioData
        }
        return studios.orEmpty()
    }

    suspend fun findTags(
        findFilter: FindFilterType? = null,
        tagFilter: TagFilterType? = null
    ): List<Tag> {
        val query = client.query(
            FindTagsQuery(
                filter = Optional.presentIfNotNull(findFilter),
                tag_filter = Optional.presentIfNotNull(tagFilter)
            )
        )
        val tags =
            executeQuery(query).data?.findTags?.tags?.map { fromFindTag(it) }
        return tags.orEmpty()
    }

    open class QueryException(msg: String? = null, cause: ApolloException? = null) :
        RuntimeException(msg, cause) {
    }

    class StashNotConfiguredException : QueryException() {
    }
}