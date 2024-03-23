package com.github.damontecres.stashapp.presenters

import androidx.core.widget.NestedScrollView
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.onlyScrollIfNeeded
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.StashRatingBar

class DetailsDescriptionPresenter(val ratingCallback: StashRatingBar.RatingCallback) :
    AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(
        viewHolder: ViewHolder,
        item: Any,
    ) {
        val context = viewHolder.view.context
        val scene = item as SlimSceneData

        viewHolder.title.text = scene.titleOrFilename

        val createdAt =
            context.getString(R.string.stashapp_created_at) + ": " +
                Constants.parseTimeToString(
                    scene.created_at,
                )
        val updatedAt =
            context.getString(R.string.stashapp_updated_at) + ": " +
                Constants.parseTimeToString(
                    scene.updated_at,
                )

        viewHolder.body.text = listOf(scene.details, "", createdAt, updatedAt).joinToString("\n")

        val scrollView = viewHolder.view.findViewById<NestedScrollView>(R.id.description_scrollview)
        val useScrollbar =
            PreferenceManager.getDefaultSharedPreferences(viewHolder.view.context)
                .getBoolean("scrollSceneDetails", true)
        if (useScrollbar) {
            scrollView.onlyScrollIfNeeded()
        } else {
            scrollView.isFocusable = false
            scrollView.isVerticalScrollBarEnabled = false
        }

        val file = scene.files.firstOrNull()
        if (file != null) {
            val resolution = "${file.videoFileData.height}P"
            val duration = Constants.durationToString(file.videoFileData.duration)
            viewHolder.subtitle.text =
                concatIfNotBlank(
                    " - ",
                    scene.studio?.studioData?.name,
                    scene.date,
                    duration,
                    resolution,
                )
        }
        val ratingBar = viewHolder.view.findViewById<StashRatingBar>(R.id.rating_bar)
        ratingBar.rating100 = scene.rating100 ?: 0
        ratingBar.setRatingCallback(ratingCallback)
    }
}
