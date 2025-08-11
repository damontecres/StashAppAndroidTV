package com.github.damontecres.stashapp.ui.components.prefs

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.updateInterfacePreferences
import com.github.damontecres.stashapp.util.updatePinPreferences

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
        val AutoSubmitPin =
            StashSwitchPreference(
                title = R.string.pref_key_pin_code_auto,
                defaultValue = true,
                getter = { it.pinPreferences.autoSubmit },
                setter = { prefs, value ->
                    prefs.updatePinPreferences { autoSubmit = value }
                },
            )
        val PinCode =
            StashPinPreference(
                title = R.string.pref_key_pin_code,
                defaultValue = "",
                getter = { it.pinPreferences.pin },
                setter = { prefs, value ->
                    prefs.updatePinPreferences { pin = value }
                },
            )
        val CardSize =
            StashIntChoicePreference(
                title = R.string.pref_key_card_size,
                defaultValue = 5,
                displayValues = R.array.card_sizes,
                indexToValue = { listOf(7, 6, 5, 4, 3)[it] },
                valueToIndex = { listOf(7, 6, 5, 4, 3).indexOf(it) },
                getter = { it.interfacePreferences.cardSize },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences { cardSize = value }
                },
            )
    }
}

data class StashSwitchPreference(
    @get:StringRes
    override val title: Int,
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
    override val title: Int,
    override val defaultValue: String,
) : StashPreference<String> {
    override fun summary(
        context: Context,
        value: String?,
    ): String? = value
}

class StashPinPreference(
    title: Int,
    defaultValue: String = "",
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

data class StashIntChoicePreference(
    override val title: Int,
    override val defaultValue: Int,
    @param:ArrayRes val displayValues: Int,
    val indexToValue: (index: Int) -> Int,
    val valueToIndex: (Int) -> Int,
    override val getter: (prefs: StashPreferences) -> Int,
    override val setter: (prefs: StashPreferences, value: Int) -> StashPreferences,
) : StashPreference<Int>

data class StashStringChoicePreference(
    override val title: Int,
    override val defaultValue: String,
    @param:ArrayRes val displayValues: Int,
    val indexToValue: (index: Int) -> String,
    val valueToIndex: (String) -> Int,
    override val getter: (prefs: StashPreferences) -> String,
    override val setter: (prefs: StashPreferences, value: String) -> StashPreferences,
) : StashPreference<String>
