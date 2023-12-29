package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import kotlinx.parcelize.Parcelize

@Parcelize
data class Scene (
    var id: Long,
    var title: String?,
    var details: String?,
    var streamUrl: String?,
    var screenshotUrl: String?,
    var studioId: String?,
    var studioName: String?,
) : Parcelable

fun sceneFromSlimSceneData(data: SlimSceneData): Scene{
    return Scene(id=data.id.toLong(), title=data.title, details=data.details,
        streamUrl=data.paths.stream, screenshotUrl = data.paths.screenshot,
        studioId = data.studio?.id, studioName = data.studio?.name
    )
}