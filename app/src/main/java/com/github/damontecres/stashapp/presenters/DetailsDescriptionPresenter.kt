package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.concatIfNotBlank

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(
        viewHolder: ViewHolder,
        item: Any,
    ) {
        val scene = item as Scene
        viewHolder.title.text = scene.title

        val resolution =
            if (scene.videoResolution != null) {
                scene.videoResolution.toString() + "P"
            } else {
                null
            }

        val duration =
            if (scene.duration != null) {
                Constants.durationToString(scene.duration)
            } else {
                null
            }

        viewHolder.subtitle.text =
            concatIfNotBlank(
                " - ",
                scene.studioName,
                scene.date,
                duration,
                resolution,
            )
        viewHolder.body.text = scene.details
    }
}
