package com.github.damontecres.stashapp.filter

import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString

fun extractTitle(item: StashData): String? {
    return when (item) {
        is TagData -> item.name
        is PerformerData -> item.name
        is StudioData -> item.name
        is GalleryData -> item.title
        is ImageData -> item.title
        is MarkerData -> item.title
        is MovieData -> item.name
        is SlimSceneData -> item.titleOrFilename
        is FullSceneData -> item.titleOrFilename
        else -> throw IllegalArgumentException("${item::class.qualifiedName} not supported")
    }
}

fun extractDescription(item: StashData): String? {
    return when (item) {
        is TagData -> item.description?.ifBlank { null }
        is PerformerData -> item.disambiguation
        is StudioData -> null
        is GalleryData -> item.date
        is ImageData -> item.date
        is MarkerData -> "${item.scene.videoSceneData.titleOrFilename} (${durationToString(item.seconds)})"
        is MovieData -> item.date
        is SlimSceneData -> item.date
        is FullSceneData -> item.date
        else -> throw IllegalArgumentException("${item::class.qualifiedName} not supported")
    }
}
