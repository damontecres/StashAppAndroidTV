package com.github.damontecres.stashapp.ui.components.prefs

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.proto.PlaybackFinishBehavior
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.updateInterfacePreferences
import com.github.damontecres.stashapp.util.updatePinPreferences
import com.github.damontecres.stashapp.util.updatePlaybackPreferences
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A group of preferences
 */
data class PreferenceGroup(
    @param:StringRes val title: Int,
    val preferences: List<StashPreference<out Any?>>,
)

/**
 * Results when validating a preference value.
 */
sealed interface PreferenceValidation {
    data object Valid : PreferenceValidation

    data class Invalid(
        val message: String,
    ) : PreferenceValidation
}

/**
 * A preference that can be stored in the shared preferences.
 *
 * @param T The type of the preference value.
 */
sealed interface StashPreference<T> {
    @get:StringRes
    val title: Int

    val defaultValue: T

    val getter: (prefs: StashPreferences) -> T

    val setter: (prefs: StashPreferences, value: T) -> StashPreferences

    fun summary(
        context: Context,
        value: T?,
    ): String? = null

    fun validate(value: T): PreferenceValidation = PreferenceValidation.Valid

    companion object {
        val CurrentServer =
            StashClickablePreference(
                title = R.string.current_server,
            )
        val ManageServers =
            StashClickablePreference(
                title = R.string.manage_servers,
                summary = R.string.add_remove_servers_summary,
            )
        val AutoSubmitPin =
            StashSwitchPreference(
                title = R.string.auto_submit_pin,
                summary = R.string.auto_submit_pin_summary,
                defaultValue = true,
                getter = { it.pinPreferences.autoSubmit },
                setter = { prefs, value ->
                    prefs.updatePinPreferences { autoSubmit = value }
                },
            )
        val PinCode =
            StashPinPreference(
                title = R.string.pin_code,
                defaultValue = "",
                description = R.string.set_app_pin_code,
                getter = { it.pinPreferences.pin },
                setter = { prefs, value ->
                    prefs.updatePinPreferences { pin = value }
                },
            )
        val CardSize =
            StashChoicePreference<Int>(
                title = R.string.card_size_title,
                defaultValue = 5,
                displayValues = R.array.card_sizes,
                indexToValue = { listOf(7, 6, 5, 4, 3)[it] },
                valueToIndex = { listOf(7, 6, 5, 4, 3).indexOf(it) },
                getter = { it.interfacePreferences.cardSize },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences { cardSize = value }
                },
            )
        val PlayVideoPreviews =
            StashSwitchPreference(
                title = R.string.play_video_previews,
                defaultValue = true,
                getter = { it.interfacePreferences.playVideoPreviews },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences { playVideoPreviews = value }
                },
                summaryOn = R.string.play_video_previews_summary_on,
                summaryOff = R.string.play_video_previews_summary_off,
            )
        val MoreUiSettings =
            StashClickablePreference(
                title = R.string.more_ui_settings,
                summary = R.string.more_ui_settings_summary,
            )

        val SkipForward =
            StashSliderPreference(
                title = R.string.skip_forward_preference,
                defaultValue = 30,
                min = 10,
                max = 5.minutes.inWholeSeconds.toInt(),
                interval = 5,
                getter = {
                    it.playbackPreferences.skipForwardMs
                        .milliseconds.inWholeSeconds
                        .toInt()
                },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences {
                        skipForwardMs = value.seconds.inWholeMilliseconds
                    }
                },
                summarizer = { value ->
                    if (value != null) {
                        "$value seconds"
                    } else {
                        null
                    }
                },
            )

        val SkipBack =
            StashSliderPreference(
                title = R.string.skip_back_preference,
                defaultValue = 10,
                min = 5,
                max = 5.minutes.inWholeSeconds.toInt(),
                interval = 5,
                getter = {
                    it.playbackPreferences.skipBackwardMs
                        .milliseconds.inWholeSeconds
                        .toInt()
                },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences {
                        skipBackwardMs = value.seconds.inWholeMilliseconds
                    }
                },
                summarizer = { value ->
                    if (value != null) {
                        "$value seconds"
                    } else {
                        null
                    }
                },
            )
        val FinishedBehavior =
            StashChoicePreference<PlaybackFinishBehavior>(
                title = R.string.playback_finished_behavior,
                defaultValue = PlaybackFinishBehavior.PLAYBACK_FINISH_BEHAVIOR_DO_NOTHING,
                displayValues = R.array.playback_finished_behavior_options,
                indexToValue = {
                    PlaybackFinishBehavior.forNumber(it)
                },
                valueToIndex = { it.number },
                getter = { it.playbackPreferences.playbackFinishBehavior },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences {
                        playbackFinishBehavior = value
                    }
                },
            )

        val ReadOnlyMode =
            StashPinPreference(
                title = R.string.read_only_mode,
                defaultValue = "",
                description = R.string.read_only_pin_description,
                getter = { it.pinPreferences.readOnlyPin },
                setter = { prefs, value ->
                    prefs.updatePinPreferences { readOnlyPin = value }
                },
            )
    }
}

