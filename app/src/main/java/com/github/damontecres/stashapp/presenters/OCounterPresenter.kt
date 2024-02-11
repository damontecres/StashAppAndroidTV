package com.github.damontecres.stashapp.presenters

import androidx.appcompat.content.res.AppCompatResources
import androidx.leanback.widget.ImageCardView
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.OCounter

class OCounterPresenter(callback: LongClickCallBack? = null) : StashPresenter(callback) {
    override fun doOnBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val counter = item as OCounter
        val cardView = viewHolder.view as ImageCardView

        val context = viewHolder.view.context

        val text = context.getString(R.string.stashapp_o_counter)
        cardView.titleText = "$text (${counter.count})"
        cardView.setMainImageDimensions(ActionPresenter.CARD_WIDTH, ActionPresenter.CARD_HEIGHT)
        cardView.mainImageView.setImageDrawable(
            AppCompatResources.getDrawable(
                context,
                R.drawable.sweat_drops,
            ),
        )
    }
}
