package com.github.damontecres.stashapp.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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

        // Add mpegts as a default force direct play format
        if (previousVersion.isEqualOrBefore(Version.fromString("0.2.9")) &&
            installedVersion.isAtLeast(Version.fromString("0.2.7"))
        ) {
            Log.d(TAG, "Checking for mpegts direct play")
            val defaultFormats =
                context.resources.getStringArray(R.array.default_force_container_formats).toSet()
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val current =
                prefs.getStringSet(
                    context.getString(R.string.pref_key_default_forced_direct_containers),
                    defaultFormats,
                )!!
            if (!current.contains("mpegts")) {
                prefs.edit {
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
            val preferences: SharedPreferences =
                context.getSharedPreferences(
                    context.packageName + "_server_preferences",
                    Context.MODE_PRIVATE,
                )
            preferences.edit(true) {
                clear()
            }
        }
    }
}
