package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.concatIfNotBlank

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(
        viewHolder: ViewHolder,
        item: Any,
    ) {
        val scene = item as Scene

        viewHolder.title.text = scene.title
        viewHolder.subtitle.text = concatIfNotBlank(" - ", scene.studioName, scene.date)
        viewHolder.body.text = scene.details
    }
}
