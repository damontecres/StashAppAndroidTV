package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
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
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.createGlideUrl
import com.github.damontecres.stashapp.util.enableMarquee
import com.github.damontecres.stashapp.util.svg.SvgSoftwareLayerSetter
import java.util.EnumMap
import kotlin.properties.Delegates

abstract class StashPresenter<T>(private val callback: LongClickCallBack<T>? = null) :
    Presenter() {
    protected var vParent: ViewGroup by Delegates.notNull()
    protected var mDefaultCardImage: Drawable? = null

    final override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        vParent = parent
        mDefaultCardImage =
            ContextCompat.getDrawable(parent.context, R.drawable.baseline_camera_indoor_48)

        val cardView = StashImageCardView(parent.context)

        val textView = cardView.findViewById<TextView>(androidx.leanback.R.id.title_text)
        textView.enableMarquee(false)
        val contentView = cardView.findViewById<TextView>(androidx.leanback.R.id.content_text)
        contentView.enableMarquee(false)

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.updateCardBackgroundColor(cardView, false)

        return ViewHolder(cardView)
    }

    final override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any,
    ) {
        val cardView = viewHolder.view as StashImageCardView

        if (callback != null) {
            cardView.setOnLongClickListener(
                PopupOnLongClickListener(
                    callback.popUpItems,
                ) { _, _, pos, _ ->
                    callback.onItemLongClick(item as T, pos)
                },
            )
        }
        doOnBindViewHolder(viewHolder.view as StashImageCardView, item as T)
    }

    fun loadImage(
        cardView: ImageCardView,
        url: String,
    ) {
        val glideUrl = createGlideUrl(url, cardView.context)
        Glide.with(cardView.context)
            .load(glideUrl)
            .fitCenter()
            .error(glideError(cardView.context))
            .into(cardView.mainImageView!!)
    }

    abstract fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: T,
    )

    open override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as StashImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
        cardView.videoView.player?.release()
        cardView.videoView.player = null
    }

    interface LongClickCallBack<T> {
        val popUpItems: List<String>

        fun onItemLongClick(
            item: T,
            popUpItemPosition: Int,
        )
    }

    class StashImageCardView(context: Context) : ImageCardView(context) {
        private val sSelectedBackgroundColor: Int =
            ContextCompat.getColor(context, R.color.selected_background)
        private val sDefaultBackgroundColor: Int =
            ContextCompat.getColor(context, R.color.default_card_background)

        var videoUrl: String? = null
        var videoPosition = -1L
        val videoView: PlayerView = findViewById(R.id.main_video)
        private val dataTypeViews =
            EnumMap<DataType, Pair<TextView, View>>(DataType::class.java)
        private val oCounterTextView: TextView
        private val oCounterIconView: View

        private var imageWidth by Delegates.notNull<Int>()
        private var imageHeight by Delegates.notNull<Int>()

        private val playVideoPreviews =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("playVideoPreviews", true)

        init {
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
                    setLayout(videoView, 0, 0)
                    setLayout(mainImageView, imageWidth, imageHeight)
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
            super.setMainImageDimensions(width, height)
            imageWidth = width
            imageHeight = height
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

        private fun setLayout(
            view: View,
            width: Int,
            height: Int,
        ) {
            val lp = view.layoutParams
            lp.width = width
            lp.height = height
            view.layoutParams = lp
            if (width == 0 && height == 0) {
                view.visibility = View.GONE
            } else {
                view.visibility = View.VISIBLE
            }
        }

        @OptIn(UnstableApi::class)
        private fun initPlayer() {
            if (videoUrl != null) {
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
                                setLayout(videoView, imageWidth, imageHeight)
                                setLayout(mainImageView, 0, 0)
                            } else {
                                setLayout(videoView, 0, 0)
                                setLayout(mainImageView, imageWidth, imageHeight)
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
    }

    companion object {
        private const val TAG = "StashPresenter"

        val SELECTOR: ClassPresenterSelector =
            ClassPresenterSelector()
                .addClassPresenter(PerformerData::class.java, PerformerPresenter())
                .addClassPresenter(SlimSceneData::class.java, ScenePresenter())
                .addClassPresenter(StudioData::class.java, StudioPresenter())
                .addClassPresenter(TagData::class.java, TagPresenter())
                .addClassPresenter(MovieData::class.java, MoviePresenter())
                .addClassPresenter(StashSavedFilter::class.java, StashFilterPresenter())
                .addClassPresenter(StashCustomFilter::class.java, StashFilterPresenter())
                .addClassPresenter(StashAction::class.java, ActionPresenter())
                .addClassPresenter(MarkerData::class.java, MarkerPresenter())
                .addClassPresenter(OCounter::class.java, OCounterPresenter())

        fun glideError(context: Context): RequestBuilder<PictureDrawable> {
            return Glide.with(context).`as`(PictureDrawable::class.java)
                .error(ContextCompat.getDrawable(context, R.drawable.baseline_camera_indoor_48))
                .listener(SvgSoftwareLayerSetter())
        }
    }
}
