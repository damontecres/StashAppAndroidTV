package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewSwitcher
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.enableMarquee
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import java.util.EnumMap

class StashImageCardView(context: Context) : ImageCardView(context) {
    private val sSelectedBackgroundColor: Int =
        ContextCompat.getColor(context, R.color.selected_background)
    private val sDefaultBackgroundColor: Int =
        ContextCompat.getColor(context, R.color.default_card_background)

    var videoUrl: String? = null
    var videoPosition = -1L
    val videoView: PlayerView = findViewById(R.id.main_video)
    val mainView: ViewSwitcher = findViewById(R.id.main_view)

    private val dataTypeViews =
        EnumMap<DataType, Pair<TextView, View>>(DataType::class.java)
    private val oCounterTextView: TextView
    private val oCounterIconView: View
    private val textOverlays = EnumMap<OverlayPosition, TextView>(OverlayPosition::class.java)
    private val progressOverlay = findViewById<ImageView>(R.id.card_overlay_progress)

    private var imageDimensionsSet = false

    private val playVideoPreviews =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("playVideoPreviews", true)

    private val listener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    showVideo()
                }
            }
        }

    init {
        mainImageView.visibility = View.VISIBLE
        val infoArea = findViewById<ViewGroup>(R.id.info_field)
        val iconRow = LayoutInflater.from(context).inflate(R.layout.image_card_icon_row, infoArea)

        val titleTextView = findViewById<TextView>(androidx.leanback.R.id.title_text)
        titleTextView.enableMarquee(false)
        val contentTextView = findViewById<TextView>(androidx.leanback.R.id.content_text)
        contentTextView.enableMarquee(false)

        dataTypeViews[DataType.MOVIE] =
            Pair(
                iconRow.findViewById(R.id.extra_movie_count),
                iconRow.findViewById(R.id.extra_movie_icon),
            )
        dataTypeViews[DataType.MARKER] =
            Pair(
                iconRow.findViewById(R.id.extra_marker_count),
                iconRow.findViewById(R.id.extra_marker_icon),
            )
        dataTypeViews[DataType.PERFORMER] =
            Pair(
                iconRow.findViewById(R.id.extra_performer_count),
                iconRow.findViewById(R.id.extra_performer_icon),
            )
        dataTypeViews[DataType.TAG] =
            Pair(
                iconRow.findViewById(R.id.extra_tag_count),
                iconRow.findViewById(R.id.extra_tag_icon),
            )
        dataTypeViews[DataType.SCENE] =
            Pair(
                iconRow.findViewById(R.id.extra_scene_count),
                iconRow.findViewById(R.id.extra_scene_icon),
            )
        dataTypeViews[DataType.MOVIE] =
            Pair(
                iconRow.findViewById(R.id.extra_movie_count),
                iconRow.findViewById(R.id.extra_movie_icon),
            )
        dataTypeViews[DataType.GALLERY] =
            Pair(
                iconRow.findViewById(R.id.extra_gallery_count),
                iconRow.findViewById(R.id.extra_gallery_icon),
            )
        dataTypeViews[DataType.IMAGE] =
            Pair(
                iconRow.findViewById(R.id.extra_image_count),
                iconRow.findViewById(R.id.extra_image_icon),
            )
        oCounterTextView = iconRow.findViewById(R.id.extra_ocounter_count)
        oCounterIconView = iconRow.findViewById(R.id.extra_ocounter_icon)

        textOverlays[OverlayPosition.TOP_LEFT] = findViewById(R.id.card_overlay_top_left)
        textOverlays[OverlayPosition.TOP_RIGHT] = findViewById(R.id.card_overlay_top_right)
        textOverlays[OverlayPosition.BOTTOM_LEFT] = findViewById(R.id.card_overlay_bottom_left)
        textOverlays[OverlayPosition.BOTTOM_RIGHT] = findViewById(R.id.card_overlay_bottom_right)
    }

    override fun setSelected(selected: Boolean) {
        if (playVideoPreviews && videoUrl.isNotNullOrBlank()) {
            if (selected) {
                initPlayer()
                videoView.player?.seekToDefaultPosition()
                videoView.player?.playWhenReady = true
            } else {
                showImage()
                StashExoPlayer.removeListeners()
                videoView.player?.stop()
                videoView.player = null
            }
        }
        updateCardBackgroundColor(this, selected)
        super.setSelected(selected)
    }

    override fun setMainImageDimensions(
        width: Int,
        height: Int,
    ) {
        val cardSize =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getInt("cardSize", context.getString(R.string.card_size_default))
        val scaledWidth = (width * 5.0 / cardSize).toInt()
        val scaledHeight = (height * 5.0 / cardSize).toInt()
        val lp = mainView.layoutParams
        lp.width = scaledWidth
        lp.height = scaledHeight
        mainView.layoutParams = lp

        imageDimensionsSet = true
    }

    fun updateCardBackgroundColor(
        view: ImageCardView,
        selected: Boolean,
    ) {
        val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
        // Both background colors should be set because the view"s background is temporarily visible
        // during animations.
        view.setBackgroundColor(color)
        view.setInfoAreaBackgroundColor(color)
    }

    @OptIn(UnstableApi::class)
    private fun initPlayer() {
        val mediaItem =
            MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .setMimeType(MimeTypes.VIDEO_MP4)
                .build()
        val player = StashExoPlayer.getInstance(context)
        StashExoPlayer.addListener(listener)

        videoView.player = player
        player.setMediaItem(mediaItem)
        if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("videoPreviewAudio", true)
        ) {
            player.volume = 1f
        } else {
            player.volume = 0f
        }
        player.prepare()
        player.repeatMode = Player.REPEAT_MODE_ONE
        if (videoPosition > 0) {
            player.seekTo(videoPosition)
        }
        player.playWhenReady = true
    }

    fun setUpExtraRow(
        iconMap: EnumMap<DataType, Int>,
        oCounter: Int?,
    ) {
        DataType.entries.forEach {
            val count = iconMap[it] ?: -1
            setUpIcon(it, count)
        }
        if ((oCounter ?: -1) > 0) {
            oCounterTextView.text = oCounter.toString()
            oCounterTextView.visibility = View.VISIBLE
            oCounterIconView.visibility = View.VISIBLE
            (oCounterTextView.parent as ViewGroup).visibility = View.VISIBLE
        } else {
            oCounterTextView.visibility = View.GONE
            oCounterIconView.visibility = View.GONE
            (oCounterTextView.parent as ViewGroup).visibility = View.GONE
        }
    }

    private fun setUpIcon(
        dataType: DataType,
        count: Int,
    ) {
        val views = dataTypeViews[dataType]
        if (views != null) {
            val textView = views.first
            val iconView = views.second
            if (count > 0) {
                textView.text = count.toString()
                textView.visibility = View.VISIBLE
                iconView.visibility = View.VISIBLE
                (textView.parent as ViewGroup).visibility = View.VISIBLE
            } else {
                textView.visibility = View.GONE
                iconView.visibility = View.GONE
                (textView.parent as ViewGroup).visibility = View.GONE
            }
        }
    }

    fun showVideo() {
        // Image's don't have a background so that transparent images show the card background
        // Since the entire card's background changes when selected, this means the selected color will flash for a moment when switching to the video
        // Changing the background to black before switching will remove that flash since the video background is also black
        mainView.setBackgroundColor(resources.getColor(android.R.color.black, null))
        mainView.displayedChild = 1
    }

    fun showImage() {
        // Reset the background back for images so transparent ones show through
        mainView.setBackgroundColor(resources.getColor(R.color.default_card_background, null))
        mainView.displayedChild = 0
    }

    fun getTextOverlay(position: OverlayPosition): TextView {
        return textOverlays[position]!!
    }

    fun setTextOverlayText(
        position: OverlayPosition,
        text: CharSequence,
    ) {
        getTextOverlay(position).text = text
    }

    fun setProgress(progress: Double) {
        if (!imageDimensionsSet) {
            throw IllegalStateException("Called setProgress before setMainImageDimensions")
        }
        val width = mainView.layoutParams.width
        val lp = progressOverlay.layoutParams
        lp.width = (width * progress).toInt()
        progressOverlay.layoutParams = lp
    }

    fun setRating100(rating100: Int?) {
        if (rating100 != null && rating100 > 0) {
            val ratingText = Constants.getRatingAsDecimalString(context, rating100)
            val text = context.getString(R.string.stashapp_rating) + ": $ratingText"
            val overlay = getTextOverlay(OverlayPosition.TOP_LEFT)

            val ratingColors = context.resources.obtainTypedArray(R.array.rating_colors)
            overlay.setTypeface(null, Typeface.BOLD)
            overlay.setBackgroundColor(ratingColors.getColor(rating100 / 5, Color.WHITE))
            overlay.text = text
            ratingColors.recycle()
        }
    }

    enum class OverlayPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
    }
}
