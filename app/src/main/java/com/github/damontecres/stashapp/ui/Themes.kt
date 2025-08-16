package com.github.damontecres.stashapp.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.proto.ThemeStyle
import com.github.damontecres.stashapp.ui.theme.inversePrimaryDark
import com.github.damontecres.stashapp.ui.theme.inversePrimaryLight
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

val FontAwesome = FontFamily(Font(resId = R.font.fa_solid_900))

val defaultColorSchemeSet =
    ColorSchemeSet(
        description = "default",
        seed = com.github.damontecres.stashapp.ui.theme.seed,
        border = inversePrimaryDark,
        light =
            AppColorScheme(
                com.github.damontecres.stashapp.ui.theme.lightScheme,
                inversePrimaryLight,
            ),
        dark =
            AppColorScheme(
                com.github.damontecres.stashapp.ui.theme.darkScheme,
                inversePrimaryDark,
            ),
    )

private var currentColorSchemeSet: ColorSchemeSet? = null

fun chooseColorScheme(
    themeStyle: ThemeStyle,
    isSystemInDark: Boolean,
    colorSchemeSet: ColorSchemeSet,
): AppColorScheme =
    when (themeStyle) {
        ThemeStyle.LIGHT -> colorSchemeSet.light
        ThemeStyle.DARK -> colorSchemeSet.dark
        else -> {
            if (isSystemInDark) {
                colorSchemeSet.dark
            } else {
                colorSchemeSet.light
            }
        }
    }

fun getTheme(
    context: Context,
    themeStyle: ThemeStyle,
    themeName: String? = null,
    forceDark: Boolean = false,
    isSystemInDarkTheme: Boolean = false,
): AppColorScheme {
    val useDark = forceDark || isSystemInDarkTheme
    val colorSchemeSet =
        if (themeName.isNotNullOrBlank() && !themeName.equals("default", true)) {
            try {
                readThemeJson(context, themeName)
            } catch (ex: Exception) {
                Log.e("Themes", "Error reading json $themeName", ex)
                Toast
                    .makeText(
                        context,
                        "Error reading theme '$themeName': ${ex.localizedMessage}",
                        Toast.LENGTH_LONG,
                    ).show()
                defaultColorSchemeSet
            }
        } else {
            defaultColorSchemeSet
        }
    currentColorSchemeSet = colorSchemeSet

    return chooseColorScheme(themeStyle, useDark, colorSchemeSet)
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun AppTheme(
    themeStyle: ThemeStyle = ThemeStyle.DARK,
    themeName: String? = null,
    colorScheme: AppColorScheme =
        getTheme(
            LocalContext.current,
            themeStyle,
            themeName,
            isSystemInDarkTheme(),
        ),
    content: @Composable () -> Unit,
) {
    if (LocalDeviceType.current == DeviceType.TV) {
        MaterialTheme(
            colorScheme = colorScheme.tvColorScheme,
            content = content,
        )
    } else {
        MaterialTheme(colorScheme = colorScheme.tvColorScheme) {
            androidx.compose.material3.MaterialTheme(
                colorScheme = colorScheme.colorScheme,
                content = content,
            )
        }
    }
}

@Composable
fun DefaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = defaultColorSchemeSet.dark.tvColorScheme, content = content)
}

@Composable
fun DefaultMaterial3Theme(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = defaultColorSchemeSet.dark.colorScheme,
        content = content,
    )
}

@Composable
fun Material3AppTheme(
    themeStyle: ThemeStyle = ThemeStyle.DARK,
    content: @Composable () -> Unit,
) {
    val colorSchemeSet = currentColorSchemeSet ?: defaultColorSchemeSet
    val colorScheme =
        when (themeStyle) {
            ThemeStyle.LIGHT -> colorSchemeSet.light
            ThemeStyle.DARK -> colorSchemeSet.dark
            else -> {
                if (isSystemInDarkTheme()) {
                    colorSchemeSet.dark
                } else {
                    colorSchemeSet.light
                }
            }
        }

    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme.colorScheme,
        content = content,
    )
}

/**
 * A theme useful for @Preview composables which provides [LocalDeviceType] for the specified device type
 */
@Composable
fun PreviewTheme(
    deviceType: DeviceType = DeviceType.TV,
    useDark: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDeviceType provides deviceType) {
        val colorScheme = if (useDark) defaultColorSchemeSet.dark else defaultColorSchemeSet.light
        if (deviceType == DeviceType.TV) {
            MaterialTheme(colorScheme = colorScheme.tvColorScheme, content = content)
        } else {
            MaterialTheme(colorScheme = colorScheme.tvColorScheme) {
                androidx.compose.material3.MaterialTheme(
                    colorScheme = colorScheme.colorScheme,
                    content = content,
                )
            }
        }
    }
}

sealed class AppColors private constructor() {
    companion object {
        val TransparentBlack25 = Color(0x40000000)
        val TransparentBlack50 = Color(0x80000000)
        val TransparentBlack75 = Color(0xBF000000)
    }
}

fun readThemeJson(
    context: Context,
    name: String,
): ColorSchemeSet {
    val dir = context.getDir("themes", Context.MODE_PRIVATE)
    val jsonString = File(dir, "$name.json").readText()
    return parseThemeJson(jsonString)
}

fun parseThemeJson(jsonString: String): ColorSchemeSet {
    val json = Json.parseToJsonElement(jsonString).jsonObject
    val description = json["description"]?.jsonPrimitive?.content
    val seed = json["seed"]?.color
//    val corePrimary = json["coreColors"]!!.jsonObject["primary"]!!.color

    val borderColor =
        if ("extendedColors" in json) {
            json["extendedColors"]!!
                .jsonArray
                .firstOrNull {
                    it is JsonObject &&
                        it.jsonObject["name"]
                            ?.jsonPrimitive
                            ?.content
                            ?.lowercase() == "border"
                }?.jsonObject
                ?.get("color")
                ?.color
        } else {
            null
        }

    val schemes = json["schemes"]!!.jsonObject
    val lightScheme = parseScheme(schemes["light"]!!.jsonObject)
    val darkScheme = parseScheme(schemes["dark"]!!.jsonObject)
    return ColorSchemeSet(
        description = description,
        seed = seed,
        border = borderColor,
        light = AppColorScheme(lightScheme, borderColor),
        dark = AppColorScheme(darkScheme, borderColor),
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

fun androidx.compose.material3.ColorScheme.tvColorScheme(border: Color?): ColorScheme =
    ColorScheme(
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
        border = border ?: inversePrimary,
        borderVariant = onPrimary, // TODO
    )

data class ColorSchemeSet(
    val description: String?,
    val seed: Color?,
    val border: Color?,
    val light: AppColorScheme,
    val dark: AppColorScheme,
)

data class AppColorScheme(
    val colorScheme: androidx.compose.material3.ColorScheme,
    val border: Color?,
) {
    val tvColorScheme = colorScheme.tvColorScheme(border)
}

@Composable
fun titleCount(
    @StringRes stringId: Int,
    items: List<Any>,
) = stringResource(stringId) + " (${items.size})"
