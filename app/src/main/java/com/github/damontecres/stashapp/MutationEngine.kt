package com.github.damontecres.stashapp

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.github.damontecres.stashapp.api.SceneSaveActivityMutation

/**
 * Class for sending graphql mutations
 */
class MutationEngine(private val context: Context, private val showToasts: Boolean = false) {
    private val client =
        createApolloClient(context) ?: throw QueryEngine.StashNotConfiguredException()

    private suspend fun <D : Mutation.Data> executeMutation(mutation: Mutation<D>): ApolloResponse<D> {
        val mutationName = mutation.name()
        try {
            val response = client.mutation(mutation).execute()
            if (response.errors.isNullOrEmpty()) {
                Log.d(TAG, "executeMutation $mutationName successful")
                return response
            } else {
                val errorMsgs = response.errors!!.map { it.message }.joinToString("\n")
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
    ): Boolean {
        Log.v(TAG, "SceneSaveActivity sceneId=$sceneId, position=$position")
        val resumeTime = position / 1000.0
        val mutation =
            SceneSaveActivityMutation(scene_id = sceneId.toString(), resume_time = resumeTime)
        val result = executeMutation(mutation)
        return result.data!!.sceneSaveActivity
    }

    companion object {
        const val TAG = "MutationEngine"
    }

    open class MutationException(msg: String? = null, cause: ApolloException? = null) :
        RuntimeException(msg, cause)
}
