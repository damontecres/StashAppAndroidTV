package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.OCounter

class OCounterPresenter(
    callback: LongClickCallBack<OCounter>? = null,
) : StashPresenter<OCounter>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: OCounter,
    ) {
        cardView.blackImageBackground = false

        val text = cardView.context.getString(R.string.stashapp_o_count)
        cardView.titleText = "$text (${item.count})"
        cardView.setMainImageDimensions(ActionPresenter.CARD_WIDTH, ActionPresenter.CARD_HEIGHT)
        cardView.imageView.setPadding(
            IMAGE_PADDING,
            IMAGE_PADDING,
            IMAGE_PADDING,
            IMAGE_PADDING,
        )
        loadImage(cardView, R.drawable.sweat_drops)
    }

    companion object {
        const val IMAGE_PADDING = (ActionPresenter.CARD_WIDTH * .07).toInt()
    }
}
