package com.github.damontecres.stashapp.navigation

import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.suppliers.FilterArgs
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    @Serializable
    data object Main : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object Search : Destination

    @Serializable
    data object Pin : Destination

    @Serializable
    data class Item(
        val dataType: DataType,
        val id: String,
    ) : Destination

    @Serializable
    data class Playback(
        val sceneId: String,
        val position: Long,
    ) : Destination

    @Serializable
    data class Filter(
        val filterArgs: FilterArgs,
        val scrollToNextPage: Boolean,
    ) : Destination

    @Serializable
    data class Playlist(
        val filterArgs: FilterArgs,
        val position: Int,
    ) : Destination

    companion object {
        fun fromStashData(item: StashData): Item = Item(getDataType(item), item.id)

        fun getDataType(item: StashData): DataType =
            when (item) {
                is SlimSceneData, is FullSceneData, is VideoSceneData -> DataType.SCENE
                is PerformerData -> DataType.PERFORMER
                else -> TODO()
            }
    }
}
