package com.github.damontecres.stashapp.playback

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.github.damontecres.stashapp.api.CountScenesQuery
import com.github.damontecres.stashapp.api.FindVideoScenesQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene

/**
 * A [PlaylistFragment] that plays [SlimSceneData] videos
 */
@OptIn(UnstableApi::class)
class PlaylistScenesFragment : PlaylistFragment<FindVideoScenesQuery.Data, VideoSceneData, CountScenesQuery.Data>() {
    override val previewsEnabled: Boolean
        get() = true

    override val optionsButtonOptions: OptionsButtonOptions
        get() = OptionsButtonOptions(DataType.SCENE, true)

    override fun builderCallback(item: VideoSceneData): (MediaItem.Builder.() -> Unit)? = null

    override fun convertToScene(item: VideoSceneData): Scene = Scene.fromVideoSceneData(item)

    companion object {
        private const val TAG = "PlaylistScenesFragment"
    }
}
