package com.github.damontecres.stashapp.ui.compat

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
import androidx.compose.runtime.Composable
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.ui.DeviceType
import com.github.damontecres.stashapp.ui.LocalDeviceType

val isTvDevice: Boolean
    @Composable get() = LocalDeviceType.current == DeviceType.TV

val isNotTvDevice: Boolean
    @Composable get() = !isTvDevice

// TODO make this a preference?
val detectTvDevice by lazy {
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
