package com.github.damontecres.stashapp.ui.components.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R

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

    @get:StringRes
    val key: Int

    val defaultValue: T

    val getter: (prefs: SharedPreferences, key: String) -> T

    val setter: (editor: SharedPreferences.Editor, key: String, value: T?) -> Unit

    fun summary(
        context: Context,
        value: T?,
    ): String? = null

    fun validate(value: T): PreferenceValidation = PreferenceValidation.Valid

    companion object {
        fun <T> get(
            context: Context,
            preference: StashPreference<T>,
        ): T {
            PreferenceManager.getDefaultSharedPreferences(context).let { prefs ->
                val key = context.getString(preference.key)
                return preference.getter(prefs, key)
            }
        }

        val AutoSubmitPin =
            StashSwitchPreference(
                title = R.string.pref_key_pin_code_auto,
                key = R.string.pref_key_pin_code_auto,
                defaultValue = true,
            )
        val ReadOnlyMode =
            StashSwitchPreference(
                title = R.string.pref_key_read_only_mode,
                key = R.string.pref_key_read_only_mode,
                defaultValue = false,
            )
        val PinCode =
            StashPinPreference(
                title = R.string.pref_key_pin_code,
                key = R.string.pref_key_pin_code,
                defaultValue = "",
            )
        val CardSize =
            StashStringChoicePreference(
                title = R.string.pref_key_card_size,
                key = R.string.pref_key_card_size,
                defaultValue = "5",
                displayValues = R.array.card_sizes,
                storeValues = listOf("7", "6", "5", "4", "3"),
            )
    }
}

data class StashSwitchPreference(
    override val title: Int,
    override val key: Int,
    override val defaultValue: Boolean,
    @param:StringRes val summary: Int? = null,
    @param:StringRes val summaryOn: Int? = null,
    @param:StringRes val summaryOff: Int? = null,
) : StashPreference<Boolean> {
    override val getter = { prefs: SharedPreferences, key: String ->
        prefs.getBoolean(key, defaultValue)
    }
    override val setter = { editor: SharedPreferences.Editor, key: String, value: Boolean? ->
        if (value == null) {
            editor.remove(key)
        } else {
            editor.putBoolean(key, value)
        }
        Unit
    }

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

open class StashStringPreference(
    override val title: Int,
    override val key: Int,
    override val defaultValue: String,
) : StashPreference<String> {
    override val getter = { prefs: SharedPreferences, key: String ->
        prefs.getString(key, defaultValue)!!
    }
    override val setter = { editor: SharedPreferences.Editor, key: String, value: String? ->
        editor.putString(key, value)
        Unit
    }
}

class StashPinPreference(
    title: Int,
    key: Int,
    defaultValue: String = "",
) : StashStringPreference(title, key, defaultValue) {
    override fun validate(value: String): PreferenceValidation =
        if (value.isBlank() || value.toIntOrNull() != null) {
            PreferenceValidation.Valid
        } else {
            PreferenceValidation.Invalid("Invalid PIN code format")
        }
}

data class StashIntChoicePreference(
    override val title: Int,
    override val key: Int,
    override val defaultValue: Int,
    @param:ArrayRes val displayValues: Int,
    val storeValues: List<Int>,
) : StashPreference<Int> {
    override val getter = { prefs: SharedPreferences, key: String ->
        prefs.getInt(key, defaultValue)
    }
    override val setter = { editor: SharedPreferences.Editor, key: String, value: Int? ->
        if (value == null) {
            editor.remove(key)
        } else {
            editor.putInt(key, value)
        }
        Unit
    }
}

data class StashStringChoicePreference(
    override val title: Int,
    override val key: Int,
    override val defaultValue: String,
    @param:ArrayRes val displayValues: Int,
    val storeValues: List<String>,
) : StashPreference<String> {
    override val getter = { prefs: SharedPreferences, key: String ->
        prefs.getString(key, defaultValue)!!
    }
    override val setter = { editor: SharedPreferences.Editor, key: String, value: String? ->
        if (value == null) {
            editor.remove(key)
        } else {
            editor.putString(key, value)
        }
        Unit
    }
}
