package com.github.damontecres.stashapp.util

import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

class StashCoroutineExceptionHandler(private val toast: Toast? = null) : CoroutineExceptionHandler {
    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler

    override fun handleException(
        context: CoroutineContext,
        exception: Throwable,
    ) {
        Log.e(TAG, "Exception in coroutine", exception)
        toast?.show()
    }

    companion object {
        const val TAG = "StashCoroutineExceptionHandler"
    }
}
