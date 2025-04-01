package com.github.damontecres.stashapp.presenters

import android.util.Log
import androidx.core.widget.NestedScrollView
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import androidx.leanback.widget.Presenter
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.playback.CodecSupport
import com.github.damontecres.stashapp.playback.displayString
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.onlyScrollIfNeeded
import com.github.damontecres.stashapp.util.readOnlyModeDisabled
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.StashRatingBar
import com.github.damontecres.stashapp.views.byteRateSuffixes
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.formatBytes
import com.github.damontecres.stashapp.views.parseTimeToString

/**
 * [AbstractDetailsDescriptionPresenter] for [com.github.damontecres.stashapp.SceneDetailsFragment]
 */
class SceneDetailsPresenter(
    val server: StashServer,
    var ratingCallback: StashRatingBar.RatingCallback?,
) : AbstractDetailsDescriptionPresenter() {
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
        if (manager.getBoolean(
                context.getString(R.string.pref_key_show_playback_debug_info),
                false,
            )
        ) {
            val debugItems =
                mutableListOf("${context.getString(R.string.stashapp_scene_id)}: ${scene.id}")

            val file = scene.files.firstOrNull()
            if (file != null) {
                val resolution = file.videoFile.resolutionName()
                val duration = durationToString(file.videoFile.duration)
                viewHolder.subtitle.text =
                    concatIfNotBlank(
                        " - ",
                        scene.date,
                        duration,
                        resolution,
                    )

                val videoFile = file.videoFile
                val supportedCodecs = CodecSupport.getSupportedCodecs(context)
                val videoSupported = supportedCodecs.isVideoSupported(videoFile.video_codec)
                val audioSupported = supportedCodecs.isAudioSupported(videoFile.audio_codec)
                val containerSupported =
                    supportedCodecs.isContainerFormatSupported(videoFile.format)

                val res = "${videoFile.width}x${videoFile.height} @ ${
                    formatBytes(
                        videoFile.bit_rate,
                        byteRateSuffixes,
                    )
                }"
                val video =
                    if (videoSupported) {
                        "Video: ${videoFile.video_codec} - $res"
                    } else {
                        "Video: ${videoFile.video_codec} (unsupported) - $res"
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

                debugItems += listOf(video, audio, format)

                if (scene.captions?.isNotEmpty() == true) {
                    debugItems.add(
                        "Captions: " +
                            scene.captions
                                .map { it.caption.displayString(context) }
                                .joinNotNullOrBlank(", "),
                    )
                }
            }
            debugItems += ""
            debugInfo = debugItems.joinToString("\n")
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
        ratingBar.configure(server)
        ratingBar.rating100 = scene.rating100 ?: 0
        if (readOnlyModeDisabled()) {
            ratingBar.setRatingCallback(ratingCallback)
        } else {
            ratingBar.disable()
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        super.onUnbindViewHolder(viewHolder)
        val ratingBar = viewHolder.view.findViewById<StashRatingBar>(R.id.rating_bar)
        ratingBar.setRatingCallback(null)
        Log.v("SceneDetailsPresenter", "onUnbindViewHolder")
    }
}
