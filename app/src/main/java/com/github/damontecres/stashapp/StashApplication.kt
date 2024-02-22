package com.github.damontecres.stashapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.util.configureHttpsTrust
import javax.net.ssl.HttpsURLConnection

class StashApplication : Application() {
    private var wasEnterBackground = false
    private var mainDestroyed = false
    var hasAskedForPin = false

    val defaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory()
    val defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()

    override fun onCreate() {
        super.onCreate()

        Log.v(TAG, "onCreate wasEnterBackground=$wasEnterBackground, mainDestroyed=$mainDestroyed")

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

        configureHttpsTrust(this)
    }

    fun showPinActivity() {
        Log.v(TAG, "showPinActivity, mainDestroyed=$mainDestroyed")
        val intent = Intent(this, PinActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("wasResumed", true)
        intent.putExtra("mainDestroyed", mainDestroyed)
        mainDestroyed = false
        startActivity(intent)
    }

    fun showPinActivityIfNeeded() {
        val pinCode = PreferenceManager.getDefaultSharedPreferences(this).getString("pinCode", "")
        if (!pinCode.isNullOrBlank() && !hasAskedForPin) {
            showPinActivity()
        }
    }

    inner class LifecycleObserverImpl : DefaultLifecycleObserver {
        override fun onPause(owner: LifecycleOwner) {
            Log.v(TAG, "LifecycleObserverImpl.onPause")
            wasEnterBackground = true
            StashExoPlayer.releasePlayer()
        }

        override fun onStop(owner: LifecycleOwner) {
            Log.v(TAG, "LifecycleObserverImpl.onStop")
            wasEnterBackground = true
            StashExoPlayer.releasePlayer()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            Log.v(TAG, "LifecycleObserverImpl.onDestroy")
            wasEnterBackground = true
            StashExoPlayer.releasePlayer()
        }
    }

    inner class ActivityLifecycleCallbacksImpl : ActivityLifecycleCallbacks {
        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ) {
            if (wasEnterBackground) {
                wasEnterBackground = false
                showPinActivity()
            } else if (activity !is PinActivity) {
                showPinActivityIfNeeded()
            }
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }

        override fun onActivityStarted(activity: Activity) {
            if (wasEnterBackground) {
                wasEnterBackground = false
                showPinActivity()
            }
        }

        override fun onActivityResumed(activity: Activity) {
            if (wasEnterBackground) {
                wasEnterBackground = false
                showPinActivity()
            }
        }

        override fun onActivityPaused(activity: Activity) {
            Log.d(TAG, "onActivityPaused: $activity")
            StashExoPlayer.releasePlayer()
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
            Log.v(TAG, "onActivityDestroyed: $activity")
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
