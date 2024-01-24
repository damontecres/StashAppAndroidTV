package com.github.damontecres.stashapp.util

import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.github.damontecres.stashapp.data.Scene
import com.github.rubensousa.previewseekbar.PreviewLoader
import java.nio.ByteBuffer
import java.security.MessageDigest

class StashPreviewLoader(private val imageView: ImageView, private val scene: Scene) : PreviewLoader {
    override fun loadPreview(
        currentPosition: Long,
        max: Long,
    ) {
        Glide.with(imageView)
            .load(scene.spriteUrl)
            .override(GlideThumbnailTransformation.IMAGE_WIDTH, GlideThumbnailTransformation.IMAGE_HEIGHT)
            .transform(GlideThumbnailTransformation((scene.duration!! * 1000).toLong(), currentPosition))
            .into(imageView)
    }

    class GlideThumbnailTransformation(duration: Long, position: Long) : BitmapTransformation() {
        private val x: Int
        private val y: Int

        init {
            val square = position / (duration / (MAX_LINES * MAX_COLUMNS))
            y = square.toInt() / MAX_LINES
            x = square.toInt() % MAX_COLUMNS
        }

        override fun transform(
            pool: BitmapPool,
            toTransform: Bitmap,
            outWidth: Int,
            outHeight: Int,
        ): Bitmap {
            val width = toTransform.width / MAX_COLUMNS
            val height = toTransform.height / MAX_LINES
            return Bitmap.createBitmap(toTransform, x * width, y * height, width, height)
        }

        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            val data = ByteBuffer.allocate(8).putInt(x).putInt(y).array()
            messageDigest.update(data)
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as GlideThumbnailTransformation
            return if (x != that.x) false else y == that.y
        }

        override fun hashCode(): Int {
            var result = x
            result = 31 * result + y
            return result
        }

        companion object {
            private const val MAX_LINES = 9
            private const val MAX_COLUMNS = 9
            const val IMAGE_WIDTH = 160
            const val IMAGE_HEIGHT = IMAGE_WIDTH * 9 / 16
            private const val THUMBNAILS_EACH = 5000 // milliseconds
        }
    }
}
