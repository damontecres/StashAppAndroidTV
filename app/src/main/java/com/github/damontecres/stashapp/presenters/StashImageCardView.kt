package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.Constants
import java.util.EnumMap

class StashImageCardView(context: Context) : ImageCardView(context) {
    private val sSelectedBackgroundColor: Int =
        ContextCompat.getColor(context, R.color.selected_background)
    private val sDefaultBackgroundColor: Int =
        ContextCompat.getColor(context, R.color.default_card_background)

    var videoUrl: String? = null
    var videoPosition = -1L
    val videoView: PlayerView = findViewById(R.id.main_video)
    val mainView: View = findViewById(R.id.main_view)

    private val dataTypeViews =
        EnumMap<DataType, Pair<TextView, View>>(DataType::class.java)
    private val oCounterTextView: TextView
    private val oCounterIconView: View

    private val playVideoPreviews =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("playVideoPreviews", true)

    init {
        mainImageView.visibility = View.VISIBLE

        dataTypeViews[DataType.MOVIE] =
            Pair(
                findViewById(R.id.extra_movie_count),
                findViewById(R.id.extra_movie_icon),
            )
        dataTypeViews[DataType.MARKER] =
            Pair(
                findViewById(R.id.extra_marker_count),
                findViewById(R.id.extra_marker_icon),
            )
        dataTypeViews[DataType.PERFORMER] =
            Pair(
                findViewById(R.id.extra_performer_count),
                findViewById(R.id.extra_performer_icon),
            )
        dataTypeViews[DataType.TAG] =
            Pair(
                findViewById(R.id.extra_tag_count),
                findViewById(R.id.extra_tag_icon),
            )
        dataTypeViews[DataType.SCENE] =
            Pair(
                findViewById(R.id.extra_scene_count),
                findViewById(R.id.extra_scene_icon),
            )
        dataTypeViews[DataType.MOVIE] =
            Pair(
                findViewById(R.id.extra_movie_count),
                findViewById(R.id.extra_movie_icon),
            )
        oCounterTextView = findViewById(R.id.extra_ocounter_count)
        oCounterIconView = findViewById(R.id.extra_ocounter_icon)
    }

    override fun setSelected(selected: Boolean) {
        if (playVideoPreviews && videoUrl != null) {
            if (selected) {
                initPlayer()
                videoView.player?.seekTo(0)
                videoView.player?.playWhenReady = true
            } else {
                showImage()
                videoView.player?.release()
                videoView.player = null
            }
        }
        updateCardBackgroundColor(this, selected)
        val textView = findViewById<TextView>(androidx.leanback.R.id.title_text)
        textView.isSelected = selected
        super.setSelected(selected)
    }

    override fun setMainImageDimensions(
        width: Int,
        height: Int,
    ) {
        val lp = mainView.layoutParams
        lp.width = width
        lp.height = height
        mainView.layoutParams = lp
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
        if (videoUrl != null) {
            videoView.player?.release()
            videoView.player = null

            val apiKey =
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("stashApiKey", null)
            val dataSourceFactory =
                DataSource.Factory {
                    val dataSource = DefaultHttpDataSource.Factory().createDataSource()
                    if (!apiKey.isNullOrBlank()) {
                        dataSource.setRequestProperty(Constants.STASH_API_HEADER, apiKey)
                        dataSource.setRequestProperty(
                            Constants.STASH_API_HEADER.lowercase(),
                            apiKey,
                        )
                    }
                    dataSource
                }

            val player =
                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(
                        DefaultMediaSourceFactory(context).setDataSourceFactory(
                            dataSourceFactory,
                        ),
                    )
                    .build()
            val mediaItem =
                MediaItem.Builder()
                    .setUri(Uri.parse(videoUrl))
                    .setMimeType(MimeTypes.VIDEO_MP4)
                    .build()
            player.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            showVideo()
                        } else {
                            showImage()
                        }
                    }
                },
            )

            videoView.player = player
            player.setMediaItem(mediaItem)
            player.prepare()
            player.repeatMode = Player.REPEAT_MODE_ONE
            if (videoPosition > 0) {
                player.seekTo(videoPosition)
            }
            player.playWhenReady = true
        }
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
        mainImageView.visibility = View.GONE
        videoView.visibility = View.VISIBLE
    }

    fun showImage() {
        videoView.visibility = View.GONE
        mainImageView.visibility = View.VISIBLE
    }
}
