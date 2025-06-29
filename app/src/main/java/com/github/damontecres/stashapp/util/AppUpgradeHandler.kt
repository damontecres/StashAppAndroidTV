package com.github.damontecres.stashapp.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R

class AppUpgradeHandler(
    private val context: Context,
    private val previousVersion: Version,
    private val installedVersion: Version,
) : Runnable {
    companion object {
        private const val TAG = "AppUpgradeHandler"
    }

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
            preferences.edit(true) {
                putBoolean(context.getString(R.string.pref_key_use_compose_ui), true)
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
}
