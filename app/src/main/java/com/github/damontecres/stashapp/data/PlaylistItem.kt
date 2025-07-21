package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.formatDate
import com.github.damontecres.stashapp.views.formatSeconds

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
            primary_tag.slimTagData.name
        }
    val details =
        buildList {
            if (title.isNotBlank()) add(primary_tag.slimTagData.name)
            addAll(tags.map { it.slimTagData.name })
        }.joinNotNullOrBlank(", ")
    return PlaylistItem(
        index,
        title = name,
        subtitle = scene.minimalSceneData.titleOrFilename,
        details1 = formatSeconds,
        details2 = details,
        imageUrl = screenshot,
    )
}

fun SlimSceneData.toPlayListItem(index: Int): PlaylistItem =
    PlaylistItem(
        index,
        title = titleOrFilename,
        subtitle = studio?.name,
        details1 = performers.map { it.name }.joinNotNullOrBlank(", "),
        details2 = formatDate(date),
        imageUrl = paths.screenshot,
    )
