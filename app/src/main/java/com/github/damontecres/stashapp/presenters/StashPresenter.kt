package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.actions.CreateMarkerAction
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.svg.SvgSoftwareLayerSetter
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
        cardView.mainImageView.visibility = View.VISIBLE
        doOnBindViewHolder(viewHolder.view as StashImageCardView, item as T)
        if (cardView.isSelected) {
            cardView.isSelected = true
        }
    }

    fun loadImage(
        cardView: ImageCardView,
        url: String,
    ) {
        StashGlide.with(cardView.context, url)
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
        cardView.onUnbindViewHolder()
    }

    interface LongClickCallBack<T> {
        val popUpItems: List<String>

        fun onItemLongClick(
            item: T,
            popUpItemPosition: Int,
        )
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
                .addClassPresenter(ImageData::class.java, ImagePresenter())
                .addClassPresenter(GalleryData::class.java, GalleryPresenter())
                .addClassPresenter(OCounter::class.java, OCounterPresenter())
                .addClassPresenter(CreateMarkerAction::class.java, CreateMarkerActionPresenter())

        fun glideError(context: Context): RequestBuilder<PictureDrawable> {
            return Glide.with(context).`as`(PictureDrawable::class.java)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .error(ContextCompat.getDrawable(context, R.drawable.baseline_camera_indoor_48))
                .listener(SvgSoftwareLayerSetter())
        }
    }
}
