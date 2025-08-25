package com.github.damontecres.stashapp.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.components.prefs.StashPreference
import com.github.damontecres.stashapp.ui.components.prefs.advancedPreferences
import com.github.damontecres.stashapp.ui.components.prefs.basicPreferences
import com.github.damontecres.stashapp.ui.components.prefs.uiPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AppUpgradeHandler(
    private val context: Context,
    private val previousVersion: Version,
    private val installedVersion: Version,
) : Runnable {
    override fun run() {
        UpdateChecker.cleanup(context)

        Log.i(TAG, "Migrating $previousVersion to $installedVersion")
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        // Add mpegts as a default force direct play format
        if (previousVersion.isEqualOrBefore(Version.fromString("0.2.9")) &&
            installedVersion.isAtLeast(Version.fromString("0.2.7"))
        ) {
            Log.d(TAG, "Checking for mpegts direct play")
            val defaultFormats =
                context.resources.getStringArray(R.array.default_force_container_formats).toSet()
            val current =
                preferences.getStringSet(
                    context.getString(R.string.pref_key_default_forced_direct_containers),
                    defaultFormats,
                )!!
            if (!current.contains("mpegts")) {
                preferences.edit {
                    val newSet = current.toMutableSet()
                    newSet.add("mpegts")
                    putStringSet(
                        context.getString(R.string.pref_key_default_forced_direct_containers),
                        newSet,
                    )
                }
            }
        }

        if (previousVersion.isEqualOrBefore(Version.fromString("v0.4.1"))) {
            val serverPreferences: SharedPreferences =
                context.getSharedPreferences(
                    context.packageName + "_server_preferences",
                    Context.MODE_PRIVATE,
                )
            serverPreferences.edit(true) {
                clear()
            }
        }

        if (previousVersion.isEqualOrBefore(Version.fromString("v0.5.2"))) {
            Log.i(TAG, "Migrating tabs for v0.5.2")
            preferences.ensureSetHas(
                context,
                R.string.pref_key_ui_tag_tabs,
                R.array.tag_tabs,
                listOf(
                    context.getString(R.string.stashapp_details),
                    context.getString(R.string.stashapp_parent_tags),
                ),
            )

            preferences.ensureSetHas(
                context,
                R.string.pref_key_ui_studio_tabs,
                R.array.studio_tabs,
                listOf(
                    context.getString(R.string.stashapp_details),
                ),
            )
        }
        if (previousVersion == Version.fromString("v0.5.2-8-gc2c5e6f")) {
            val key = context.getString(R.string.pref_key_read_only_mode_pin)
            val readOnlyPin = preferences.getInt(key, -1)
            if (readOnlyPin >= 0) {
                preferences.edit(true) {
                    remove(key)
                    putString(key, readOnlyPin.toString())
                }
            }
        }

        if (previousVersion.isEqualOrBefore(Version.fromString("v0.5.11-3-gf0cf79e2"))) {
            Log.i(TAG, "Migrating tabs for v0.5.11-3-gf0cf79e2")
            preferences.ensureSetHas(
                context,
                R.string.pref_key_ui_performer_tabs,
                R.array.performer_tabs,
                listOf(
                    context.getString(R.string.stashapp_studio),
                ),
            )
        }

        if (previousVersion.isLessThan(Version.fromString("0.6.6"))) {
            Log.i(TAG, "Setting new UI to true")
            val key = context.getString(R.string.pref_key_use_compose_ui)
            if (!preferences.getBoolean(key, true)) {
                // User turned on new UI and turned it off, so show a Toast
                Toast
                    .makeText(
                        context,
                        "The new UI is now the default. You can still switch back to the legacy UI in settings.",
                        Toast.LENGTH_LONG,
                    ).show()
            }
            preferences.edit(true) {
                putBoolean(key, true)
            }
        }
        if (previousVersion.isLessThan(Version.fromString("0.6.10"))) {
            try {
                preferences.getString(context.getString(R.string.pref_key_card_size), "5")
            } catch (_: ClassCastException) {
                val value = preferences.getInt(context.getString(R.string.pref_key_card_size), 5)
                preferences.edit(true) {
                    putString(context.getString(R.string.pref_key_card_size), value.toString())
                }
            }
        }

        if (previousVersion.isLessThan(Version.fromString("0.6.11"))) {
            preferences
                .getStringSet(context.getString(R.string.pref_key_ui_performer_tabs), null)
                ?.let {
                    if (it.contains(context.getString(R.string.stashapp_studio))) {
                        // Rename studio to studios
                        val newSet =
                            it.toMutableSet().apply {
                                remove(context.getString(R.string.stashapp_studio))
                                add(context.getString(R.string.stashapp_studios))
                            }
                        preferences.edit(true) {
                            putStringSet(
                                context.getString(R.string.pref_key_ui_performer_tabs),
                                newSet,
                            )
                        }
                    }
                }

            CoroutineScope(Dispatchers.IO + StashCoroutineExceptionHandler()).launch {
                val preferencesMigratedV1 =
                    context.preferences.data
                        .map { it.preferencesMigratedV1 }
                        .firstOrNull() ?: false
                if (!preferencesMigratedV1) {
                    migratePreferences(context)
                }
                context.preferences.updateData {
                    it.updatePlaybackPreferences {
                        seekBarSteps = 16

                        if (directPlayFormatList.isEmpty()) {
                            // Fix broken migration for formats
                            addAllDirectPlayFormat(
                                preferences.getStringSet(
                                    context.getString(R.string.pref_key_default_forced_direct_containers),
                                    null,
                                ) ?: StashPreference.DirectPlayFormat.defaultValue.toSet(),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun SharedPreferences.ensureSetHas(
        context: Context,
        @StringRes prefKey: Int,
        @ArrayRes defaultValues: Int,
        newValues: Collection<String>,
    ) {
        val key = context.getString(prefKey)
        val currentValues =
            getStringSet(
                key,
                context.resources.getStringArray(defaultValues).toSet(),
            )!!.toMutableSet()
        if (currentValues.addAll(newValues)) {
            edit(true) {
                putStringSet(key, currentValues)
            }
        }
    }

    companion object {
        private const val TAG = "AppUpgradeHandler"

        /**
         * Migrate preferences from SharedPreferences to Proto DataStore.
         * Returns true if all preferences were migrated successfully, false if there were any errors.
         */
        suspend fun migratePreferences(context: Context): Boolean {
            Log.i(TAG, "Starting preferences migration")
            val pm = PreferenceManager.getDefaultSharedPreferences(context)

            fun <T> set(
                preferences: StashPreferences,
                pref: StashPreference<T>,
            ): StashPreferences {
                val value = pref.prefGetter.invoke(context, pm)
                return pref.setter.invoke(preferences, value)
            }

            var errors = false

            context.preferences.updateData { preferences ->
                (basicPreferences + uiPreferences + advancedPreferences)
                    .flatMap { it.preferences }
                    .filter { it.prefKey != 0 }
                    .fold(preferences) { prefs, pref ->
                        try {
                            set(prefs, pref)
                        } catch (ex: Exception) {
                            errors = true
                            Log.e(
                                TAG,
                                "Error migrating preference ${context.getString(pref.prefKey)}",
                                ex,
                            )
                            prefs
                        }
                    }
            }
            Log.i(TAG, "Finished preferences migration: errors=$errors")
            if (errors) {
                Toast
                    .makeText(
                        context,
                        "Some settings could not be migrated. Please check the log for details.",
                        Toast.LENGTH_LONG,
                    ).show()
            }
            return !errors
        }
    }
}
