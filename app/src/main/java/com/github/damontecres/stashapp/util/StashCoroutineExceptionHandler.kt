package com.github.damontecres.stashapp.util

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

class StashCoroutineExceptionHandler : CoroutineExceptionHandler {
    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler

    override fun handleException(
        context: CoroutineContext,
        exception: Throwable,
    ) {
        Log.e(TAG, "Exception in coroutine", exception)
    }

    companion object {
        const val TAG = "StashCoroutineExceptionHandler"
    }
}
