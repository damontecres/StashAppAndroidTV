package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.leanback.widget.ImageCardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
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

        cardView.titleText = scene.titleOrFilename
        cardView.contentText = scene.date

        val infoView = cardView.findViewById<ViewGroup>(androidx.leanback.R.id.info_field)
        val sceneExtra =
            LayoutInflater.from(infoView.context)
                .inflate(R.layout.scene_card_extra, infoView, true) as ViewGroup

        setUpIcon(sceneExtra, DataType.TAG, scene.tags.size)
        setUpIcon(sceneExtra, DataType.PERFORMER, scene.performers.size)
        setUpIcon(sceneExtra, DataType.MOVIE, scene.movies.size)
        setUpIcon(sceneExtra, DataType.MARKER, scene.scene_markers.size)
        setUpIcon(sceneExtra, DataType.SCENE, scene.o_counter ?: -1)
        Log.v(TAG, "${scene.titleOrFilename} view count is ${sceneExtra.size}")

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

    private fun setUpIcon(
        rootView: View,
        textResId: Int,
        iconResId: Int,
        text: String,
    ) {
        val textView = rootView.findViewById<TextView>(textResId)
        textView.text = text
        textView.isVisible = true
        rootView.findViewById<TextView>(iconResId).isVisible = true
    }

    private fun setUpIcon(
        rootView: ViewGroup,
        dataType: DataType,
        count: Int,
    ) {
        var textResId: Int
        var iconResId: Int
        when (dataType) {
            DataType.MOVIE -> {
                textResId = R.id.scene_movie_count
                iconResId = R.id.scene_movie_icon
            }

            DataType.MARKER -> {
                textResId = R.id.scene_marker_count
                iconResId = R.id.scene_marker_icon
            }

            DataType.PERFORMER -> {
                textResId = R.id.scene_performer_count
                iconResId = R.id.scene_performer_icon
            }

            DataType.TAG -> {
                textResId = R.id.scene_tag_count
                iconResId = R.id.scene_tag_icon
            }

            // Workaround for O Counter
            DataType.SCENE -> {
                textResId = R.id.scene_ocounter_count
                iconResId = R.id.scene_ocounter_icon
            }

            else -> throw IllegalArgumentException()
        }
        val textView = rootView.findViewById<TextView>(textResId)
        val iconView = rootView.findViewById<TextView>(iconResId)
        if (count > 0) {
            textView.text = count.toString()
            textView.visibility = View.VISIBLE
            iconView.visibility = View.VISIBLE
        } else {
            Log.v(TAG, "Removing $dataType view")
            textView.visibility = View.GONE
            iconView.visibility = View.GONE
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
