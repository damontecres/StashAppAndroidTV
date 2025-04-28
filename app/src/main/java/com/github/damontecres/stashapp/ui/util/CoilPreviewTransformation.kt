package com.github.damontecres.stashapp.ui.util

import android.graphics.Bitmap
import androidx.core.graphics.scale
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.transform.Transformation
import com.github.damontecres.stashapp.util.StashPreviewLoader.GlideThumbnailTransformation.Companion.MAX_COLUMNS
import com.github.damontecres.stashapp.util.StashPreviewLoader.GlideThumbnailTransformation.Companion.MAX_LINES

class CoilPreviewTransformation(
    val targetWidth: Int,
    val targetHeight: Int,
    duration: Long,
    position: Long,
) : Transformation() {
    private val x: Int
    private val y: Int

    init {
        val square = position / (duration / (MAX_LINES * MAX_COLUMNS))
        y = square.toInt() / MAX_LINES
        x = square.toInt() % MAX_COLUMNS
    }

    override val cacheKey: String
        get() = "CoilPreviewTransformation_$x,$y"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        val width = input.width / MAX_COLUMNS
        val height = input.height / MAX_LINES
//        Log.d(TAG, "input.width=${input.width}, input.height=${input.height}, width=$width, height=$height, size=$size")
        return Bitmap
            .createBitmap(input, x * width, y * height, width, height)
            .scale(size.width.pxOrElse { targetWidth }, size.height.pxOrElse { targetHeight })
    }

    companion object {
        private const val TAG = "CoilPreviewTransformation"
    }
}
