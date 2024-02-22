package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.createGlideUrl
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import java.security.MessageDigest
import java.util.EnumMap

class ScenePresenter(callback: LongClickCallBack<SlimSceneData>? = null) :
    StashPresenter<SlimSceneData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: SlimSceneData,
    ) {
        cardView.titleText = item.titleOrFilename

        val details = mutableListOf<String?>()
        details.add(item.studio?.name)
        details.add(item.date)
        cardView.contentText = concatIfNotBlank(" - ", details)

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.TAG] = item.tags.size
        dataTypeMap[DataType.PERFORMER] = item.performers.size
        dataTypeMap[DataType.MOVIE] = item.movies.size
        dataTypeMap[DataType.MARKER] = item.scene_markers.size

        cardView.setUpExtraRow(dataTypeMap, item.o_counter)

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        if (!item.paths.screenshot.isNullOrBlank()) {
            val url = createGlideUrl(item.paths.screenshot, vParent.context)
            Glide.with(cardView.context)
                .load(url)
                .transform(CenterCrop(), TextOverlay(cardView.context, item))
                .error(glideError(cardView.context))
                .into(cardView.mainImageView!!)
        }
        if (item.paths.preview.isNotNullOrBlank()) {
            cardView.videoUrl = item.paths.preview
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