data class StashSwitchPreference(
    @get:StringRes override val title: Int,
    override val defaultValue: Boolean,
    override val getter: (prefs: StashPreferences) -> Boolean,
    override val setter: (prefs: StashPreferences, value: Boolean) -> StashPreferences,
    val validator: (value: Boolean) -> PreferenceValidation = { PreferenceValidation.Valid },
    @param:StringRes val summary: Int? = null,
    @param:StringRes val summaryOn: Int? = null,
    @param:StringRes val summaryOff: Int? = null,
) : StashPreference<Boolean> {
    override fun summary(
        context: Context,
        value: Boolean?,
    ): String? =
        when {
            summaryOn != null && value == true -> context.getString(summaryOn)
            summaryOff != null && value == false -> context.getString(summaryOff)
            else -> summary?.let { context.getString(summary) }
        }
}

abstract class StashStringPreference(
    @param:StringRes override val title: Int,
    override val defaultValue: String,
) : StashPreference<String> {
    override fun summary(
        context: Context,
        value: String?,
    ): String? = value
}

class StashPinPreference(
    @StringRes title: Int,
    defaultValue: String = "",
    @param:StringRes val description: Int,
    override val getter: (prefs: StashPreferences) -> String,
    override val setter: (prefs: StashPreferences, value: String) -> StashPreferences,
) : StashStringPreference(
        title,
        defaultValue,
    ) {
    override fun validate(value: String): PreferenceValidation =
        if (value.isBlank() || value.toIntOrNull() != null) {
            PreferenceValidation.Valid
        } else {
            PreferenceValidation.Invalid("Invalid PIN code format")
        }

    override fun summary(
        context: Context,
        value: String?,
    ): String? =
        if (value.isNotNullOrBlank()) {
            "Pin code set"
        } else {
            "No pin code set"
        }
}

data class StashChoicePreference<T>(
    @param:StringRes override val title: Int,
    override val defaultValue: T,
    @param:ArrayRes val displayValues: Int,
    val indexToValue: (index: Int) -> T,
    val valueToIndex: (T) -> Int,
    override val getter: (prefs: StashPreferences) -> T,
    override val setter: (prefs: StashPreferences, value: T) -> StashPreferences,
) : StashPreference<T>

data class StashClickablePreference(
    @param:StringRes override val title: Int,
    override val defaultValue: Unit = Unit,
    override val getter: (prefs: StashPreferences) -> Unit = { },
    override val setter: (prefs: StashPreferences, value: Unit) -> StashPreferences = { prefs, _ -> prefs },
    @param:StringRes val summary: Int? = null,
) : StashPreference<Unit> {
    override fun summary(
        context: Context,
        value: Unit?,
    ): String? = summary?.let { context.getString(it) }
}

class StashSliderPreference(
    @param:StringRes override val title: Int,
    override val defaultValue: Int,
    val min: Int = 0,
    val max: Int = 100,
    val interval: Int = 1,
    override val getter: (prefs: StashPreferences) -> Int,
    override val setter: (prefs: StashPreferences, value: Int) -> StashPreferences,
    @param:StringRes val summary: Int? = null,
    val summarizer: ((Int?) -> String?)? = null,
) : StashPreference<Int> {
    override fun summary(
        context: Context,
        value: Int?,
    ): String? =
        summarizer?.invoke(value)
            ?: summary?.let { context.getString(it) }
            ?: value?.toString()
}
