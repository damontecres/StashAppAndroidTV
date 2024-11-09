package com.github.damontecres.stashapp.presenters

import androidx.core.widget.NestedScrollView
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.playback.CodecSupport
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.onlyScrollIfNeeded
import com.github.damontecres.stashapp.util.readOnlyModeDisabled
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.StashRatingBar
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.parseTimeToString

/**
 * [AbstractDetailsDescriptionPresenter] for [com.github.damontecres.stashapp.SceneDetailsFragment]
 */
class SceneDetailsPresenter(val ratingCallback: StashRatingBar.RatingCallback) :
    AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(
        viewHolder: ViewHolder,
        item: Any,
    ) {
        val context = viewHolder.view.context
        val scene = item as FullSceneData
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
            val resolution = file.videoFileData.resolutionName()
            val duration = durationToString(file.videoFileData.duration)
            viewHolder.subtitle.text =
                concatIfNotBlank(
                    " - ",
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
                val supportedCodecs = CodecSupport.getSupportedCodecs(context)
                val videoSupported = supportedCodecs.isVideoSupported(videoFile.video_codec)
                val audioSupported = supportedCodecs.isAudioSupported(videoFile.audio_codec)
                val containerSupported =
                    supportedCodecs.isContainerFormatSupported(videoFile.format)

                val video =
                    if (videoSupported) {
                        "Video: ${videoFile.video_codec}"
                    } else {
                        "Video: ${videoFile.video_codec} (unsupported)"
                    }
                val audio =
                    if (audioSupported) {
                        "Audio: ${videoFile.audio_codec}"
                    } else {
                        "Audio: ${videoFile.audio_codec} (unsupported)"
                    }
                val format =
                    if (containerSupported) {
                        "Format: ${videoFile.format}"
                    } else {
                        "Format: ${videoFile.format} (unsupported)"
                    }

                debugInfo = listOf(video, audio, format).joinToString(", ")
            }
        }
        val playCount =
            if (scene.play_count != null && scene.play_count > 0) {
                context.getString(R.string.stashapp_play_count) + ": " + scene.play_count.toString()
            } else {
                null
            }
        val playDuration =
            if (scene.play_duration != null && scene.play_duration >= 1.0) {
                context.getString(R.string.stashapp_play_duration) + ": " + durationToString(scene.play_duration)
            } else {
                null
            }

        val playHistory =
            if (playCount != null || playDuration != null) {
                concatIfNotBlank(
                    ", ",
                    playCount,
                    playDuration,
                )
            } else {
                null
            }

        viewHolder.body.text =
            listOfNotNull(scene.details, "", debugInfo, playHistory, createdAt, updatedAt)
                .joinToString("\n")

        val ratingBar = viewHolder.view.findViewById<StashRatingBar>(R.id.rating_bar)
        ratingBar.rating100 = scene.rating100 ?: 0
        if (readOnlyModeDisabled()) {
            ratingBar.setRatingCallback(ratingCallback)
        } else {
            ratingBar.disable()
        }
    }
}
