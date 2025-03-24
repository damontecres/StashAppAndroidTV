package com.github.damontecres.stashapp.ui

import com.github.damontecres.stashapp.ui.components.StarRatingPrecision
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashServer

data class ComposeUiConfig(
    val ratingAsStars: Boolean,
    val starPrecision: StarRatingPrecision,
    val showStudioAsText: Boolean,
    val debugTextEnabled: Boolean = false,
) {
    companion object {
        fun fromStashServer(server: StashServer): ComposeUiConfig =
            ComposeUiConfig(
                ratingAsStars = server.serverPreferences.ratingsAsStars,
                starPrecision =
                    StarRatingPrecision.fromFloat(
                        server.serverPreferences.preferences.getFloat(
                            ServerPreferences.PREF_RATING_PRECISION,
                            1.0f,
                        ),
                    ),
                showStudioAsText = server.serverPreferences.showStudioAsText,
            )
    }
}
