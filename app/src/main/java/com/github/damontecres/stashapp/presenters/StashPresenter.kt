package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.graphics.drawable.PictureDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.actions.CreateMarkerAction
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.GroupRelationshipData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.presenters.StashPresenter.PopUpAction
import com.github.damontecres.stashapp.presenters.StashPresenter.PopUpFilter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.svg.SvgSoftwareLayerSetter

abstract class StashPresenter<T>(
    private var callback: LongClickCallBack<T>? = null,
) : Presenter() {
    val longClickCallBack: LongClickCallBack<T>
        get() {
            if (callback == null) {
                callback = getDefaultLongClickCallBack()
            }
            return callback!!
        }

    final override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = StashImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = false
        cardView.updateCardBackgroundColor(cardView, false)
        return ViewHolder(cardView)
    }

    final override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val cardView = viewHolder.view as StashImageCardView
        cardView.onBindViewHolder()
        if (item != null) {
            val localCallBack = callback ?: getDefaultLongClickCallBack()
            val popUpItems = localCallBack.getPopUpItems(item as T)
            cardView.setOnLongClickListener(
                PopupOnLongClickListener(
                    popUpItems.map { it.text },
                ) { _, _, pos, _ ->
                    localCallBack.onItemLongClick(cardView, item as T, popUpItems[pos])
                },
            )

            cardView.mainImageView.visibility = View.VISIBLE
            doOnBindViewHolder(viewHolder.view as StashImageCardView, item as T)
        } else if (this is NullPresenter) {
            bindNull(cardView)
        }
    }

    fun loadImage(
        cardView: ImageCardView,
        url: String,
    ) {
        val cropImages =
            PreferenceManager
                .getDefaultSharedPreferences(cardView.context)
                .getBoolean(cardView.context.getString(R.string.pref_key_crop_card_images), true)
        if (url.contains("default=true")) {
            cardView.mainImageView.setBackgroundColor(cardView.context.getColor(android.R.color.transparent))
        } else {
            cardView.mainImageView.setBackgroundColor(cardView.context.getColor(android.R.color.black))
        }
        if (cropImages) {
            cardView.mainImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            StashGlide
                .with(cardView.context, url)
                .optionalCenterCrop()
                .error(glideError(cardView.context))
                .into(cardView.mainImageView!!)
        } else {
            cardView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
            StashGlide
                .with(cardView.context, url)
                .error(glideError(cardView.context))
                .into(cardView.mainImageView!!)
        }
    }

    abstract fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: T,
    )

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as StashImageCardView
        cardView.onUnbindViewHolder()
    }

    open fun getDefaultLongClickCallBack(): LongClickCallBack<T> =
        LongClickCallBack(
            PopUpItem.DEFAULT to PopUpAction { cardView, _ -> cardView.performClick() },
        )

    fun addLongCLickAction(
        popUpItem: PopUpItem,
        filter: PopUpFilter<T> = PopUpFilter { true },
        action: PopUpAction<T>,
    ): StashPresenter<T> {
        longClickCallBack.addAction(popUpItem, filter, action)
        return this
    }

    class LongClickCallBack<T>(
        vararg actions: Pair<PopUpItem, PopUpAction<T>>,
    ) {
        private val actions = mutableMapOf<PopUpItem, PopUpAction<T>>()
        private val filters = mutableMapOf<PopUpItem, PopUpFilter<T>>()

        init {
            this.actions.putAll(actions)
            actions.forEach { filters[it.first] = PopUpFilter { true } }
        }

        fun addAction(
            popUpItem: PopUpItem,
            filter: PopUpFilter<T> = PopUpFilter { true },
            action: PopUpAction<T>,
        ): LongClickCallBack<T> {
            actions[popUpItem] = action
            filters[popUpItem] = filter
            return this
        }

        fun getPopUpItems(item: T): List<PopUpItem> = actions.keys.filter { filters[it]!!.include(item) }

        fun onItemLongClick(
            cardView: StashImageCardView,
            item: T,
            popUpItem: PopUpItem,
        ) {
            actions[popUpItem]!!.run(cardView, item)
        }
    }

    fun interface PopUpFilter<T> {
        fun include(item: T): Boolean
    }

    fun interface PopUpAction<T> {
        fun run(
            cardView: StashImageCardView,
            item: T,
        )
    }

    data class PopUpItem(
        val id: Long,
        val text: String,
    ) {
        constructor(id: Int, text: String) : this(id.toLong(), text)

        constructor(
            id: Long,
            @StringRes stringId: Int,
        ) : this(
            id,
            StashApplication.getApplication().getString(stringId),
        )

        companion object {
            const val DEFAULT_ID = -1L
            const val REMOVE_ID = -2L
            const val PLAY_FROM_ID = -3L

            val DEFAULT =
                PopUpItem(DEFAULT_ID, StashApplication.getApplication().getString(R.string.go_to))

            val REMOVE_POPUP_ITEM =
                PopUpItem(
                    REMOVE_ID,
                    StashApplication.getApplication().getString(R.string.stashapp_actions_remove),
                )

            val PLAY_FROM = PopUpItem(PLAY_FROM_ID, "Play from here")
        }
    }

    companion object {
        private const val TAG = "StashPresenter"

        fun defaultClassPresenterSelector(): ClassPresenterSelector =
            ClassPresenterSelector()
                .addClassPresenter(PerformerData::class.java, PerformerPresenter())
                .addClassPresenter(SlimSceneData::class.java, ScenePresenter())
                .addClassPresenter(StudioData::class.java, StudioPresenter())
                .addClassPresenter(TagData::class.java, TagPresenter())
                .addClassPresenter(GroupData::class.java, GroupPresenter())
                .addClassPresenter(FilterArgs::class.java, FilterArgsPresenter())
                .addClassPresenter(StashAction::class.java, ActionPresenter())
                .addClassPresenter(MarkerData::class.java, MarkerPresenter())
                .addClassPresenter(ImageData::class.java, ImagePresenter())
                .addClassPresenter(GalleryData::class.java, GalleryPresenter())
                .addClassPresenter(OCounter::class.java, OCounterPresenter())
                .addClassPresenter(CreateMarkerAction::class.java, CreateMarkerActionPresenter())
                .addClassPresenter(GroupRelationshipData::class.java, GroupRelationshipPresenter())

        fun glideError(context: Context): RequestBuilder<PictureDrawable> =
            Glide
                .with(context)
                .`as`(PictureDrawable::class.java)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .error(ContextCompat.getDrawable(context, R.drawable.baseline_camera_indoor_48))
                .listener(SvgSoftwareLayerSetter())
    }
}
