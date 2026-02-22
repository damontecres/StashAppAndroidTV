package com.github.damontecres.stashapp.ui.util

import android.graphics.Bitmap
import androidx.core.graphics.scale
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.transform.Transformation
import com.github.damontecres.stashapp.ui.components.playback.SpriteData

class CoilPreviewTransformation(
    val s: SpriteData,
    val targetWidth: Int,
    val targetHeight: Int,
) : Transformation() {
    override val cacheKey: String
        get() = "CoilPreviewTransformation_$s"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap =
        Bitmap
            .createBitmap(input, s.x, s.y, s.w, s.h)
            .scale(size.width.pxOrElse { targetWidth }, size.height.pxOrElse { targetHeight })

    companion object {
        private const val TAG = "CoilPreviewTransformation"
    }
}
