package com.github.damontecres.stashapp.navigation

import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimPerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.SlimTagData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.Release
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

/**
 * Represents a "page" of the app that can be navigated to
 */
@Serializable
sealed class Destination {
    private val destId = counter.getAndIncrement()

    val fragmentTag = "${this::class.simpleName}_$destId"

    @Serializable
    data object Setup : Destination()

    @Serializable
    data object Main : Destination()

    @Serializable
    data object Settings : Destination()

    @Serializable
    data object Search : Destination()

    @Serializable
    data object Pin : Destination()

    @Serializable
    data object SettingsPin : Destination()

    @Serializable
    data class Item(
        val dataType: DataType,
        val id: String,
    ) : Destination()

    @Serializable
    data class MarkerDetails(
        val id: String,
        val sceneId: String,
    ) : Destination()

    @Serializable
    data class Playback(
        val sceneId: String,
        val position: Long,
        val mode: PlaybackMode,
    ) : Destination()

    @Serializable
    data class Slideshow(
        val filterArgs: FilterArgs,
        val position: Int,
        val automatic: Boolean,
    ) : Destination()

    @Serializable
    data class Filter(
        val filterArgs: FilterArgs,
        val scrollToNextPage: Boolean = false,
    ) : Destination()

    @Serializable
    data class Playlist(
        val filterArgs: FilterArgs,
        val position: Int,
        val duration: Long? = null,
    ) : Destination()

    @Serializable
    data class SearchFor(
        val requestKey: String,
        val sourceId: Long,
        val dataType: DataType,
        val title: String? = null,
    ) : Destination()

    @Serializable
    data class UpdateMarker(
        val markerId: String,
        val sceneId: String,
    ) : Destination()

    @Serializable
    data class UpdateApp(
        val release: Release,
    ) : Destination()

    @Serializable
    data class ManageServers(
        val overrideReadOnly: Boolean,
    ) : Destination()

    @Serializable
    data class CreateFilter(
        val dataType: DataType,
        val startingFilter: FilterArgs?,
    ) : Destination()

    /**
     * An arbitrary fragment that requires no arguments
     */
    @Serializable
    data class Fragment(
        val className: String,
    ) : Destination()

    companion object {
        private val counter = AtomicLong(0)

        fun fromStashData(item: StashData): Item = Item(getDataType(item), item.id)

        fun getDataType(item: StashData): DataType =
            when (item) {
                is SlimSceneData, is FullSceneData, is VideoSceneData -> DataType.SCENE
                is PerformerData, is SlimPerformerData -> DataType.PERFORMER
                is TagData, is SlimTagData -> DataType.TAG
                is GroupData -> DataType.GROUP
                is ImageData -> DataType.IMAGE
                is GalleryData -> DataType.GALLERY
                is StudioData -> DataType.STUDIO
                is MarkerData -> DataType.MARKER
                else -> throw IllegalArgumentException("Class not mapped: ${item::class}")
            }
    }
}
