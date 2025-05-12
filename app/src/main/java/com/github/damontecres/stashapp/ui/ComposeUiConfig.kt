package com.github.damontecres.stashapp.ui

import android.content.Context
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.components.StarRatingPrecision
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.views.models.CardUiSettings
import com.github.damontecres.stashapp.views.models.ServerViewModel

data class ComposeUiConfig(
    val ratingAsStars: Boolean,
    val starPrecision: StarRatingPrecision,
    val showStudioAsText: Boolean,
    val debugTextEnabled: Boolean,
    val showTitleDuringPlayback: Boolean,
    val readOnlyModeEnabled: Boolean,
    val showCardProgress: Boolean,
    val playSoundOnFocus: Boolean,
    val cardSettings: CardUiSettings,
) {
    val readOnlyModeDisabled = !readOnlyModeEnabled

    companion object {
        fun fromStashServer(
            context: Context,
            server: StashServer,
        ): ComposeUiConfig {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return ComposeUiConfig(
                ratingAsStars = server.serverPreferences.ratingsAsStars,
                starPrecision =
                    StarRatingPrecision.fromFloat(
                        server.serverPreferences.preferences.getFloat(
                            ServerPreferences.PREF_RATING_PRECISION,
                            1.0f,
                        ),
                    ),
                showStudioAsText = server.serverPreferences.showStudioAsText,
                debugTextEnabled =
                    prefs.getBoolean(
                        context.getString(R.string.pref_key_show_playback_debug_info),
                        false,
                    ),
                cardSettings = ServerViewModel.createUiSettings(context),
                showTitleDuringPlayback = prefs.getBoolean("exoShowTitle", true),
                showCardProgress = !server.serverPreferences.alwaysStartFromBeginning,
                playSoundOnFocus =
                    prefs.getBoolean(
                        context.getString(R.string.pref_key_movement_sounds),
                        true,
                    ),
                readOnlyModeEnabled =
                    prefs.getBoolean(
                        context.getString(R.string.pref_key_read_only_mode),
                        false,
                    ),
            )
        }
    }
}
