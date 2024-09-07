package com.github.damontecres.stashapp.playback

import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.github.damontecres.stashapp.api.CountScenesQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.animateToInvisible
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.views.showSimpleListPopupWindow

/**
 * A [PlaylistFragment] that plays [SlimSceneData] videos
 */
@OptIn(UnstableApi::class)
class PlaylistScenesFragment :
    PlaylistFragment<FindScenesQuery.Data, SlimSceneData, CountScenesQuery.Data>() {
    override val previewsEnabled: Boolean
        get() = true

    override val optionsButtonOptions: OptionsButtonOptions
        get() = OptionsButtonOptions(DataType.SCENE, true)

    override fun builderCallback(item: SlimSceneData): (MediaItem.Builder.() -> Unit)? {
        return null
    }

    override fun convertToScene(item: SlimSceneData): Scene {
        return Scene.fromSlimSceneData(item)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // TODO add more options like regular scene playback
        moreOptionsButton.setOnClickListener {
            val debugToggleText =
                if (debugView.isVisible) "Hide transcode info" else "Show transcode info"
            showSimpleListPopupWindow(
                moreOptionsButton,
                listOf("Show playlist", debugToggleText),
            ) { position ->
                if (position == 0) {
                    showPlaylist()
                } else if (position == 1) {
                    if (debugView.isVisible) {
                        debugView.animateToInvisible(View.GONE)
                    } else {
                        debugView.animateToVisible()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "PlaylistScenesFragment"
    }
}
