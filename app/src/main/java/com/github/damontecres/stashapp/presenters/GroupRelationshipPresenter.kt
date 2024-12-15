package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.data.GroupRelationshipData

class GroupRelationshipPresenter(
    callback: LongClickCallBack<GroupRelationshipData>? = null,
) : StashPresenter<GroupRelationshipData>(
        callback,
    ) {
    private val groupPresenter = GroupPresenter()

    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: GroupRelationshipData,
    ) {
        groupPresenter.doOnBindViewHolder(cardView, item.group)
        cardView.contentText = item.description
    }
}
