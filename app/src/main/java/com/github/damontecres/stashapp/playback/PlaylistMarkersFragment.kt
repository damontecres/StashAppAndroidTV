package com.github.damontecres.stashapp.playback

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.github.damontecres.stashapp.api.CountMarkersQuery
import com.github.damontecres.stashapp.api.FindMarkersQuery
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import kotlin.properties.Delegates

/**
 * A [PlaylistFragment] that plays [MarkerData] videos
 */
@OptIn(UnstableApi::class)
class PlaylistMarkersFragment :
    PlaylistFragment<FindMarkersQuery.Data, MarkerData, CountMarkersQuery.Data>() {
    private var duration by Delegates.notNull<Long>()

    override val optionsButtonOptions: OptionsButtonOptions
        get() = OptionsButtonOptions(DataType.MARKER, true)

    override val previewsEnabled: Boolean
        get() = false

    override fun builderCallback(item: MarkerData): (MediaItem.Builder.() -> Unit) {
        return {
            // Clip the media item to a start position & duration
            val startPos = (item.seconds * 1000).toLong()
            val clipConfig =
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startPos)
                    .setEndPositionMs(startPos + duration)
                    .build()
            setClippingConfiguration(clipConfig)
        }
    }

    override fun convertToScene(item: MarkerData): Scene {
        return Scene.fromVideoSceneData(item.scene.videoSceneData)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        duration = requireActivity().intent.getLongExtra(INTENT_DURATION_ID, 15_000L)
        // Override the skip forward/back since many users will have default seeking values larger than the duration
        super.skipForwardOverride = duration / 4
        super.skipBackOverride = duration / 4
    }

    companion object {
        const val TAG = "PlaylistMarkersFragment"
        const val INTENT_DURATION_ID = "$TAG.duration"
    }
}
