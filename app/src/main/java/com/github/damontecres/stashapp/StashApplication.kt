package com.github.damontecres.stashapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.annotation.FontRes
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.setup.SetupActivity
import com.github.damontecres.stashapp.util.AppUpgradeHandler
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.Version
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

@HiltAndroidApp
class StashApplication : Application() {
    private var wasEnterBackground = false
    private var mainDestroyed = false
    var hasAskedForPin = false

    override fun onCreate() {
        super.onCreate()

        application = this

        Log.v(TAG, "onCreate wasEnterBackground=$wasEnterBackground, mainDestroyed=$mainDestroyed")

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            excludeMatchingSharedPreferencesKeys =
                listOf("^stashApiKey$", "^stashUrl$", "^server_.*", "^apikey_.*", "^pinCode$")
            reportContent =
                listOf(
                    ReportField.ANDROID_VERSION,
                    ReportField.APP_VERSION_CODE,
                    ReportField.APP_VERSION_NAME,
                    ReportField.BRAND,
                    // ReportField.BUILD_CONFIG,
                    // ReportField.BUILD,
                    ReportField.CUSTOM_DATA,
                    ReportField.LOGCAT,
                    ReportField.PHONE_MODEL,
                    ReportField.PRODUCT,
                    ReportField.REPORT_ID,
                    ReportField.SHARED_PREFERENCES,
                    ReportField.STACK_TRACE,
                    ReportField.USER_COMMENT,
                    ReportField.USER_CRASH_DATE,
                )
            dialog {
                text =
                    "StashAppAndroidTV has crashed! Would you like to attempt to send a crash report to your Stash server?" +
                    "\n\nThis will only work if you have already installed the companion plugin."
                title = "StashAppAndroidTV Crash Report"
                positiveButtonText = "Send"
                negativeButtonText = "Do not send"
            }
            reportSendFailureToast = "Crash report failed to send"
            reportSendSuccessToast = "Attempted to send crash report!"
        }
        ACRA.errorReporter.putCustomData("SDK_INT", Build.VERSION.SDK_INT.toString())

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
            if (currentVersion != null) {
                CoroutineScope(Dispatchers.IO + StashCoroutineExceptionHandler()).launch {
                    AppUpgradeHandler(
                        this@StashApplication,
                        Version.fromString(currentVersion),
                        Version.fromString(pkgInfo.versionName),
                    ).run()
                }
            }
        }
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
            } else if (activity !is PinActivity && activity !is SetupActivity) {
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
        private lateinit var application: StashApplication

        private val fontCache = mutableMapOf<Int, Typeface>()

        fun getApplication(): StashApplication {
            return application
        }

        fun getCurrentStashServer(): StashServer? {
            return StashServer.getCurrentStashServer(application)
        }

        fun getFont(
            @FontRes fontId: Int,
        ): Typeface {
            return fontCache.getOrPut(fontId) {
                return ResourcesCompat.getFont(getApplication(), fontId)!!
            }
        }

        const val TAG = "StashApplication"
        const val VERSION_NAME_PREVIOUS_KEY = "VERSION_NAME_PREVIOUS_NAME"
        const val VERSION_CODE_PREVIOUS_KEY = "VERSION_CODE_PREVIOUS_NAME"
        const val VERSION_NAME_CURRENT_KEY = "VERSION_CURRENT_KEY"
        const val VERSION_CODE_CURRENT_KEY = "VERSION_CODE_CURRENT_KEY"
    }
}
