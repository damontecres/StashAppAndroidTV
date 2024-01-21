package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.github.damontecres.stashapp.data.Scene

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(
        viewHolder: ViewHolder,
        item: Any,
    ) {
        val scene = item as Scene

        viewHolder.title.text = scene.title
        viewHolder.subtitle.text = scene.studioName
        // TODO: override max lines in the body text somehow
        viewHolder.body.text = scene.details
    }
}
