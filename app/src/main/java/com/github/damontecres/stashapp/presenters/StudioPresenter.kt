package com.github.damontecres.stashapp.presenters

import android.widget.ImageView
import androidx.leanback.widget.ImageCardView
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.createGlideUrl
import kotlin.properties.Delegates

class StudioPresenter : StashPresenter() {
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val studio = item as StudioData
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = studio.name
        val contentText = ArrayList<String>()
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
            val apiKey =
                PreferenceManager.getDefaultSharedPreferences(vParent.context)
                    .getString("stashApiKey", "")
            val url = createGlideUrl(studio.image_path, apiKey)
            Glide.with(viewHolder.view.context)
                .load(url)
                .fitCenter()
                .error(mDefaultCardImage)
                .into(cardView.mainImageView!!)
        }
    }

    companion object {
        private const val TAG = "StudioPresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 198
    }
}
