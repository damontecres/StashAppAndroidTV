package com.github.damontecres.stashapp.data

sealed class JobResult {
    data object NotFound : JobResult()

    data class Failure(
        val message: String?,
    ) : JobResult()

    data object Success : JobResult()
}
