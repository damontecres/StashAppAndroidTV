package com.github.damontecres.stashapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication

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
    val resources = StashApplication.getApplication().resources
    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary = Color(resources.getColor(R.color.default_card_background, null)),
                onPrimary = Color(resources.getColor(android.R.color.white, null)),
                secondary = Color(resources.getColor(R.color.popup_background, null)),
                primaryContainer = Color(resources.getColor(R.color.default_card_background, null)),
                background = Color(resources.getColor(R.color.default_background, null)),
                onBackground = Color(resources.getColor(android.R.color.white, null)),
                surface = Color(resources.getColor(R.color.default_card_background, null)),
                onSurface = Color(resources.getColor(android.R.color.white, null)),
                surfaceVariant = Color(resources.getColor(R.color.default_card_background, null)),
                onSurfaceVariant = Color(resources.getColor(android.R.color.white, null)),
                border = Color(resources.getColor(R.color.selected_background, null)),
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
