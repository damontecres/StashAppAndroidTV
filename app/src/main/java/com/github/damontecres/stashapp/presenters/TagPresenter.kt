package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import com.github.damontecres.stashapp.data.Tag

class TagPresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val tag = item as Tag
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = tag.name
        // TODO: content text
        cardView.contentText = "${tag.sceneCount} scenes"
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        // TODO: fetch image
    }

    companion object {
        private const val TAG = "TagPresenter"

        const val CARD_WIDTH = 250
        const val CARD_HEIGHT = 250
    }
}
