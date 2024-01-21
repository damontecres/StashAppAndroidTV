package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.util.createGlideUrl
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MarkerPresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val marker = item as MarkerData
        val cardView = viewHolder.view as ImageCardView
        val title =
            marker.title.ifBlank {
                marker.primary_tag.tagData.name
            }
        cardView.titleText = "$title - ${marker.seconds.toInt().toDuration(DurationUnit.SECONDS)}"
        cardView.contentText = marker.primary_tag.tagData.name

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        val url = createGlideUrl(marker.screenshot, viewHolder.view.context)
        Glide.with(viewHolder.view.context)
            .load(url)
            .transform(CenterCrop())
            .error(mDefaultCardImage)
            .into(cardView.mainImageView!!)
    }

    companion object {
        private val TAG = "MarkerPresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 198
    }
}
