package com.github.damontecres.stashapp.ui

import com.github.damontecres.stashapp.util.StashServer

data class ComposeUiConfig(
    val ratingAsStars: Boolean,
) {
    companion object {
        fun fromStashServer(server: StashServer): ComposeUiConfig =
            ComposeUiConfig(
                ratingAsStars = server.serverPreferences.ratingsAsStars,
            )
    }
}
