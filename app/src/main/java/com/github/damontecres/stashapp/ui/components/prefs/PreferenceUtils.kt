package com.github.damontecres.stashapp.ui.components.prefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.StringRes
import com.github.damontecres.stashapp.proto.TabType
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

fun toPrefString(value: TabType): String =
    value.name
        .split("_")
        .joinToString(" ") {
            it.lowercase().replaceFirstChar { it.uppercaseChar() }
        }

fun fromPrefString(value: String): TabType =
    try {
        TabType.valueOf(
            value.uppercase().replace(" ", "_"),
        )
    } catch (_: IllegalArgumentException) {
        TabType.UNRECOGNIZED
    }

class SharedPreferencesListener(
    val context: Context,
    val scope: CoroutineScope,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferenceMapping by lazy {
        (basicPreferences + uiPreferences + advancedPreferences)
            .flatMap { it.preferences }
            .filter { it.prefKey != 0 }
            .associateBy { context.getString(it.prefKey) }
    }

    private suspend fun <T> set(
        sharedPreferences: SharedPreferences,
        preference: StashPreference<T>,
    ) {
        val newValue = preference.prefGetter.invoke(context, sharedPreferences)
        context.preferences.updateData {
            preference.setter.invoke(it, newValue)
        }
        if (preference == StashPreference.UseNewUI) {
            newValue as Boolean
            if (newValue) {
                Log.v("SharedPreferencesListener", "New UI enabled, unregistering listener")
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            }
        }
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String?,
    ) {
        Log.v("SharedPreferencesListener", "Preference changed: $key")
        if (key != null) {
            scope.launch(StashCoroutineExceptionHandler()) {
                preferenceMapping[key]?.let { preference ->
                    set(sharedPreferences, preference)
                }
            }
        }
    }
}
