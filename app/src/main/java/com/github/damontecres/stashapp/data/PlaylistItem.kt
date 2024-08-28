package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Represents an item in a playlist/queue
 */
data class PlaylistItem(
    val index: Int,
    val title: CharSequence?,
    val subtitle: CharSequence?,
    val details1: CharSequence?,
    val details2: CharSequence?,
    val imageUrl: String?,
)

fun MarkerData.toPlayListItem(index: Int): PlaylistItem {
    val name =
        title.ifBlank {
            primary_tag.tagData.name
        }
    val details =
        buildList {
            add(primary_tag.tagData.name)
            addAll(tags.map { it.tagData.name })
        }.joinNotNullOrBlank(", ")
    return PlaylistItem(
        index,
        title = "$name - ${seconds.toInt().toDuration(DurationUnit.SECONDS)}",
        subtitle = scene.videoSceneData.titleOrFilename,
        details1 = details,
        details2 = scene.videoSceneData.date,
        imageUrl = screenshot,
    )
}

fun SlimSceneData.toPlayListItem(index: Int): PlaylistItem {
    return PlaylistItem(
        index,
        title = titleOrFilename,
        subtitle = studio?.name,
        details1 = performers.map { it.name }.joinNotNullOrBlank(", "),
        details2 = date,
        imageUrl = paths.screenshot,
    )
}
