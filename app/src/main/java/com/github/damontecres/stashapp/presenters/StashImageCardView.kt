package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewSwitcher
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.leanback.widget.ImageCardView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
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
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.animateToInvisible
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.util.enableMarquee
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.FontSpan
import com.github.damontecres.stashapp.views.getRatingAsDecimalString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.EnumMap

class StashImageCardView(
    context: Context,
) : ImageCardView(context) {
    companion object {
        private const val TAG = "StashImageCardView"

        const val ICON_SPACING = "  "

        val FA_FONT = StashApplication.getFont(R.font.fa_solid_900)

        private val ICON_ORDER =
            listOf(
                DataType.SCENE,
                DataType.GROUP,
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
        set(value) {
            field = if (value.isNotNullOrBlank()) value else null
        }
    var videoPosition = -1L

    private var videoView: PlayerView? = null
    val mainView: ViewSwitcher = findViewById(R.id.main_view)

    private val iconTextView: TextView

    private val cardOverlay = findViewById<View>(R.id.card_overlay)
    private val textOverlays = EnumMap<OverlayPosition, TextView>(OverlayPosition::class.java)
    private val progressOverlay = findViewById<ImageView>(R.id.card_overlay_progress)
    private val topRightImageOverlay = findViewById<ImageView>(R.id.card_overlay_top_right_image)

    private var imageDimensionsSet = false

    private var videoPreviewAudio: Boolean = false
    private var playVideoPreviews = true
    private var videoDelay =
        context.resources.getInteger(R.integer.pref_key_ui_card_overlay_delay_default).toLong()

    private val listener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    showVideo()
                }
            }
        }

    private val color =
        context.getColor(androidx.leanback.R.color.lb_basic_card_content_text_color)
    private val textSize =
        context.resources.getDimension(androidx.leanback.R.dimen.lb_basic_card_content_text_size)
    private val sweat = ContextCompat.getDrawable(context, R.drawable.sweat_drops)!!

    private var delayJob: Job? = null

    private var mSelected = false
    private val selectedMessage = {
        if (mSelected) {
            super.setSelected(true)
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

        textOverlays[OverlayPosition.TOP_LEFT] = findViewById(R.id.card_overlay_top_left)
        textOverlays[OverlayPosition.TOP_RIGHT] = findViewById(R.id.card_overlay_top_right)
        textOverlays[OverlayPosition.BOTTOM_LEFT] = findViewById(R.id.card_overlay_bottom_left)
        textOverlays[OverlayPosition.BOTTOM_RIGHT] = findViewById(R.id.card_overlay_bottom_right)

        sweat.colorFilter =
            PorterDuffColorFilter(
                color,
                PorterDuff.Mode.MULTIPLY,
            )
        sweat.setBounds(0, 0, textSize.toInt(), textSize.toInt())
    }

    private fun setSelected(
        selected: Boolean,
        overrideDelay: Boolean,
    ) {
        mSelected = selected
        delayJob?.cancel()
        if (playVideoPreviews && videoUrl.isNotNullOrBlank()) {
            if (selected) {
                initPlayer()
                videoView?.player?.seekToDefaultPosition()
//                videoView?.player?.playWhenReady = true
            } else {
                showImage()
                StashExoPlayer.removeListener(listener)
                videoView?.player?.stop()
                videoView?.player = null
            }
        }
        if (selected) {
            if (!overrideDelay && videoDelay > 0) {
                val scope = findViewTreeLifecycleOwner()?.lifecycleScope
                if (scope != null) {
                    delayJob =
                        scope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                            delay(videoDelay)
                            withContext(Dispatchers.Main) {
                                hideOverlayAndPlayVideo()
                            }
                        }
                } else {
                    // No lifecycle scope
                    hideOverlayAndPlayVideo()
                }
            } else {
                // No delay, so do it immediately
                hideOverlayAndPlayVideo()
            }
        } else {
            cardOverlay.clearAnimation()
            cardOverlay.animateToVisible(animateTime)
        }
        updateCardBackgroundColor(this, selected)
        if (selected) {
            this.postDelayed(selectedMessage, videoDelay.coerceAtLeast(500L))
        } else {
            this.removeCallbacks(selectedMessage)
            super.setSelected(false)
        }
    }

    override fun setSelected(selected: Boolean) {
        setSelected(selected, false)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && isSelected) {
            setSelected(true, true)
        }
    }

    override fun setMainImageDimensions(
        width: Int,
        height: Int,
    ) {
        setMainImageDimensions(width, height, 0)
    }

    fun setMainImageDimensions(
        width: Int,
        height: Int,
        paddingDp: Int,
    ) {
        val cardSize =
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .getInt("cardSize", context.getString(R.string.card_size_default))
        val scaledWidth = (width * 5.0 / cardSize).toInt()
        val scaledHeight = (height * 5.0 / cardSize).toInt()
        val lp = mainView.layoutParams
        lp.width = scaledWidth
        lp.height = scaledHeight
        mainView.layoutParams = lp

        if (paddingDp > 0) {
            val scale = resources.displayMetrics.density
            val paddingPixels = (paddingDp * scale + 0.5f).toInt()
            mainImageView.setPadding(paddingPixels)
        }

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
            MediaItem
                .Builder()
                .setUri(Uri.parse(videoUrl))
                .setMimeType(MimeTypes.VIDEO_MP4)
                .build()
        val player = StashExoPlayer.getInstance(context, StashServer.requireCurrentServer())
        StashExoPlayer.addListener(listener)

        if (videoView == null) {
            // Create the PlayerView on demand
            videoView =
                LayoutInflater
                    .from(context)
                    .inflate(R.layout.stash_card_player_view, mainView, false) as PlayerView
            mainView.addView(videoView)
        }

        videoView!!.player = player
        player.setMediaItem(mediaItem, if (videoPosition > 0) videoPosition else C.TIME_UNSET)
        if (videoPreviewAudio) {
            player.volume = 1f
        } else {
            if (C.TRACK_TYPE_AUDIO !in player.trackSelectionParameters.disabledTrackTypes) {
                player.trackSelectionParameters =
                    player.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .build()
            }
            player.volume = 0f
        }
        player.prepare()
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.playWhenReady = videoDelay <= 0
    }

    fun setUpExtraRow(
        iconMap: EnumMap<DataType, Int>,
        oCounter: Int?,
        stringBuilder: (SpannableStringBuilder.() -> Unit)? = null,
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
                        append(ICON_SPACING)
                    }
                    setSpan(
                        FontSpan(FA_FONT),
                        start,
                        start + 1,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE,
                    )
                }
                stringBuilder?.invoke(this)
                if (oCounter != null && oCounter > 0) {
                    if (countStrings.isNotEmpty()) {
                        // Add space after previous icons
                        append(ICON_SPACING)
                    }
                    val imageSpan =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ImageSpan(sweat, ImageSpan.ALIGN_CENTER)
                        } else {
                            ImageSpan(sweat, ImageSpan.ALIGN_BASELINE)
                        }

                    val start = length
                    append(ICON_SPACING)
                    setSpan(imageSpan, start, start + 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    append(oCounter.toString())
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

    private fun hideOverlayAndPlayVideo() {
        if (videoUrl != null) {
            videoView?.player?.play()
        }
        cardOverlay.clearAnimation()
        cardOverlay.animateToInvisible(durationMs = animateTime)
    }

    fun getTextOverlay(position: OverlayPosition): TextView = textOverlays[position]!!

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
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_key_show_rating), true)
        if (rating100 != null && rating100 > 0 && showRatings) {
            val serverPrefs = StashServer.requireCurrentServer().serverPreferences
            val ratingText =
                getRatingAsDecimalString(rating100, serverPrefs.ratingsAsStars)
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

    fun onBindViewHolder() {
        val prefs =
            PreferenceManager
                .getDefaultSharedPreferences(context)
        playVideoPreviews = prefs.getBoolean("playVideoPreviews", true)
        videoDelay =
            prefs
                .getInt(
                    context.getString(R.string.pref_key_ui_card_overlay_delay),
                    context.resources.getInteger(R.integer.pref_key_ui_card_overlay_delay_default),
                ).toLong()
        videoPreviewAudio = prefs.getBoolean("videoPreviewAudio", false) &&
            !prefs.getBoolean(
                context.getString(R.string.pref_key_playback_start_muted),
                false,
            )
    }

    fun onUnbindViewHolder() {
        // Remove references to images so that the garbage collector can free up memory
        badgeImage = null
        mainImage = null
        videoUrl = null
//        videoView?.player?.release()
        videoView?.player = null

        mainImageView.setPadding(0)

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

            StashGlide
                .with(context, imageUrl)
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
                ).into(topRightImageOverlay)
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
