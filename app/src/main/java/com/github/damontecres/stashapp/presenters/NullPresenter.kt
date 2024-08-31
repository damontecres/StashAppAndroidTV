package com.github.damontecres.stashapp.presenters

/**
 * A no-op implementation of [StashPresenter] that accepts a null item
 */
class NullPresenter : StashPresenter<Any?>() {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: Any?,
    ) {
        // no-op
    }
}
