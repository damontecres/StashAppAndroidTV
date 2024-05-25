package com.github.damontecres.stashapp.presenters

import androidx.core.widget.NestedScrollView
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.onlyScrollIfNeeded
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.StashRatingBar
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.parseTimeToString

class DetailsDescriptionPresenter(val ratingCallback: StashRatingBar.RatingCallback) :
    AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(
        viewHolder: ViewHolder,
        item: Any,
    ) {
        val context = viewHolder.view.context
        val scene = item as SlimSceneData
        val manager = PreferenceManager.getDefaultSharedPreferences(context)

        viewHolder.title.text = scene.titleOrFilename

        val createdAt =
            context.getString(R.string.stashapp_created_at) + ": " +
                parseTimeToString(
                    scene.created_at,
                )
        val updatedAt =
            context.getString(R.string.stashapp_updated_at) + ": " +
                parseTimeToString(
                    scene.updated_at,
                )

        val scrollView = viewHolder.view.findViewById<NestedScrollView>(R.id.description_scrollview)
        val useScrollbar = manager.getBoolean("scrollSceneDetails", true)
        if (useScrollbar) {
            scrollView.onlyScrollIfNeeded()
        } else {
            scrollView.isFocusable = false
            scrollView.isVerticalScrollBarEnabled = false
        }

        var debugInfo: String? = null
        val file = scene.files.firstOrNull()
        if (file != null) {
            val resolution = "${file.videoFileData.height}P"
            val duration = durationToString(file.videoFileData.duration)
            viewHolder.subtitle.text =
                concatIfNotBlank(
                    " - ",
                    scene.studio?.studioData?.name,
                    scene.date,
                    duration,
                    resolution,
                )

            if (manager.getBoolean(
                    context.getString(R.string.pref_key_show_playback_debug_info),
                    false,
                )
            ) {
                val videoFile = file.videoFileData
                debugInfo =
                    listOf(
                        "Video: ${videoFile.video_codec}",
                        "Audio: ${videoFile.audio_codec}",
                        "Format: ${videoFile.format}",
                    ).joinToString(", ")
            }
        }

        viewHolder.body.text =
            listOfNotNull(scene.details, "", debugInfo, createdAt, updatedAt)
                .joinToString("\n")

        val ratingBar = viewHolder.view.findViewById<StashRatingBar>(R.id.rating_bar)
        ratingBar.rating100 = scene.rating100 ?: 0
        ratingBar.setRatingCallback(ratingCallback)
    }
}
