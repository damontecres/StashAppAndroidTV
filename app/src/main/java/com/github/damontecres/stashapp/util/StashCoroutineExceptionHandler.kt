package com.github.damontecres.stashapp.util

import android.util.Log
import android.widget.Toast
import com.github.damontecres.stashapp.StashApplication
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

/**
 * A general [CoroutineExceptionHandler] which can optionally show [Toast]s when an exception is thrown
 *
 * Note: a toast will be shown for each parameter given, up to three!
 *
 * @param autoToast automatically show a toast with the exception's message
 * @param toast the toast to show when an exception occurs
 * @param makeToast make a toast to show when an exception occurs
 */
class StashCoroutineExceptionHandler(
    private val autoToast: Boolean = false,
    private val toast: Toast? = null,
    private val makeToast: ((Throwable) -> Toast)? = null,
) : CoroutineExceptionHandler {
    constructor(
        toast: Toast? = null,
        makeToast: ((Throwable) -> Toast)? = null,
    ) : this(false, toast, makeToast)

    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler

    override fun handleException(
        context: CoroutineContext,
        exception: Throwable,
    ) {
        Log.e(TAG, "Exception in coroutine", exception)
        toast?.show()
        makeToast?.let { it(exception).show() }
        if (autoToast) {
            Toast
                .makeText(
                    StashApplication.getApplication(),
                    "Error: ${exception.message}",
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    companion object {
        const val TAG = "StashCoroutineExceptionHandler"
    }
}
