package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.actions.CreateMarkerAction
import com.github.damontecres.stashapp.util.Constants

class CreateMarkerActionPresenter(callback: LongClickCallBack<CreateMarkerAction>? = null) : StashPresenter<CreateMarkerAction>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: CreateMarkerAction,
    ) {
        cardView.titleText = "Create Marker"
        cardView.contentText = Constants.durationToString(item.position / 1000.0)
        cardView.setMainImageDimensions(
            ActionPresenter.CARD_WIDTH,
            ActionPresenter.CARD_HEIGHT,
        )
    }
}
