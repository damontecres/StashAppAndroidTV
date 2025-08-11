package com.github.damontecres.stashapp.ui

import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.components.StarRatingPrecision
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.models.CardUiSettings
import com.github.damontecres.stashapp.views.models.ServerViewModel.Companion.cardSettings

data class ComposeUiConfig(
    val ratingAsStars: Boolean,
    val starPrecision: StarRatingPrecision,
    val showStudioAsText: Boolean,
    val debugTextEnabled: Boolean,
    val showTitleDuringPlayback: Boolean,
    val readOnlyModeEnabled: Boolean,
    val showCardProgress: Boolean,
    val playSoundOnFocus: Boolean,
    val persistVideoFilters: Boolean = true,
    val cardSettings: CardUiSettings,
) {
    val readOnlyModeDisabled = !readOnlyModeEnabled

    companion object {
        fun fromStashServer(
            preferences: StashPreferences,
            server: StashServer,
        ): ComposeUiConfig =
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
                debugTextEnabled = preferences.playbackPreferences.showDebugInfo,
                cardSettings = preferences.cardSettings,
                showTitleDuringPlayback = true,
                showCardProgress = !server.serverPreferences.alwaysStartFromBeginning,
                playSoundOnFocus = preferences.interfacePreferences.playMovementSounds,
                readOnlyModeEnabled = preferences.pinPreferences.readOnlyPin.isNotNullOrBlank(),
                persistVideoFilters = preferences.playbackPreferences.saveVideoFilters,
            )
    }
}
