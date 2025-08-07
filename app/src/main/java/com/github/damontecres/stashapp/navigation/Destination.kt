package com.github.damontecres.stashapp.navigation

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.github.damontecres.stashapp.PreferenceScreenOption
import com.github.damontecres.stashapp.api.fragment.ExtraImageData
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.GroupRelationshipData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MinimalSceneData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimImageData
import com.github.damontecres.stashapp.api.fragment.SlimPerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.SlimTagData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.Release
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.putDestination
import com.github.damontecres.stashapp.util.toLongMilliseconds
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

/**
 * Represents a "page" of the app that can be navigated to
 */
@Serializable
sealed class Destination(
    val fullScreen: Boolean = false,
) : Parcelable {
    protected val destId = counter.getAndIncrement()

    val fragmentTag = "${this::class.simpleName}_$destId"

    @Serializable
    data object Setup : Destination(true)

    @Serializable
    data object Main : Destination()

    @Serializable
    data class Settings(
        val screenOption: PreferenceScreenOption,
    ) : Destination(true)

    @Serializable
    data object Search : Destination()

    @Serializable
    data object Pin : Destination(true)

    @Serializable
    data object SettingsPin : Destination(true)

    @Serializable
    data class Item(
        val dataType: DataType,
        val id: String,
    ) : Destination()

    @Serializable
    data class MarkerDetails(
        val markerId: String,
    ) : Destination()

    @Serializable
    data class Playback(
        val sceneId: String,
        val position: Long,
        val mode: PlaybackMode,
    ) : Destination(true)

    @Serializable
    data class Slideshow(
        val filterArgs: FilterArgs,
        val position: Int,
        val automatic: Boolean,
    ) : Destination(true) {
        override fun toString(): String = "Slideshow(destId=$destId, position=$position, automatic=$automatic)"
    }

    @Serializable
    data class Filter(
        val filterArgs: FilterArgs,
        val scrollToNextPage: Boolean = false,
    ) : Destination() {
        override fun toString(): String = "Filter(destId=$destId, dataType=${filterArgs.dataType}, scrollToNextPage=$scrollToNextPage)"
    }

    @Serializable
    data class Playlist(
        val filterArgs: FilterArgs,
        val position: Int,
        val duration: Long? = null,
    ) : Destination(true) {
        override fun toString(): String =
            "Playlist(destId=$destId, dataType=${filterArgs.dataType}, position=$position, duration=$duration)"
    }

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
    ) : Destination(true)

    @Serializable
    data class UpdateApp(
        val release: Release,
    ) : Destination(true) {
        override fun toString(): String = "UpdateApp(version=${release.version})"
    }

    @Serializable
    data class ManageServers(
        val overrideReadOnly: Boolean,
    ) : Destination(true)

    @Serializable
    data class CreateFilter(
        val dataType: DataType,
        val startingFilter: FilterArgs?,
    ) : Destination(true) {
        override fun toString(): String = "CreateFilter(destId=$destId, dataType=$dataType)"
    }

    @Serializable
    data object ChooseTheme : Destination(true)

    @Serializable
    data object Debug : Destination(true)

    /**
     * An arbitrary fragment that requires no arguments
     */
    @Serializable
    data class Fragment(
        val className: String,
    ) : Destination(true)

    override fun describeContents(): Int = 0

    override fun writeToParcel(
        out: Parcel,
        flags: Int,
    ) {
        val bundle = Bundle()
        bundle.putDestination(this)
        out.writeBundle(bundle)
    }

    companion object {
        private val counter = AtomicLong(0)

        @JvmField
        val CREATOR =
            object : Creator<Destination?> {
                override fun createFromParcel(`in`: Parcel): Destination? =
                    `in`
                        .readBundle(Destination::class.java.getClassLoader())
                        ?.getDestination<Destination>()

                override fun newArray(size: Int): Array<Destination?> = arrayOfNulls(size)
            }

        fun fromStashData(item: StashData): Destination =
            when (val dataType = getDataType(item)) {
                DataType.MARKER -> {
                    // Clicking a marker should start playback
                    when (item) {
                        is MarkerData -> {
                            Playback(
                                item.scene.minimalSceneData.id,
                                item.seconds.toLongMilliseconds,
                                PlaybackMode.Choose,
                            )
                        }

                        is FullMarkerData -> {
                            Playback(
                                item.scene.videoSceneData.id,
                                item.seconds.toLongMilliseconds,
                                PlaybackMode.Choose,
                            )
                        }

                        else -> {
                            throw IllegalStateException("Unknown marker type: ${item::class.qualifiedName}")
                        }
                    }
                }

                DataType.IMAGE -> {
                    // Showing an image requires a filter
                    throw IllegalArgumentException("Image not supported")
                }

                else -> Item(dataType, item.id)
            }

        fun getDataType(item: StashData): DataType =
            when (item) {
                is SlimSceneData, is FullSceneData, is VideoSceneData, is MinimalSceneData -> DataType.SCENE
                is PerformerData, is SlimPerformerData -> DataType.PERFORMER
                is TagData, is SlimTagData -> DataType.TAG
                is GroupData, is GroupRelationshipData -> DataType.GROUP
                is ImageData, is ExtraImageData, is SlimImageData -> DataType.IMAGE
                is GalleryData -> DataType.GALLERY
                is StudioData -> DataType.STUDIO
                is MarkerData, is FullMarkerData -> DataType.MARKER
            }
    }
}
