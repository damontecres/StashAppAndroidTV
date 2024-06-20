package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewSwitcher
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.animateToInvisible
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.util.enableMarquee
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.FontSpan
import com.github.damontecres.stashapp.views.getRatingAsDecimalString
import java.util.EnumMap

class StashImageCardView(context: Context) : ImageCardView(context) {
    companion object {
        private const val TAG = "StashImageCardView"

        private val FA_FONT = StashApplication.getFont(R.font.fa_solid_900)

        private val ICON_ORDER =
            listOf(
                DataType.SCENE,
                DataType.MOVIE,
                DataType.IMAGE,
                DataType.GALLERY,
                DataType.TAG,
                DataType.PERFORMER,
                DataType.MARKER,
                DataType.STUDIO,
            )
    }

    private val sSelectedBackgroundColor: Int =
        ContextCompat.getColor(context, R.color.selected_background)
    private val sDefaultBackgroundColor: Int =
        ContextCompat.getColor(context, R.color.default_card_background)
    private val transparentColor = ContextCompat.getColor(context, android.R.color.transparent)
    private val animateTime = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

    var videoUrl: String? = null
    var videoPosition = -1L

    private var videoView: PlayerView? = null
    val mainView: ViewSwitcher = findViewById(R.id.main_view)
    var hideOverlayOnSelection = true

    private val iconTextView: TextView
    private val oCounterTextView: TextView
    private val oCounterIconView: View

    private val cardOverlay = findViewById<View>(R.id.card_overlay)
    private val textOverlays = EnumMap<OverlayPosition, TextView>(OverlayPosition::class.java)
    private val progressOverlay = findViewById<ImageView>(R.id.card_overlay_progress)
    private val topRightImageOverlay = findViewById<ImageView>(R.id.card_overlay_top_right_image)

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

        iconTextView = iconRow.findViewById(R.id.icon_text)
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
                videoView?.player?.seekToDefaultPosition()
                videoView?.player?.playWhenReady = true
            } else {
                showImage()
                StashExoPlayer.removeListeners()
                videoView?.player?.stop()
                videoView?.player = null
            }
        }
        if (selected && hideOverlayOnSelection) {
            cardOverlay.clearAnimation()
            cardOverlay.animateToInvisible(durationMs = animateTime)
        } else if (hideOverlayOnSelection) {
            cardOverlay.clearAnimation()
            cardOverlay.animateToVisible(animateTime)
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

    private fun initPlayer() {
        val mediaItem =
            MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .setMimeType(MimeTypes.VIDEO_MP4)
                .build()
        val player = StashExoPlayer.getInstance(context)
        StashExoPlayer.addListener(listener)

        if (videoView == null) {
            // Create the PlayerView on demand
            videoView =
                LayoutInflater.from(context)
                    .inflate(R.layout.stash_card_player_view, mainView, false) as PlayerView
            mainView.addView(videoView)
        }

        videoView!!.player = player
        player.setMediaItem(mediaItem, if (videoPosition > 0) videoPosition else C.TIME_UNSET)
        if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("videoPreviewAudio", false)
        ) {
            player.volume = 1f
        } else {
            player.volume = 0f
        }
        player.prepare()
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.playWhenReady = true
    }

    fun setUpExtraRow(
        iconMap: EnumMap<DataType, Int>,
        oCounter: Int?,
    ) {
        val countStrings =
            ICON_ORDER.mapNotNull {
                val count = iconMap[it]
                if (count != null && count > 0) {
                    context.getString(it.iconStringId) + " " + count.toString()
                } else {
                    null
                }
            }

        iconTextView.text =
            SpannableStringBuilder().apply {
                countStrings.forEachIndexed { index, s ->
                    val start = length
                    append(s)
                    if (index + 1 < countStrings.size) {
                        append("   ")
                    }
                    setSpan(
                        FontSpan(FA_FONT),
                        start,
                        start + 1,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE,
                    )
                }
            }

        if ((oCounter ?: -1) > 0) {
            oCounterTextView.text = oCounter.toString()
            oCounterTextView.visibility = View.VISIBLE
            oCounterIconView.visibility = View.VISIBLE
        } else {
            oCounterTextView.visibility = View.GONE
            oCounterIconView.visibility = View.GONE
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
        val showRatings =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_key_show_rating), true)
        if (rating100 != null && rating100 > 0 && showRatings) {
            val ratingText = getRatingAsDecimalString(context, rating100)
            val text = context.getString(R.string.stashapp_rating) + ": $ratingText"
            val overlay = getTextOverlay(OverlayPosition.TOP_LEFT)

            val ratingColors = context.resources.obtainTypedArray(R.array.rating_colors)
            overlay.setTypeface(null, Typeface.BOLD)
            overlay.setBackgroundColor(ratingColors.getColor(rating100 / 5, Color.WHITE))
            overlay.text = text
            ratingColors.recycle()
        }
    }

    fun setIsFavorite() {
        val typeface = StashApplication.getFont(R.font.fa_solid_900)
        val textView = getTextOverlay(OverlayPosition.TOP_RIGHT)
        textView.typeface = typeface
        textView.text = context.getString(R.string.fa_heart)
        textView.setTextColor(context.getColor(android.R.color.holo_red_light))
        textView.textSize = 18.0f
    }

    fun onUnbindViewHolder() {
        // Remove references to images so that the garbage collector can free up memory
        badgeImage = null
        mainImage = null
        videoUrl = null
        videoView?.player?.release()
        videoView?.player = null

        textOverlays.values.forEach {
            it.text = null
            it.setBackgroundColor(transparentColor)
        }
        val lp = progressOverlay.layoutParams
        lp.width = 0
        progressOverlay.layoutParams = lp

        topRightImageOverlay.visibility = View.INVISIBLE
    }

    fun setTopRightImage(
        imageUrl: String?,
        fallbackText: CharSequence?,
    ) {
        if (imageUrl != null && !imageUrl.contains("default=true")) {
            val lp = topRightImageOverlay.layoutParams
            lp.width = 128
            lp.height = 50
            topRightImageOverlay.layoutParams = lp

            StashGlide.with(context, imageUrl)
                .listener(
                    object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean,
                        ): Boolean {
                            if (fallbackText != null) {
                                setTextOverlayText(
                                    OverlayPosition.TOP_RIGHT,
                                    fallbackText,
                                )
                            }
                            return true
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean,
                        ): Boolean {
                            topRightImageOverlay.visibility = View.VISIBLE
                            return false
                        }
                    },
                )
                .into(topRightImageOverlay)
        } else if (fallbackText != null) {
            setTextOverlayText(
                OverlayPosition.TOP_RIGHT,
                fallbackText,
            )
        }
    }

    enum class OverlayPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
    }
}
