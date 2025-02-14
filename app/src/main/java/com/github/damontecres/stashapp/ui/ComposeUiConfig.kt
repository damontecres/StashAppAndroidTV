package com.github.damontecres.stashapp.ui

import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashServer

data class ComposeUiConfig(
    val ratingAsStars: Boolean,
    val starPrecision: Float,
    val showStudioAsText: Boolean,
) {
    companion object {
        fun fromStashServer(server: StashServer): ComposeUiConfig =
            ComposeUiConfig(
                ratingAsStars = server.serverPreferences.ratingsAsStars,
                starPrecision =
                    server.serverPreferences.preferences.getFloat(
                        ServerPreferences.PREF_RATING_PRECISION,
                        1.0f,
                    ),
                showStudioAsText = server.serverPreferences.showStudioAsText,
            )
    }
}
