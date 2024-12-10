package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.actions.StashAction

class ActionPresenter(callback: LongClickCallBack<StashAction>? = null) :
    StashPresenter<StashAction>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: StashAction,
    ) {
        cardView.titleText = item.actionName
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
    }

    companion object {
        private const val TAG = "ActionPresenter"

        const val CARD_WIDTH = 235
        const val CARD_HEIGHT = 160
    }
}
