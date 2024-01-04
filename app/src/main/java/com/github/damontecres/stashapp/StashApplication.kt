package com.github.damontecres.stashapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class StashApplication : Application() {

    private var wasEnterBackground = false
    private var mainDestroyed = false

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(ActivityLifecycleCallbacksImpl())
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleObserverImpl())
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
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

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

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

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
    }
}