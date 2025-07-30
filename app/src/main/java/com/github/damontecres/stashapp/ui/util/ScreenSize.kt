package com.github.damontecres.stashapp.ui.util

import android.content.res.Configuration
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun screenSize(): ScreenSize {
    val orientation = LocalConfiguration.current.orientation
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        when (windowSizeClass.heightSizeClass) {
            WindowHeightSizeClass.Compact -> ScreenSize.COMPACT
            WindowHeightSizeClass.Medium -> ScreenSize.MEDIUM
            WindowHeightSizeClass.Expanded -> ScreenSize.EXPANDED
            else -> ScreenSize.MEDIUM
        }
    } else {
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> ScreenSize.COMPACT
            WindowWidthSizeClass.Medium -> ScreenSize.MEDIUM
            WindowWidthSizeClass.Expanded -> ScreenSize.EXPANDED
            else -> ScreenSize.MEDIUM
        }
    }
}

enum class ScreenSize {
    COMPACT,
    MEDIUM,
    EXPANDED,
}
