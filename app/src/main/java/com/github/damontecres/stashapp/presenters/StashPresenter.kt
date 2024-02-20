package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.VideoView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
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
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.svg.SvgSoftwareLayerSetter
import java.util.EnumMap
import kotlin.properties.Delegates

abstract class StashPresenter<T>(private val callback: LongClickCallBack<T>? = null) : Presenter() {
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
        doOnBindViewHolder(viewHolder.view as ImageCardView, item as T)
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

    fun showVideoOrImage(
        stashCardView: StashImageCardView,
        video: Boolean,
        callback: (VideoView) -> Unit,
    ) {
        val videoView = stashCardView.findViewById<VideoView>(R.id.main_video)
        if (video) {
            stashCardView.mainImageView.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            val lp = videoView.layoutParams
            lp.width = ScenePresenter.CARD_WIDTH
            lp.height = ScenePresenter.CARD_HEIGHT
            videoView.layoutParams = lp
            callback(videoView)
//            videoView.setVideoURI(
//                Uri.parse(item.paths.preview),
//                mapOf(
//                    Pair(Constants.STASH_API_HEADER, apiKey),
//                    Pair(Constants.STASH_API_HEADER.lowercase(), apiKey)
//                )
//            )
            videoView.start()
        } else {
            videoView.stopPlayback()
            stashCardView.mainImageView.visibility = View.VISIBLE
            videoView.visibility = View.GONE
            val lp = videoView.layoutParams
            lp.width = 0
            lp.height = 0
            videoView.layoutParams = lp
        }
    }

    abstract fun doOnBindViewHolder(
        cardView: ImageCardView,
        item: T,
    )

    final override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as StashImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
        cardView.videoView.stopPlayback()
    }

    protected fun setUpExtraRow(cardView: View): ViewGroup {
        val infoView = cardView.findViewById<ViewGroup>(androidx.leanback.R.id.info_field)
        return LayoutInflater.from(infoView.context)
            .inflate(R.layout.image_card_extra, infoView, true) as ViewGroup
    }

    protected fun setUpExtraRow(
        cardView: View,
        iconMap: EnumMap<DataType, Int>,
        oCounter: Int?,
    ) {
        val sceneExtra = setUpExtraRow(cardView)

        DataType.entries.forEach {
            val count = iconMap[it] ?: -1
            setUpIcon(sceneExtra, it, count)
        }
        setUpIcon(sceneExtra, null, oCounter ?: -1)
    }

    private fun setUpIcon(
        rootView: ViewGroup,
        dataType: DataType?,
        count: Int,
    ) {
        val textResId: Int
        val iconResId: Int
        when (dataType) {
            DataType.MOVIE -> {
                textResId = R.id.extra_movie_count
                iconResId = R.id.extra_movie_icon
            }

            DataType.MARKER -> {
                textResId = R.id.extra_marker_count
                iconResId = R.id.extra_marker_icon
            }

            DataType.PERFORMER -> {
                textResId = R.id.extra_performer_count
                iconResId = R.id.extra_performer_icon
            }

            DataType.TAG -> {
                textResId = R.id.extra_tag_count
                iconResId = R.id.extra_tag_icon
            }

            DataType.SCENE -> {
                textResId = R.id.extra_scene_count
                iconResId = R.id.extra_scene_icon
            }

            // Workaround for O Counter
            null -> {
                textResId = R.id.extra_ocounter_count
                iconResId = R.id.extra_ocounter_icon
            }

            else -> return
        }
        val textView = rootView.findViewById<TextView>(textResId)
        val iconView = rootView.findViewById<View>(iconResId)
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

    interface LongClickCallBack<T> {
        val popUpItems: List<String>

        fun onItemLongClick(
            item: T,
            popUpItemPosition: Int,
        )
    }

    fun interface SelectedCallBack {
        fun setSelected(selected: Boolean)
    }

    class StashImageCardView(context: Context) : ImageCardView(context) {
        private val sSelectedBackgroundColor: Int =
            ContextCompat.getColor(context, R.color.selected_background)
        private val sDefaultBackgroundColor: Int =
            ContextCompat.getColor(context, R.color.default_card_background)

        val videoView = findViewById<VideoView>(R.id.main_video)
        private var videoEnabled = false

        private var layoutParams: ViewGroup.LayoutParams? = null

        override fun setSelected(selected: Boolean) {
            if (videoEnabled) {
                if (selected) {
                    val lp = mainImageView.layoutParams
                    setLayout(videoView!!, lp.width, lp.height)
                    setLayout(mainImageView, 0, 0)
                    videoView.setOnErrorListener { mp, what, extra ->
                        videoEnabled = false
                        setLayout(mainImageView, lp.width, lp.height)
                        setLayout(videoView, 0, 0)
                        true
                    }
                    videoView.start()
                    videoView.seekTo(0)
                } else {
                    val lp = videoView!!.layoutParams
                    setLayout(mainImageView, lp.width, lp.height)
                    setLayout(videoView, 0, 0)
                    videoView.pause()
                }
            }
            updateCardBackgroundColor(this, selected)
            val textView = findViewById<TextView>(androidx.leanback.R.id.title_text)
            textView.isSelected = selected
            super.setSelected(selected)
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

        fun setVideoUrl(url: String) {
            val apiKey =
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("stashApiKey", null)
            val headers =
                if (apiKey.isNotNullOrBlank()) {
                    mapOf(
                        Pair(Constants.STASH_API_HEADER, apiKey),
                        Pair(Constants.STASH_API_HEADER.lowercase(), apiKey),
                    )
                } else {
                    mapOf()
                }

            videoEnabled = true
            videoView.setVideoURI(Uri.parse(url), headers)
            videoView.setOnCompletionListener {
                it.start()
            }
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
    }

    companion object {
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
