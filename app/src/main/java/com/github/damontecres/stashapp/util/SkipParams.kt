package com.github.damontecres.stashapp.util

sealed interface SkipParams {
    data object Default : SkipParams

    data class Values(
        val skipForward: Long,
        val skipBack: Long,
    ) : SkipParams
}
