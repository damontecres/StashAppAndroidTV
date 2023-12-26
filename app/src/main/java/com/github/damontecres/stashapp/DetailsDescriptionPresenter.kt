package com.github.damontecres.stashapp

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.Scene

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(
            viewHolder: AbstractDetailsDescriptionPresenter.ViewHolder,
            item: Any) {
        val scene = item as Scene

        viewHolder.title.text = scene.title
        // TODO
//        viewHolder.subtitle.text = scene.studio?.name
        viewHolder.body.text = scene.details
    }
}