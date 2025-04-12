package com.github.damontecres.stashapp.ui

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.preference.PreferenceManager
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.R
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val FontAwesome = FontFamily(Font(resId = R.font.fa_solid_900))

val defaultColorSchemeSet =
    ColorSchemeSet(
        description = "default",
        seed = com.github.damontecres.stashapp.ui.theme.seed,
        light = com.github.damontecres.stashapp.ui.theme.lightScheme,
        dark = com.github.damontecres.stashapp.ui.theme.darkScheme,
    )

private var currentColorSchemeSet: ColorSchemeSet? = null

@Suppress("ktlint:standard:function-naming")
@Composable
fun AppTheme(
    forceDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val themeChoice =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(
                stringResource(R.string.pref_key_ui_theme_dark_appearance),
                stringResource(R.string.ui_theme_dark_appearance_choice_default),
            )

    val themeFileName =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(
                stringResource(R.string.pref_key_ui_theme_file),
                null,
            )

    val useDark = forceDark || isSystemInDarkTheme()

    Log.i(
        "AppTheme",
        "AppTheme: themeChoice=$themeChoice, themeFileName=$themeFileName, useDark=$useDark",
    )

    val colorSchemeSet =
        if (themeFileName != null) {
            TODO()
        } else {
            defaultColorSchemeSet
        }
    currentColorSchemeSet = colorSchemeSet

    val colorScheme =
        when (themeChoice) {
            stringResource(id = R.string.ui_theme_dark_appearance_choice_light) -> colorSchemeSet.light
            stringResource(id = R.string.ui_theme_dark_appearance_choice_dark) -> colorSchemeSet.dark
            else -> {
                if (useDark) {
                    colorSchemeSet.dark
                } else {
                    colorSchemeSet.light
                }
            }
        }
    MaterialTheme(colorScheme = colorScheme.tvColorScheme, content = content)
}

@Composable
fun DefaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = defaultColorSchemeSet.dark.tvColorScheme, content = content)
}

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

    val colorSchemeSet = currentColorSchemeSet ?: defaultColorSchemeSet
    val colorScheme =
        when (themeChoice) {
            stringResource(id = R.string.ui_theme_dark_appearance_choice_light) -> colorSchemeSet.light
            stringResource(id = R.string.ui_theme_dark_appearance_choice_dark) -> colorSchemeSet.dark
            else -> {
                if (isSystemInDarkTheme()) {
                    colorSchemeSet.dark
                } else {
                    colorSchemeSet.light
                }
            }
        }

    androidx.compose.material3.MaterialTheme(colorScheme = colorScheme, content = content)
}

sealed class AppColors private constructor() {
    companion object {
        val TransparentBlack25 = Color(0x40000000)
        val TransparentBlack50 = Color(0x80000000)
        val TransparentBlack75 = Color(0xBF000000)
    }
}

fun parseThemeJson(jsonString: String): ColorSchemeSet {
    val json = Json.parseToJsonElement(jsonString).jsonObject
    val description = json["description"]?.jsonPrimitive?.content
    val seed = json["seed"]?.color
//    val corePrimary = json["coreColors"]!!.jsonObject["primary"]!!.color

    val schemes = json["schemes"]!!.jsonObject
    val lightScheme = parseScheme(schemes["light"]!!.jsonObject)
    val darkScheme = parseScheme(schemes["dark"]!!.jsonObject)
    return ColorSchemeSet(
        description = description,
        seed = seed,
        light = lightScheme,
        dark = darkScheme,
    )
}

fun parseScheme(scheme: JsonObject): androidx.compose.material3.ColorScheme =
    androidx.compose.material3.ColorScheme(
        primary = scheme["primary"]!!.color,
        onPrimary = scheme["onPrimary"]!!.color,
        primaryContainer = scheme["primaryContainer"]!!.color,
        onPrimaryContainer = scheme["onPrimaryContainer"]!!.color,
        inversePrimary = scheme["inversePrimary"]!!.color,
        secondary = scheme["secondary"]!!.color,
        onSecondary = scheme["onSecondary"]!!.color,
        secondaryContainer = scheme["secondaryContainer"]!!.color,
        onSecondaryContainer = scheme["onSecondaryContainer"]!!.color,
        tertiary = scheme["tertiary"]!!.color,
        onTertiary = scheme["onTertiary"]!!.color,
        tertiaryContainer = scheme["tertiaryContainer"]!!.color,
        onTertiaryContainer = scheme["onTertiaryContainer"]!!.color,
        background = scheme["background"]!!.color,
        onBackground = scheme["onBackground"]!!.color,
        surface = scheme["surface"]!!.color,
        onSurface = scheme["onSurface"]!!.color,
        surfaceVariant = scheme["surfaceVariant"]!!.color,
        onSurfaceVariant = scheme["onSurfaceVariant"]!!.color,
        surfaceTint = scheme["surfaceTint"]!!.color,
        inverseSurface = scheme["inverseSurface"]!!.color,
        inverseOnSurface = scheme["inverseOnSurface"]!!.color,
        error = scheme["error"]!!.color,
        onError = scheme["onError"]!!.color,
        errorContainer = scheme["errorContainer"]!!.color,
        onErrorContainer = scheme["onErrorContainer"]!!.color,
        outline = scheme["outline"]!!.color,
        outlineVariant = scheme["outlineVariant"]!!.color,
        scrim = scheme["scrim"]!!.color,
        surfaceBright = scheme["surfaceBright"]!!.color,
        surfaceDim = scheme["surfaceDim"]!!.color,
        surfaceContainer = scheme["surfaceContainer"]!!.color,
        surfaceContainerHigh = scheme["surfaceContainerHigh"]!!.color,
        surfaceContainerHighest = scheme["surfaceContainerHighest"]!!.color,
        surfaceContainerLow = scheme["surfaceContainerLow"]!!.color,
        surfaceContainerLowest = scheme["surfaceContainerLowest"]!!.color,
    )

val JsonElement.color: Color
    get() {
        var colorStr = this.jsonPrimitive.content.substring(1)
        if (colorStr.length < 8) {
            colorStr = colorStr.padStart(8, 'F')
        }
        return Color(colorStr.toLong(16))
    }

val androidx.compose.material3.ColorScheme.tvColorScheme: ColorScheme
    get() {
        return ColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            scrim = scrim,
            border = inversePrimary, // TODO
            borderVariant = onPrimary, // TODO
        )
    }

data class ColorSchemeSet(
    val description: String?,
    val seed: Color?,
    val light: androidx.compose.material3.ColorScheme,
    val dark: androidx.compose.material3.ColorScheme,
)
