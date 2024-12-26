package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import kotlinx.parcelize.Parcelize

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
    val videoResolution: Int?,
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
            val fileData = data.files.firstOrNull()?.videoFileData
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
                videoResolution = fileData?.height,
                audioCodec = fileData?.audio_codec,
                format = fileData?.format,
                oCounter = data.o_counter,
                captionUrl = data.paths.caption,
                captions = data.captions?.map { it.toCaption() }.orEmpty(),
            )
        }

        fun fromSlimSceneData(data: SlimSceneData): Scene {
            val streams =
                data.sceneStreams
                    .filter { it.label != null }
                    .associate {
                        Pair(it.label.toString(), it.url)
                    }
            val fileData = data.files.firstOrNull()?.videoFileData
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
                videoResolution = fileData?.height,
                audioCodec = fileData?.audio_codec,
                format = fileData?.format,
                oCounter = data.o_counter,
                captionUrl = data.paths.caption,
                captions = data.captions?.map { it.toCaption() }.orEmpty(),
            )
        }

        fun fromVideoSceneData(data: VideoSceneData): Scene {
            val streams =
                data.sceneStreams
                    .filter { it.label != null }
                    .associate {
                        Pair(it.label.toString(), it.url)
                    }
            val fileData = data.files.firstOrNull()?.videoFileData
            return Scene(
                id = data.id,
                title = data.titleOrFilename,
                date = data.date,
                streamUrl = data.paths.stream,
                screenshotUrl = data.paths.screenshot,
                streams = streams,
                spriteUrl = data.paths.sprite,
                duration = fileData?.duration,
                resumeTime = null,
                videoCodec = fileData?.video_codec,
                videoResolution = fileData?.height,
                audioCodec = fileData?.audio_codec,
                format = fileData?.format,
                oCounter = data.o_counter,
                captionUrl = null,
                captions = emptyList(),
            )
        }
    }
}

@Parcelize
data class Caption(
    val lang: String,
    val type: String,
) : Parcelable

fun SlimSceneData.Caption.toCaption(): Caption = Caption(language_code, caption_type)

fun FullSceneData.Caption.toCaption(): Caption = Caption(language_code, caption_type)
