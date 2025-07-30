package com.github.damontecres.stashapp.ui.compat

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
import com.github.damontecres.stashapp.StashApplication

// TODO make this a preference?
val isTvDevice by lazy {
    val context = StashApplication.getApplication()
    val pm = context.packageManager
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager

    val hasLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    val hasTelevision = pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
    val uiIsTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

    val isTvDevice = hasLeanback || hasTelevision || uiIsTv
    Log.i("isTvDevice", "isTvDevice=$isTvDevice")
    isTvDevice
}

val isNotTvDevice = !isTvDevice
