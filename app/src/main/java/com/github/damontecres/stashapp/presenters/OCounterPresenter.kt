package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.OCounter

class OCounterPresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any,
    ) {
        val counter = item as OCounter
        val cardView = viewHolder.view as ImageCardView

        val text = viewHolder.view.context.getString(R.string.stashapp_o_counter)
        cardView.titleText = "$text (${counter.count})"

        cardView.setMainImageDimensions(ActionPresenter.CARD_WIDTH, ActionPresenter.CARD_HEIGHT)
        // TODO image
    }
}
