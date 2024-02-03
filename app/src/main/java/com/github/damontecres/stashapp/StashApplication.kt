package com.github.damontecres.stashapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager

class StashApplication : Application() {
    private var wasEnterBackground = false
    private var mainDestroyed = false

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(ActivityLifecycleCallbacksImpl())
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleObserverImpl())

        val pkgInfo = packageManager.getPackageInfo(packageName, 0)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val currentVersion = prefs.getString(VERSION_NAME_CURRENT_KEY, null)
        val currentVersionCode = prefs.getLong(VERSION_CODE_CURRENT_KEY, -1)
        if (pkgInfo.versionName != currentVersion || pkgInfo.versionCode.toLong() != currentVersionCode) {
            Log.i(
                TAG,
                "App installed: $currentVersion=>${pkgInfo.versionName} ($currentVersionCode=>${pkgInfo.versionCode})",
            )
            prefs.edit(true) {
                putString(VERSION_NAME_PREVIOUS_KEY, currentVersion)
                putLong(VERSION_CODE_PREVIOUS_KEY, currentVersionCode)
                putString(VERSION_NAME_CURRENT_KEY, pkgInfo.versionName)
                putLong(VERSION_CODE_CURRENT_KEY, pkgInfo.versionCode.toLong())
            }
        }
    }

    private fun showPinActivity() {
        val intent = Intent(this, PinActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("wasResumed", true)
        intent.putExtra("mainDestroyed", mainDestroyed)
        mainDestroyed = false
        startActivity(intent)
    }

    inner class LifecycleObserverImpl : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            wasEnterBackground = true
        }
    }

    inner class ActivityLifecycleCallbacksImpl : ActivityLifecycleCallbacks {
        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ) {
        }

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
            if (wasEnterBackground) {
                wasEnterBackground = false
                showPinActivity()
            }
        }

        override fun onActivityPaused(activity: Activity) {
            Log.d(TAG, "onActivityPaused: $activity")
        }

        override fun onActivityStopped(activity: Activity) {
            Log.d(TAG, "onActivityStopped: $activity")
        }

        override fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle,
        ) {
        }

        override fun onActivityDestroyed(activity: Activity) {
            Log.d(TAG, "onActivityDestroyed: $activity")
            if (activity is MainActivity) {
                mainDestroyed = true
            }
        }
    }

    companion object {
        const val TAG = "StashApplication"
        const val VERSION_NAME_PREVIOUS_KEY = "VERSION_NAME_PREVIOUS_NAME"
        const val VERSION_CODE_PREVIOUS_KEY = "VERSION_CODE_PREVIOUS_NAME"
        const val VERSION_NAME_CURRENT_KEY = "VERSION_CURRENT_KEY"
        const val VERSION_CODE_CURRENT_KEY = "VERSION_CODE_CURRENT_KEY"
    }
}
