package com.github.damontecres.stashapp.ui

import com.github.damontecres.stashapp.di.server.ServerPreferences
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.components.StarRatingPrecision
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.models.CardUiSettings
import com.github.damontecres.stashapp.views.models.ServerViewModel.Companion.cardSettings

data class ComposeUiConfig(
    val preferences: StashPreferences,
    val ratingAsStars: Boolean,
    val starPrecision: StarRatingPrecision,
    val showStudioAsText: Boolean,
    val debugTextEnabled: Boolean,
    val showTitleDuringPlayback: Boolean,
    val readOnlyModeEnabled: Boolean,
    val showCardProgress: Boolean,
    val playSoundOnFocus: Boolean,
    val sfwMode: Boolean,
    val alwaysStartFromBeginning: Boolean,
    val minimumPlayPercent: Int,
    val persistVideoFilters: Boolean = true,
    val cardSettings: CardUiSettings,
) {
    val readOnlyModeDisabled = !readOnlyModeEnabled

    companion object {
        fun fromStashServer(
            preferences: StashPreferences,
            serverPreferences: ServerPreferences,
        ): ComposeUiConfig =
            ComposeUiConfig(
                preferences = preferences,
                ratingAsStars = serverPreferences.ratingsAsStars,
                starPrecision =
                    StarRatingPrecision.fromFloat(serverPreferences.starPrecision),
                showStudioAsText = serverPreferences.showStudioAsText,
                debugTextEnabled = preferences.playbackPreferences.showDebugInfo,
                cardSettings = preferences.cardSettings,
                showTitleDuringPlayback = true,
                showCardProgress = !serverPreferences.alwaysStartFromBeginning,
                playSoundOnFocus = preferences.interfacePreferences.playMovementSounds,
                readOnlyModeEnabled = preferences.pinPreferences.readOnlyPin.isNotNullOrBlank(),
                persistVideoFilters = preferences.playbackPreferences.saveVideoFilters,
                sfwMode = serverPreferences.sfwMode,
                minimumPlayPercent = serverPreferences.minimumPlayPercent,
                alwaysStartFromBeginning = serverPreferences.alwaysStartFromBeginning,
            )
    }
}
