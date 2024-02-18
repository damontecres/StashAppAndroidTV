package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import com.github.damontecres.stashapp.api.fragment.MarkerData
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MarkerPresenter(callback: LongClickCallBack<MarkerData>? = null) :
    StashPresenter<MarkerData>(callback) {
    override fun doOnBindViewHolder(
        cardView: ImageCardView,
        item: MarkerData,
    ) {
        val title =
            item.title.ifBlank {
                item.primary_tag.tagData.name
            }
        cardView.titleText = "$title - ${item.seconds.toInt().toDuration(DurationUnit.SECONDS)}"
        cardView.contentText =
            if (item.title.isNotBlank()) item.primary_tag.tagData.name else null

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        loadImage(cardView, item.screenshot)
    }

    companion object {
        private const val TAG = "MarkerPresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 198
    }
}
