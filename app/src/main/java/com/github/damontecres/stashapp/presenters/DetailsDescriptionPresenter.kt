package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.concatIfNotBlank

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(
        viewHolder: ViewHolder,
        item: Any,
    ) {
        val scene = item as SlimSceneData

        viewHolder.title.text = scene.title
        viewHolder.body.text = scene.details

        val file = scene.files.firstOrNull()
        if (file != null) {
            val resolution = "${file.videoFileData.height}P"
            val duration = Constants.durationToString(file.videoFileData.duration)
            viewHolder.subtitle.text =
                concatIfNotBlank(
                    " - ",
                    scene.studio?.name,
                    scene.date,
                    duration,
                    resolution,
                )
        }
    }
}
