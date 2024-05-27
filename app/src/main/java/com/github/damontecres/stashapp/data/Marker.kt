package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.MarkerData
import kotlinx.parcelize.Parcelize

@Parcelize
data class Marker(
    val id: String,
    val title: String?,
    val tagIds: List<String>,
    val primaryTagName: String,
    val screenshot: String,
    val scene: Scene,
    val seconds: Double,
) : Parcelable {
    constructor(
        markerData: MarkerData,
    ) : this(
        markerData.id,
        markerData.title,
        listOf(markerData.primary_tag.tagData.id, *markerData.tags.map { it.tagData.id }.toTypedArray()),
        markerData.primary_tag.tagData.name,
        markerData.screenshot,
        Scene.fromSlimSceneData(markerData.scene.slimSceneData),
        markerData.seconds,
    )
}
