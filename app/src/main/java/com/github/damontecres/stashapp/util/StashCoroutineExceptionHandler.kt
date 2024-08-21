package com.github.damontecres.stashapp.util

import android.util.Log
import android.widget.Toast
import com.github.damontecres.stashapp.StashApplication
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

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
            Toast.makeText(
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
