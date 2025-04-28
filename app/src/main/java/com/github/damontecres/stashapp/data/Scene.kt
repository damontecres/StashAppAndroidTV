package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.api.fragment.Caption
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename

data class Scene(
    val id: String,
    val title: String?,
    val date: String?,
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
                date = data.date,
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
                date = data.date,
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
    }
}
