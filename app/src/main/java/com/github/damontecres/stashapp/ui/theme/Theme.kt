package com.github.damontecres.stashapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@Suppress("ktlint:standard:function-naming")
@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (useDarkTheme) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun Material3AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (useDarkTheme) {
            androidx.compose.material3.darkColorScheme()
        } else {
            androidx.compose.material3.lightColorScheme()
        }
    androidx.compose.material3.MaterialTheme(colorScheme = colorScheme, content = content)
}
