package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.util.createGlideUrl
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MarkerPresenter(callback: LongClickCallBack<MarkerData>? = null) :
    StashPresenter<MarkerData>(callback) {
    override fun doOnBindViewHolder(
        viewHolder: ViewHolder,
        item: MarkerData,
    ) {
        val cardView = viewHolder.view as ImageCardView
        val title =
            item.title.ifBlank {
                item.primary_tag.tagData.name
            }
        cardView.titleText = "$title - ${item.seconds.toInt().toDuration(DurationUnit.SECONDS)}"
        cardView.contentText =
            if (item.title.isNotBlank()) item.primary_tag.tagData.name else null

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        val url = createGlideUrl(item.screenshot, viewHolder.view.context)
        Glide.with(viewHolder.view.context)
            .load(url)
            .transform(CenterCrop())
            .error(mDefaultCardImage)
            .into(cardView.mainImageView!!)
    }

    companion object {
        private const val TAG = "MarkerPresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 198
    }
}
