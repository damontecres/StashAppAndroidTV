package com.github.damontecres.stashapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.github.damontecres.stashapp.R

val FontAwesome = FontFamily(Font(resId = R.font.fa_solid_900))

@Suppress("ktlint:standard:function-naming")
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themeChoice =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(
                stringResource(R.string.pref_key_ui_theme_dark_appearance),
                stringResource(R.string.ui_theme_dark_appearance_choice_default),
            )

    val colorScheme =
        when (themeChoice) {
            stringResource(id = R.string.ui_theme_dark_appearance_choice_light) -> lightColorScheme()
            stringResource(id = R.string.ui_theme_dark_appearance_choice_dark) -> darkColorScheme()
            else -> {
                if (isSystemInDarkTheme()) {
                    darkColorScheme()
                } else {
                    lightColorScheme()
                }
            }
        }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun MainTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary = Color(0xFF30404D),
                onPrimary = Color.White,
                onPrimaryContainer = Color.White,
                secondary = Color(0xFF111a20),
                secondaryContainer = Color(0xFF111a20),
                onSecondary = Color.White,
                onSecondaryContainer = Color.White,
                onTertiary = Color.White,
                primaryContainer = Color(0xFF30404d),
                background = Color(0xFF202b33),
                onBackground = Color.White,
                surface = Color(0xFF202b33),
                onSurface = Color.White,
                surfaceVariant = Color(0xFF30404d),
                onSurfaceVariant = Color.White,
                border = Color(0xFF4785b5),
            ),
        content = content,
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun Material3AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themeChoice =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(
                stringResource(R.string.pref_key_ui_theme_dark_appearance),
                stringResource(R.string.ui_theme_dark_appearance_choice_default),
            )

    val colorScheme =
        when (themeChoice) {
            stringResource(id = R.string.ui_theme_dark_appearance_choice_light) -> androidx.compose.material3.lightColorScheme()
            stringResource(id = R.string.ui_theme_dark_appearance_choice_dark) -> androidx.compose.material3.darkColorScheme()
            else -> {
                if (isSystemInDarkTheme()) {
                    androidx.compose.material3.darkColorScheme()
                } else {
                    androidx.compose.material3.lightColorScheme()
                }
            }
        }

    androidx.compose.material3.MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun Material3MainTheme(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(
        colorScheme =
            androidx.compose.material3.darkColorScheme(
                primary = Color(0xFF30404D),
                onPrimary = Color.White,
                onPrimaryContainer = Color.White,
                secondary = Color(0xFF111a20),
                secondaryContainer = Color(0xFF111a20),
                onSecondary = Color.White,
                onSecondaryContainer = Color.White,
                onTertiary = Color.White,
                primaryContainer = Color(0xFF30404d),
                background = Color(0xFF202b33),
                onBackground = Color.White,
                surface = Color(0xFF202b33),
                onSurface = Color.White,
                surfaceVariant = Color(0xFF30404d),
                onSurfaceVariant = Color.White,
                surfaceContainerHighest = Color(0xFF111a20),
            ),
        content = content,
    )
}

sealed class AppColors private constructor() {
    companion object {
        val TransparentBlack25 = Color(0x40000000)
        val TransparentBlack50 = Color(0x80000000)
        val TransparentBlack75 = Color(0xBF000000)
    }
}
