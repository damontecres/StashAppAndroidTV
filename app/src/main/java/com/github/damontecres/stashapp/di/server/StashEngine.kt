package com.github.damontecres.stashapp.di.server

import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Super class for "engines" that interact with the server
 */
abstract class StashEngine(
    protected val api: StashApi,
) {
    protected val client get() = api.apolloClient
    private var requestId: Int = 0
    protected val mutex = Mutex()

    protected suspend fun getRequestId() = mutex.withLock { requestId++ }

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
                Timber.e(ex, "Network error in $id $operationName")
                rethrow("Network error ($operationName): $causeMsg", ex)
            }

            is ApolloHttpException -> {
                Timber.e(ex, "HTTP ${ex.statusCode} error in $id $operationName")
                rethrow("HTTP ${ex.statusCode} ($operationName): $causeMsg", ex)
            }

            is ApolloException -> {
                Timber.e(ex, "ApolloException in $id $operationName")
                rethrow("Error with server ($operationName): $causeMsg", ex)
            }

            else -> {
                Timber.e(ex, "Exception in $id $operationName")
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
}
