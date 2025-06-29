package com.github.damontecres.stashapp.util

import android.util.Log
import android.widget.Toast
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.util.plugin.CompanionPlugin
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

class LoggingCoroutineExceptionHandler(
    private val server: StashServer,
    private val scope: CoroutineScope,
    private val showToast: Boolean = true,
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
        val context = StashApplication.getApplication()
        Log.e(TAG, "Exception in coroutine", exception)

        val destination = StashApplication.navigationManager.previousDestination
        if (server.serverPreferences.companionPluginInstalled &&
            context.getPreference(R.string.pref_key_log_to_server, true)
        ) {
            scope.launchIO {
                val message =
                    "Exception: destination=$destination\n${exception.stackTraceToString()}"
                CompanionPlugin.sendLogMessage(server, message, true)
            }
        }

        if (showToast) {
            Toast
                .makeText(
                    context,
                    "Error: ${exception.localizedMessage.ifBlank { "Unknown error" }}",
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    companion object {
        const val TAG = "LoggingCoroutineExceptionHandler"
    }
}
