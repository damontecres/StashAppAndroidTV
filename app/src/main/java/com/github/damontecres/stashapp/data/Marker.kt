package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.util.titleOrFilename
import kotlinx.parcelize.Parcelize

@Parcelize
data class Marker(
    val id: String,
    val title: String?,
    val tagIds: List<String>,
    val primaryTagId: String,
    val primaryTagName: String,
    val screenshot: String,
    val seconds: Double,
    val sceneId: String,
    val sceneTitle: String?,
) : Parcelable {
    constructor(
        markerData: MarkerData,
    ) : this(
        markerData.id,
        markerData.title,
        markerData.tags.map { it.slimTagData.id },
        markerData.primary_tag.slimTagData.id,
        markerData.primary_tag.slimTagData.name,
        markerData.screenshot,
        markerData.seconds,
        markerData.scene.videoSceneData.id,
        markerData.scene.videoSceneData.titleOrFilename,
    )
}
