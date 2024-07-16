package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.MarkerData
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
) : Parcelable {
    constructor(
        markerData: MarkerData,
    ) : this(
        markerData.id,
        markerData.title,
        markerData.tags.map { it.tagData.id },
        markerData.primary_tag.tagData.id,
        markerData.primary_tag.tagData.name,
        markerData.screenshot,
        markerData.seconds,
        markerData.scene.videoSceneData.id,
    )
}
