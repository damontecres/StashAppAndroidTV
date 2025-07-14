package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.api.fragment.Caption
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.formatDate
import com.github.damontecres.stashapp.views.formatSeconds

data class Scene(
    val id: String,
    val title: String?,
    val subtitle: String?,
    val streamUrl: String?,
    val screenshotUrl: String?,
    val streams: Map<String, String>,
    val spriteUrl: String?,
    val duration: Double?,
    val resumeTime: Double?,
    val videoCodec: String?,
    val videoWidth: Int?,
    val videoHeight: Int?,
    val audioCodec: String?,
    val format: String?,
    val oCounter: Int?,
    val captionUrl: String?,
    val captions: List<Caption>,
) {
    val durationPosition get() = duration?.times(1000L)?.toLong()

    val hasCaptions get() = captionUrl.isNotNullOrBlank() && captions.isNotEmpty()

    companion object {
        fun fromFullSceneData(data: FullSceneData): Scene {
            val streams =
                data.sceneStreams
                    .filter { it.label != null }
                    .associate {
                        Pair(it.label.toString(), it.url)
                    }
            val fileData = data.files.firstOrNull()?.videoFile
            return Scene(
                id = data.id,
                title = data.titleOrFilename,
                subtitle = formatDate(data.date),
                streamUrl = data.paths.stream,
                screenshotUrl = data.paths.screenshot,
                streams = streams,
                spriteUrl = data.paths.sprite,
                duration = fileData?.duration,
                resumeTime = data.resume_time,
                videoCodec = fileData?.video_codec,
                videoWidth = fileData?.width,
                videoHeight = fileData?.height,
                audioCodec = fileData?.audio_codec,
                format = fileData?.format,
                oCounter = data.o_counter,
                captionUrl = data.paths.caption,
                captions = data.captions?.map { it.caption }.orEmpty(),
            )
        }

        fun fromVideoSceneData(data: VideoSceneData): Scene {
            val streams =
                data.sceneStreams
                    .filter { it.label != null }
                    .associate {
                        Pair(it.label.toString(), it.url)
                    }
            val fileData = data.files.firstOrNull()?.videoFile
            return Scene(
                id = data.id,
                title = data.titleOrFilename,
                subtitle = formatDate(data.date),
                streamUrl = data.paths.stream,
                screenshotUrl = data.paths.screenshot,
                streams = streams,
                spriteUrl = data.paths.sprite,
                duration = fileData?.duration,
                resumeTime = data.resume_time,
                videoCodec = fileData?.video_codec,
                videoWidth = fileData?.width,
                videoHeight = fileData?.height,
                audioCodec = fileData?.audio_codec,
                format = fileData?.format,
                oCounter = data.o_counter,
                captionUrl = data.paths.caption,
                captions = data.captions?.map { it.caption }.orEmpty(),
            )
        }

        fun fromMarkerData(marker: FullMarkerData): Scene {
            val video = marker.scene.videoSceneData
            val streams =
                video.sceneStreams
                    .filter { it.label != null }
                    .associate {
                        Pair(it.label.toString(), it.url)
                    }
            val fileData = video.files.firstOrNull()?.videoFile
            val title = marker.title.ifBlank { marker.primary_tag.tagData.name }
            val subtitle =
                if (marker.title.isNotBlank()) {
                    marker.primary_tag.tagData.name + " - " + video.titleOrFilename
                } else {
                    video.titleOrFilename
                } + " (${marker.formatSeconds})"
            return Scene(
                id = video.id,
                title = title,
                subtitle = subtitle,
                streamUrl = video.paths.stream,
                screenshotUrl = video.paths.screenshot,
                streams = streams,
                spriteUrl = video.paths.sprite,
                duration = fileData?.duration,
                resumeTime = video.resume_time,
                videoCodec = fileData?.video_codec,
                videoWidth = fileData?.width,
                videoHeight = fileData?.height,
                audioCodec = fileData?.audio_codec,
                format = fileData?.format,
                oCounter = video.o_counter,
                captionUrl = video.paths.caption,
                captions = video.captions?.map { it.caption }.orEmpty(),
            )
        }
    }
}
