package com.github.damontecres.stashapp.ui.util

sealed interface DataLoadingState<out T> {
    data object Pending : DataLoadingState<Nothing>

    data object Loading : DataLoadingState<Nothing>

    data class Success<T>(
        val data: T,
    ) : DataLoadingState<T>

    data class Error(
        val message: String? = null,
        val exception: Throwable? = null,
    ) : DataLoadingState<Nothing> {
        constructor(exception: Throwable) : this(null, exception)

        val localizedMessage: String =
            listOfNotNull(message, exception?.localizedMessage).joinToString(" - ")
    }
}
