package com.github.damontecres.stashapp.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.github.damontecres.stashapp.data.VideoFilter
import java.nio.charset.Charset
import java.security.MessageDigest

class EffectTransformation(
    val videoFilter: VideoFilter,
) : BitmapTransformation() {
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
        messageDigest.update(videoFilter.toString().toByteArray(Charset.forName("UTF-8")))
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int,
    ): Bitmap {
        // Inspired by https://github.com/wasabeef/glide-transformations/blob/main/transformations/src/main/java/jp/wasabeef/glide/transformations/BitmapTransformation.java
        val width = toTransform.width
        val height = toTransform.height

        val config =
            if (toTransform.config != null) toTransform.config else Bitmap.Config.ARGB_8888
        val bitmap = pool[width, height, config]

        bitmap.density = toTransform.density

        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.setColorFilter(ColorMatrixColorFilter(videoFilter.createColorMatrix()))
        canvas.drawBitmap(toTransform, 0f, 0f, paint)

        return bitmap
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EffectTransformation

        return videoFilter == other.videoFilter
    }

    override fun hashCode(): Int = videoFilter.hashCode()

    companion object {
        private val TAG = "EffectTransformation"
        private val ID = EffectTransformation::class.qualifiedName!!
        private val ID_BYTES: ByteArray = ID.toByteArray(Charset.forName("UTF-8"))
    }
}
