package com.github.damontecres.stashapp.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class StashEngine(
    private val context: Context,
    private val showToasts: Boolean = false,
) {
    protected val serverPreferences = ServerPreferences(context)
    protected val serverVersion = serverPreferences.serverVersion
    protected val client = StashClient.getApolloClient(context)

    protected suspend fun <T : Exception> createException(
        id: Int,
        queryName: String,
        ex: Exception,
        rethrow: (Int, String, Exception) -> T,
    ): T {
        return when (ex) {
            is ApolloNetworkException -> {
                showToast("Network error ($queryName). Message: ${ex.message}, ${ex.cause?.message}")
                Log.e(TAG, "Network error in $id $queryName", ex)
                rethrow(id, "Network error ($queryName)", ex)
            }

            is ApolloHttpException -> {
                showToast("HTTP error ($queryName). Status=${ex.statusCode}, Msg=${ex.message}, ${ex.cause?.message}")
                Log.e(TAG, "HTTP ${ex.statusCode} error in $id $queryName", ex)
                rethrow(id, "HTTP ${ex.statusCode} ($queryName)", ex)
            }

            is ApolloException -> {
                showToast("Server query error ($queryName). Msg=${ex.message}, ${ex.cause?.message}")
                Log.e(TAG, "ApolloException in $id $queryName", ex)
                rethrow(id, "Apollo exception ($queryName)", ex)
            }

            else -> {
                showToast("Error ($queryName). Msg=${ex.message}, ${ex.cause?.message}")
                Log.e(TAG, "Error in $id $queryName", ex)
                rethrow(id, "Apollo exception ($queryName)", ex)
            }
        }
    }

    protected suspend fun showToast(message: String) {
        if (showToasts) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val TAG = "StashEngine"
    }
}
