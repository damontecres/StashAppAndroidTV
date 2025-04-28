package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.defaultCardHeight
import com.github.damontecres.stashapp.util.defaultCardWidth

/**
 * A no-op implementation of [StashPresenter] that accepts a null item
 */
class NullPresenter(
    private val width: Int,
    private val height: Int,
) : StashPresenter<Any?>() {
    constructor(dataType: DataType) : this(dataType.defaultCardWidth, dataType.defaultCardHeight)

    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: Any?,
    ) {
        // no-op
    }

    fun bindNull(cardView: StashImageCardView) {
        cardView.setMainImageDimensions(width, height)
    }
}
