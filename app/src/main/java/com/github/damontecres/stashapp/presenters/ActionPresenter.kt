package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import com.github.damontecres.stashapp.actions.StashAction

class ActionPresenter(callback: LongClickCallBack<StashAction>? = null) :
    StashPresenter<StashAction>(callback) {
    override fun doOnBindViewHolder(
        viewHolder: ViewHolder,
        item: StashAction,
    ) {
        val cardView = viewHolder.view as ImageCardView
        cardView.titleText = item.actionName
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
    }

    companion object {
        private const val TAG = "ActionPresenter"

        const val CARD_WIDTH = 250
        const val CARD_HEIGHT = 250
    }
}
