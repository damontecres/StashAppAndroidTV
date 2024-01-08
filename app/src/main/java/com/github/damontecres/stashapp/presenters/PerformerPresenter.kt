package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.createGlideUrl

class PerformerPresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val performer = item as PerformerData
        val cardView = viewHolder.view as ImageCardView

        if (performer.image_path != null) {
            val title =
                performer.name + (if (!performer.disambiguation.isNullOrBlank()) " (${performer.disambiguation})" else "")
            cardView.titleText = title
            cardView.contentText = "${performer.scene_count} Scenes"
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            val apiKey =
                PreferenceManager.getDefaultSharedPreferences(vParent.context)
                    .getString("stashApiKey", "")
            val url = createGlideUrl(performer.image_path, apiKey)
            Glide.with(viewHolder.view.context)
                .load(url)
                .centerCrop()
                .error(mDefaultCardImage)
                .into(cardView.mainImageView!!)
        }
    }

    companion object {
        private const val TAG = "CardPresenter"

        const val CARD_HEIGHT = 381
        const val CARD_WIDTH = CARD_HEIGHT * 2 / 3
    }
}
