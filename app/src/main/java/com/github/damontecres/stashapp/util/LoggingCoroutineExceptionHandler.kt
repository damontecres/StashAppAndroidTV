package com.github.damontecres.stashapp.util

import android.util.Log
import android.widget.Toast
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.util.plugin.CompanionPlugin
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

class LoggingCoroutineExceptionHandler(
    private val server: StashServer,
    private val scope: CoroutineScope,
    private val showToast: Boolean = true,
    private val toastMessage: String = "Error",
) : CoroutineExceptionHandler {
    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler

    override fun handleException(
        context: CoroutineContext,
        exception: Throwable,
    ) {
        handleException(exception)
    }

    fun handleException(exception: Throwable) {
        Log.e(TAG, "Exception in coroutine", exception)

        try {
            val context = StashApplication.getApplication()
            val destination = StashApplication.navigationManager.previousDestination
            if (server.serverPreferences.companionPluginInstalled &&
                getPreference(context, R.string.pref_key_log_to_server, true)
            ) {
                val compose = StashApplication.navigationManager is NavigationManagerCompose
                scope.launchIO {
                    val message =
                        "Exception: compose=$compose, destination=$destination\n${exception.stackTraceToString()}"
                    CompanionPlugin.sendLogMessage(server, message, true)
                }
            }

            if (showToast) {
                Toast
                    .makeText(
                        context,
                        "$toastMessage: ${exception.localizedMessage.ifBlank { "Unknown error" }}",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error while trying to log to server", ex)
        }
    }

    fun with(toastMessage: String) = LoggingCoroutineExceptionHandler(server, scope, showToast, toastMessage)

    companion object {
        const val TAG = "LoggingCoroutineExceptionHandler"
    }
}
