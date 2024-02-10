package com.github.damontecres.stashapp.presenters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.data.Tag
import com.github.damontecres.stashapp.util.enableMarquee
import java.util.EnumMap
import kotlin.properties.Delegates

abstract class StashPresenter : Presenter() {
    protected var vParent: ViewGroup by Delegates.notNull()
    protected var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        vParent = parent

        sDefaultBackgroundColor =
            ContextCompat.getColor(parent.context, R.color.default_card_background)
        sSelectedBackgroundColor =
            ContextCompat.getColor(
                parent.context,
                R.color.selected_background,
            )
        mDefaultCardImage =
            ContextCompat.getDrawable(parent.context, R.drawable.baseline_camera_indoor_48)

        val cardView =
            object : ImageCardView(parent.context) {
                override fun setSelected(selected: Boolean) {
                    updateCardBackgroundColor(this, selected)
                    val textView = findViewById<TextView>(androidx.leanback.R.id.title_text)
                    textView.isSelected = selected
                    super.setSelected(selected)
                }
            }

        val textView = cardView.findViewById<TextView>(androidx.leanback.R.id.title_text)
        textView.enableMarquee(false)
        val contentView = cardView.findViewById<TextView>(androidx.leanback.R.id.content_text)
        contentView.enableMarquee(false)

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        updateCardBackgroundColor(cardView, false)
        return ViewHolder(cardView)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun updateCardBackgroundColor(
        view: ImageCardView,
        selected: Boolean,
    ) {
        val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
        // Both background colors should be set because the view"s background is temporarily visible
        // during animations.
        view.setBackgroundColor(color)
        view.setInfoAreaBackgroundColor(color)
    }

    protected fun setUpExtraRow(
        cardView: View,
        iconMap: EnumMap<DataType, Int>,
        oCounter: Int?,
    ) {
        val infoView = cardView.findViewById<ViewGroup>(androidx.leanback.R.id.info_field)
        val sceneExtra =
            LayoutInflater.from(infoView.context)
                .inflate(R.layout.image_card_extra, infoView, true) as ViewGroup

        iconMap.forEach {
            setUpIcon(sceneExtra, it.key, it.value)
        }
        if (oCounter != null && oCounter > 0) {
            setUpIcon(sceneExtra, null, oCounter)
        }
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

            else -> throw IllegalArgumentException()
        }
        val textView = rootView.findViewById<TextView>(textResId)
        val iconView = rootView.findViewById<View>(iconResId)
        if (count > 0) {
            textView.text = count.toString()
            textView.visibility = View.VISIBLE
            iconView.visibility = View.VISIBLE
            (textView.parent as ViewGroup).visibility = View.VISIBLE
        }
    }

    companion object {
        val SELECTOR: ClassPresenterSelector =
            ClassPresenterSelector()
                .addClassPresenter(PerformerData::class.java, PerformerPresenter())
                .addClassPresenter(SlimSceneData::class.java, ScenePresenter())
                .addClassPresenter(StudioData::class.java, StudioPresenter())
                .addClassPresenter(Tag::class.java, TagPresenter())
                .addClassPresenter(MovieData::class.java, MoviePresenter())
                .addClassPresenter(StashSavedFilter::class.java, StashFilterPresenter())
                .addClassPresenter(StashCustomFilter::class.java, StashFilterPresenter())
                .addClassPresenter(StashAction::class.java, ActionPresenter())
                .addClassPresenter(MarkerData::class.java, MarkerPresenter())
                .addClassPresenter(OCounter::class.java, OCounterPresenter())
    }
}
