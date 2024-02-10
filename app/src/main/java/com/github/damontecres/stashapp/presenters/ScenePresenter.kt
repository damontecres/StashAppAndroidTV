package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.TextView
import androidx.leanback.widget.ImageCardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.createGlideUrl
import com.github.damontecres.stashapp.util.titleOrFilename
import java.security.MessageDigest

class ScenePresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val scene = item as SlimSceneData
        val cardView = viewHolder.view as ImageCardView

        val contentView = cardView.findViewById<TextView>(androidx.leanback.R.id.content_text)
        contentView.typeface = Typeface.createFromAsset(cardView.context.assets, "fa-solid-900.ttf")

        cardView.titleText = scene.titleOrFilename
        val details = mutableListOf<String>()
        if (!scene.date.isNullOrBlank()) {
            details.add(scene.date)
        }
        if (scene.tags.isNotEmpty()) {
            val tagIcon = cardView.context.getString(R.string.fa_tag)
            details.add("${scene.tags.size}$tagIcon")
        }
        if (scene.performers.isNotEmpty()) {
            details.add("${scene.performers.size}\uD83D\uDC64")
        }
        cardView.contentText = details.joinToString("  ")

        if (!scene.paths.screenshot.isNullOrBlank()) {
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            val url = createGlideUrl(scene.paths.screenshot, vParent.context)
            Glide.with(viewHolder.view.context)
                .load(url)
                .transform(CenterCrop(), TextOverlay(viewHolder.view.context, scene))
                .error(mDefaultCardImage)
                .into(cardView.mainImageView!!)
        }
    }

    /**
     * Adds some text on top of a scene's image
     */
    private class TextOverlay(val context: Context, val scene: SlimSceneData) :
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
                paint.textSize = outHeight / 10F
                paint.isAntiAlias = true
                paint.style = Paint.Style.FILL
                paint.setShadowLayer(5F, 5F, 5F, Color.BLACK)

                paint.textAlign = Paint.Align.RIGHT
                val duration = Constants.durationToString(videoFile.duration)
                canvas.drawText(
                    duration,
                    outWidth.toFloat() - TEXT_PADDING,
                    outHeight - (WATCHED_HEIGHT + TEXT_PADDING),
                    paint,
                )

                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.LEFT
                // TODO: 2160P => 4k, etc
                val resolution = "${videoFile.height}P"
                canvas.drawText(
                    resolution,
                    TEXT_PADDING,
                    outHeight - (WATCHED_HEIGHT + TEXT_PADDING),
                    paint,
                )
                if (scene.resume_time != null) {
                    val percentWatched = scene.resume_time / videoFile.duration
                    val barWidth = percentWatched * outWidth
                    canvas.drawRect(
                        0F,
                        outHeight.toFloat() - WATCHED_HEIGHT,
                        barWidth.toFloat(),
                        outHeight.toFloat(),
                        paint,
                    )
                }
            }
            return result
        }

        companion object {
            private val ID =
                "com.github.damontecres.stashapp.presenters.ScenePresenter.TextOverlay".toByteArray()
        }
    }

    companion object {
        private const val TAG = "ScenePresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 198

        const val TEXT_PADDING = 5F
        const val WATCHED_HEIGHT = 3F
    }
}
