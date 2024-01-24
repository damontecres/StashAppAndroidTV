package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.concatIfNotBlank
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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

        viewHolder.subtitle.text =
            concatIfNotBlank(
                " - ",
                scene.studioName,
                scene.date,
                scene.duration?.toDuration(DurationUnit.SECONDS).toString(),
                resolution,
            )
        viewHolder.body.text = scene.details
    }
}
