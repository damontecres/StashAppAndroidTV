package com.github.damontecres.stashapp

import android.app.Activity
import android.app.Application
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.FontRes
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.github.damontecres.stashapp.data.room.AppDatabase
import com.github.damontecres.stashapp.util.AppUpgradeHandler
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.Version
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

class StashApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        application = this

        val pkgInfo = packageManager.getPackageInfo(packageName, 0)
        val versionNameStr = pkgInfo.versionName ?: "Unknown version"

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            excludeMatchingSharedPreferencesKeys =
                listOf(
                    "^stashApiKey$",
                    "^stashUrl$",
                    "^server_.*",
                    "^apikey_.*",
                    "^pinCode$",
                    "^readOnlyMode\\.pinCode$",
                )
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
                    "StashAppAndroidTV ($versionNameStr) has crashed! Would you like to attempt to " +
                    "send a crash report to your Stash server?" +
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

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val currentVersion = prefs.getString(VERSION_NAME_CURRENT_KEY, null)
        val currentVersionCode = prefs.getLong(VERSION_CODE_CURRENT_KEY, -1)

        val newVersionCode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                pkgInfo.versionCode.toLong()
            }
        if (pkgInfo.versionName != currentVersion || newVersionCode != currentVersionCode) {
            Log.i(
                TAG,
                "App installed: $currentVersion=>${pkgInfo.versionName} ($currentVersionCode=>$newVersionCode",
            )
            prefs.edit(true) {
                putString(VERSION_NAME_PREVIOUS_KEY, currentVersion)
                putLong(VERSION_CODE_PREVIOUS_KEY, currentVersionCode)
                putString(VERSION_NAME_CURRENT_KEY, pkgInfo.versionName)
                putLong(VERSION_CODE_CURRENT_KEY, newVersionCode)
            }
            if (currentVersion != null) {
                CoroutineScope(Dispatchers.IO + StashCoroutineExceptionHandler()).launch {
                    AppUpgradeHandler(
                        this@StashApplication,
                        Version.fromString(currentVersion),
                        Version.fromString(pkgInfo.versionName!!),
                    ).run()
                }
            }
        }

        setupDB()
    }

    private fun setupDB() {
        val dbName = getString(R.string.app_name)
        database =
            Room
                .databaseBuilder(this, AppDatabase::class.java, dbName)
                .fallbackToDestructiveMigration()
                .build()
    }

    inner class LifecycleObserverImpl : DefaultLifecycleObserver {
        override fun onPause(owner: LifecycleOwner) {
            Log.v(TAG, "LifecycleObserverImpl.onPause")
            StashExoPlayer.releasePlayer()
        }

        override fun onStop(owner: LifecycleOwner) {
            Log.v(TAG, "LifecycleObserverImpl.onStop")
            StashExoPlayer.releasePlayer()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            Log.v(TAG, "LifecycleObserverImpl.onDestroy")
            StashExoPlayer.releasePlayer()
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
            Log.d(TAG, "onActivityDestroyed: $activity")
        }
    }

    companion object {
        private lateinit var application: StashApplication
        private lateinit var database: AppDatabase
        var currentServer: StashServer? = null

        private val fontCache = mutableMapOf<Int, Typeface>()

        fun getApplication(): StashApplication = application

        fun requireCurrentServer(): StashServer = currentServer ?: throw QueryEngine.StashNotConfiguredException()

        fun getFont(
            @FontRes fontId: Int,
        ): Typeface {
            return fontCache.getOrPut(fontId) {
                return ResourcesCompat.getFont(getApplication(), fontId)!!
            }
        }

        fun getDatabase(): AppDatabase = database

        const val TAG = "StashApplication"
        const val VERSION_NAME_PREVIOUS_KEY = "VERSION_NAME_PREVIOUS_NAME"
        const val VERSION_CODE_PREVIOUS_KEY = "VERSION_CODE_PREVIOUS_NAME"
        const val VERSION_NAME_CURRENT_KEY = "VERSION_CURRENT_KEY"
        const val VERSION_CODE_CURRENT_KEY = "VERSION_CODE_CURRENT_KEY"
    }
}
