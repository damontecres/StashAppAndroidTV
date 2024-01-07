package com.github.damontecres.stashapp.presenters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.leanback.widget.ImageCardView
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.createGlideUrl
import java.io.File
import java.security.MessageDigest
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ScenePresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val scene = item as SlimSceneData
        val cardView = viewHolder.view as ImageCardView
        if (scene.title.isNullOrBlank()) {
            val path = scene.files.firstOrNull()?.videoFileData?.path
            if (path != null) {
                cardView.titleText = File(path).name
            }
        } else {
            cardView.titleText = scene.title
        }
        cardView.contentText =
            """${scene.date.orEmpty()} (${scene.performers.size}P, ${scene.tags.size}T)"""

        if (!scene.paths.screenshot.isNullOrBlank()) {
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            val apiKey =
                PreferenceManager.getDefaultSharedPreferences(vParent.context)
                    .getString("stashApiKey", "")
            val url = createGlideUrl(scene.paths.screenshot, apiKey)
            Glide.with(viewHolder.view.context)
                .load(url)
                .transform(CenterCrop(), TextOverlay(scene))
                .error(mDefaultCardImage)
                .into(cardView.mainImageView!!)
        }
    }

    /**
     * Adds some text on top of a scene's image
     */
    private class TextOverlay(val scene: SlimSceneData) :
        BitmapTransformation() {
        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update(ID)
            messageDigest.update(scene.id.toByteArray())
        }

        override fun transform(
            pool: BitmapPool,
            toTransform: Bitmap,
            outWidth: Int,
            outHeight: Int,
        ): Bitmap {
            // TODO: just grabbing the first seems like not the best solution
            val result = Bitmap.createBitmap(outWidth, outHeight, toTransform.config)
            val videoFile = scene.files.firstOrNull()?.videoFileData
            if (videoFile != null) {
                // TODO: change pixels to dp?
                val canvas = Canvas(result)
                canvas.drawBitmap(toTransform, 0.0F, 0.0F, null)
                val paint = Paint()
                paint.color = Color.WHITE
                paint.textSize = CARD_HEIGHT / 10F
                paint.isAntiAlias = true
                paint.style = Paint.Style.FILL
                paint.setShadowLayer(5F, 5F, 5F, Color.BLACK)

                paint.textAlign = Paint.Align.RIGHT
                val duration =
                    videoFile.duration.times(100).toInt().div(100).toDuration(DurationUnit.SECONDS)
                        .toString()
                canvas.drawText(
                    duration,
                    outWidth.toFloat() - 5F,
                    outHeight - 5F,
                    paint,
                )

                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.LEFT
                // TODO: 2160P => 4k, etc
                val resolution = "${videoFile.height}P"
                canvas.drawText(
                    resolution,
                    5F,
                    outHeight - 5F,
                    paint,
                )
            }
            return result
        }

        companion object {
            private val ID =
                "com.github.damontecres.stashapp.presenters.ScenePresenter.TextOverlay".toByteArray()
        }
    }

    companion object {
        private val TAG = "ScenePresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 198
    }
}
