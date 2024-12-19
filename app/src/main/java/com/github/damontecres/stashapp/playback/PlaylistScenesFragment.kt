package com.github.damontecres.stashapp.playback

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.github.damontecres.stashapp.api.CountScenesQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene

/**
 * A [PlaylistFragment] that plays [SlimSceneData] videos
 */
@OptIn(UnstableApi::class)
class PlaylistScenesFragment : PlaylistFragment<FindScenesQuery.Data, SlimSceneData, CountScenesQuery.Data>() {
    override val previewsEnabled: Boolean
        get() = true

    override val optionsButtonOptions: OptionsButtonOptions
        get() = OptionsButtonOptions(DataType.SCENE, true)

    override fun builderCallback(item: SlimSceneData): (MediaItem.Builder.() -> Unit)? = null

    override fun convertToScene(item: SlimSceneData): Scene = Scene.fromSlimSceneData(item)

    companion object {
        private const val TAG = "PlaylistScenesFragment"
    }
}
