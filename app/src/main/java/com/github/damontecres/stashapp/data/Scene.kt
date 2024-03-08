package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.util.titleOrFilename
import kotlinx.parcelize.Parcelize

@Parcelize
data class Scene(
    val id: String,
    val title: String?,
    val date: String?,
    val details: String?,
    val streamUrl: String?,
    val screenshotUrl: String?,
    val studioId: String?,
    val studioName: String?,
    val streams: Map<String, String>,
    val spriteUrl: String?,
    val duration: Double?,
    val resumeTime: Double?,
    val videoCodec: String?,
    val videoResolution: Int?,
    val oCounter: Int?,
) : Parcelable {
    companion object {
        fun fromSlimSceneData(data: SlimSceneData): Scene {
            val streams =
                data.sceneStreams
                    .filter { it.label != null }.associate {
                        Pair(it.label.toString(), it.url)
                    }
            val fileData = data.files.firstOrNull()?.videoFileData
            return Scene(
                id = data.id,
                title = data.titleOrFilename,
                date = data.date,
                details = data.details,
                streamUrl = data.paths.stream,
                screenshotUrl = data.paths.screenshot,
                studioId = data.studio?.id,
                studioName = data.studio?.name,
                streams = streams,
                spriteUrl = data.paths.sprite,
                duration = fileData?.duration,
                resumeTime = data.resume_time,
                videoCodec = fileData?.video_codec,
                videoResolution = fileData?.height,
                oCounter = data.o_counter,
            )
        }
    }
}
