package com.github.damontecres.stashapp.presenters

import androidx.appcompat.content.res.AppCompatResources
import androidx.leanback.widget.ImageCardView
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.OCounter

class OCounterPresenter(callback: LongClickCallBack<OCounter>? = null) :
    StashPresenter<OCounter>(callback) {
    override fun doOnBindViewHolder(
        cardView: ImageCardView,
        item: OCounter,
    ) {
        val text = cardView.context.getString(R.string.stashapp_o_counter)
        cardView.titleText = "$text (${item.count})"
        cardView.setMainImageDimensions(ActionPresenter.CARD_WIDTH, ActionPresenter.CARD_HEIGHT)
        cardView.mainImageView.setImageDrawable(
            AppCompatResources.getDrawable(
                cardView.context,
                R.drawable.sweat_drops,
            ),
        )
    }
}
