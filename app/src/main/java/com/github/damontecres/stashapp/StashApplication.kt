package com.github.damontecres.stashapp

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class StashApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val pkgInfo = packageManager.getPackageInfo(packageName, 0)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val currentVersion = prefs.getString(VERSION_NAME_CURRENT_KEY, null)
        val currentVersionCode = prefs.getLong(VERSION_CODE_CURRENT_KEY, -1)
        if (pkgInfo.versionName != currentVersion || pkgInfo.versionCode.toLong() != currentVersionCode) {
            Log.i(TAG, "App installed: $currentVersion=>${pkgInfo.versionName} ($currentVersionCode=>${pkgInfo.versionCode})")
            prefs.edit(true) {
                putString(VERSION_NAME_PREVIOUS_KEY, currentVersion)
                putLong(VERSION_CODE_PREVIOUS_KEY, currentVersionCode)
                putString(VERSION_NAME_CURRENT_KEY, pkgInfo.versionName)
                putLong(VERSION_CODE_CURRENT_KEY, pkgInfo.versionCode.toLong())
            }
        }
    }

    companion object {
        private const val TAG = "StashApplication"
        const val VERSION_NAME_PREVIOUS_KEY = "VERSION_NAME_PREVIOUS_NAME"
        const val VERSION_CODE_PREVIOUS_KEY = "VERSION_CODE_PREVIOUS_NAME"
        const val VERSION_NAME_CURRENT_KEY = "VERSION_CURRENT_KEY"
        const val VERSION_CODE_CURRENT_KEY = "VERSION_CODE_CURRENT_KEY"
    }
}
