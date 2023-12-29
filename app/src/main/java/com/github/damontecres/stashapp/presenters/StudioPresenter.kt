package com.github.damontecres.stashapp.presenters

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.createGlideUrl
import kotlin.properties.Delegates

class StudioPresenter : Presenter() {
    private var vParent: ViewGroup by Delegates.notNull()
    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        vParent = parent

        sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
        sSelectedBackgroundColor = ContextCompat.getColor(
            parent.context,
            R.color.selected_background
        )
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.baseline_camera_indoor_48)

        val cardView = object : ImageCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                updateCardBackgroundColor(this, selected)
                super.setSelected(selected)
            }
        }

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        updateCardBackgroundColor(cardView, false)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
//        val scene = sceneFromSlimSceneData(item as SlimSceneData)
        val studio = item as StudioData
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = studio.name
        var contentText = ArrayList<String>()
        if (studio.scene_count > 0) {
            contentText += "${studio.scene_count}S"
        }
        if (studio.performer_count > 0) {
            contentText += "${studio.performer_count}P"
        }
        cardView.contentText = contentText.joinToString(" ")

        if (!studio.image_path.isNullOrBlank()) {
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            cardView.setMainImageScaleType(ImageView.ScaleType.FIT_CENTER)
            val apiKey = PreferenceManager.getDefaultSharedPreferences(vParent.context)
                .getString("stashApiKey", "")
            val url = createGlideUrl(studio.image_path, apiKey)
            Glide.with(viewHolder.view.context)
                .load(url)
                .fitCenter()
                .error(mDefaultCardImage)
                .into(cardView.mainImageView!!)
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        Log.d(TAG, "onUnbindViewHolder")
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
        val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
        // Both background colors should be set because the view"s background is temporarily visible
        // during animations.
        view.setBackgroundColor(color)
        view.setInfoAreaBackgroundColor(color)
    }

    companion object {
        private val TAG = "CardPresenter"

        private val CARD_WIDTH = 351
        private val CARD_HEIGHT = 198
    }
}