package com.github.damontecres.stashapp.presenters

import androidx.appcompat.content.res.AppCompatResources
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.OCounter

class OCounterPresenter(
    callback: LongClickCallBack<OCounter>? = null,
) : StashPresenter<OCounter>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: OCounter,
    ) {
        val text = cardView.context.getString(R.string.stashapp_o_counter)
        cardView.titleText = "$text (${item.count})"
        cardView.setMainImageDimensions(ActionPresenter.CARD_WIDTH, ActionPresenter.CARD_HEIGHT)
        cardView.mainImageView.setPadding(
            IMAGE_PADDING,
            IMAGE_PADDING,
            IMAGE_PADDING,
            IMAGE_PADDING,
        )
        cardView.mainImageView.setImageDrawable(
            AppCompatResources.getDrawable(
                cardView.context,
                R.drawable.sweat_drops,
            ),
        )
    }

    companion object {
        const val IMAGE_PADDING = (ActionPresenter.CARD_WIDTH * .07).toInt()
    }
}
