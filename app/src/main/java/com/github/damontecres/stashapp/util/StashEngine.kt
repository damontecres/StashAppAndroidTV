package com.github.damontecres.stashapp.util

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException

/**
 * Super class for "engines" that interact with the server
 */
abstract class StashEngine(
    protected val server: StashServer,
    protected val client: ApolloClient,
) {
    protected val serverPreferences = server.serverPreferences
    protected val serverVersion = serverPreferences.serverVersion

    protected fun <T : ServerCommunicationException> createException(
        id: Int,
        operationName: String,
        ex: Exception,
        rethrow: (String, Exception) -> T,
    ): T {
        val causeMsg =
            if (ex.message.isNotNullOrBlank()) {
                ex.message
            } else {
                ex.cause?.message
            }
        return when (ex) {
            is ApolloNetworkException -> {
                Log.e(TAG, "Network error in $id $operationName", ex)
                rethrow("Network error ($operationName): $causeMsg", ex)
            }

            is ApolloHttpException -> {
                Log.e(TAG, "HTTP ${ex.statusCode} error in $id $operationName", ex)
                rethrow("HTTP ${ex.statusCode} ($operationName): $causeMsg", ex)
            }

            is ApolloException -> {
                Log.e(TAG, "ApolloException in $id $operationName", ex)
                rethrow("Error with server ($operationName): $causeMsg", ex)
            }

            else -> {
                Log.e(TAG, "Exception in $id $operationName", ex)
                rethrow("Error with $operationName: $causeMsg", ex)
            }
        }
    }

    open class ServerCommunicationException(
        val operationId: Int,
        val operationName: String,
        message: String? = null,
        cause: Exception? = null,
    ) : Exception(message, cause)

    companion object {
        private const val TAG = "StashEngine"
    }
}
