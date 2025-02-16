package com.github.damontecres.stashapp.util

sealed class SkipParams(
    val skipForward: Long,
    val skipBack: Long,
) {
    data object Default : SkipParams(-1, -1)

    class Values(
        skipForward: Long,
        skipBack: Long,
    ) : SkipParams(skipForward, skipBack)
}
