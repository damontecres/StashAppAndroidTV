package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.api.fragment.GroupRelationshipData
import com.github.damontecres.stashapp.util.StashServer

class GroupRelationshipPresenter(
    server: StashServer,
    callback: LongClickCallBack<GroupRelationshipData>? = null,
) : StashPresenter<GroupRelationshipData>(server, callback) {
    private val groupPresenter = GroupPresenter(server)

    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: GroupRelationshipData,
    ) {
        groupPresenter.doOnBindViewHolder(cardView, item.group)
        cardView.contentText = item.description
    }
}
