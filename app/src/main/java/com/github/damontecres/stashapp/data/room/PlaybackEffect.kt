package com.github.damontecres.stashapp.data.room

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.GaussianBlur
import androidx.media3.effect.GlEffect
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.room.Embedded
import androidx.room.Entity

/**
 * Store a [VideoFilter] for a specific scene (by server & scene ID)
 */
@Entity(tableName = "playback_effects", primaryKeys = ["serverUrl", "id"])
data class PlaybackEffect(
    val serverUrl: String,
    val id: String,
    @Embedded val videoFilter: VideoFilter,
)

/**
 * Modifications to a video playback
 */
data class VideoFilter(
    val rotation: Int = 0,
    val brightness: Int = COLOR_DEFAULT,
    val contrast: Int = COLOR_DEFAULT,
    val saturation: Int = COLOR_DEFAULT,
    val hue: Int = HUE_DEFAULT,
    val red: Int = COLOR_DEFAULT,
    val green: Int = COLOR_DEFAULT,
    val blue: Int = COLOR_DEFAULT,
    val blur: Int = 0,
) {
    companion object {
        const val COLOR_DEFAULT = 100
        const val HUE_DEFAULT = 0
    }

    fun isRotated(): Boolean = rotation != 0 && rotation % 360 != 0

    fun hasRgb(): Boolean = red != COLOR_DEFAULT || green != COLOR_DEFAULT || blue != COLOR_DEFAULT

    fun hasBrightness(): Boolean = brightness != COLOR_DEFAULT

    fun hasContrast(): Boolean = contrast != COLOR_DEFAULT

    fun hasHsl(): Boolean = hue != HUE_DEFAULT || saturation != COLOR_DEFAULT

    fun hasBlur(): Boolean = blur > 0

    /**
     * Create the list of effects to apply
     */
    @OptIn(UnstableApi::class)
    fun createEffectList(): List<GlEffect> =
        buildList {
            if (isRotated()) {
                add(
                    ScaleAndRotateTransformation
                        .Builder()
                        .setRotationDegrees(rotation.toFloat())
                        .build(),
                )
            }
            if (hasRgb()) {
                add(
                    RgbAdjustment
                        .Builder()
                        .setRedScale(red / COLOR_DEFAULT.toFloat())
                        .setGreenScale(green / COLOR_DEFAULT.toFloat())
                        .setBlueScale(blue / COLOR_DEFAULT.toFloat())
                        .build(),
                )
            }
            if (hasBrightness()) {
                add(Brightness((brightness - 100) / 100f))
            }
            if (hasContrast()) {
                add(Contrast((contrast - 100) / 100f))
            }
            if (hasHsl()) {
                add(
                    HslAdjustment
                        .Builder()
                        .adjustHue(hue.toFloat())
                        .adjustSaturation((saturation - 100).toFloat())
                        .build(),
                )
            }
            if (hasBlur()) {
                add(GaussianBlur(blur / 10f))
            }
        }
}
