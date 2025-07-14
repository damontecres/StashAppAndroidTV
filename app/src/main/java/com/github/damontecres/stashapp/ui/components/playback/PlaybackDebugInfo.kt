package com.github.damontecres.stashapp.ui.components.playback

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.StreamDecision
import com.github.damontecres.stashapp.playback.TranscodeDecision
import com.github.damontecres.stashapp.ui.components.TableRow
import com.github.damontecres.stashapp.ui.components.TableRowComposable

@Composable
fun PlaybackDebugInfo(
    scene: Scene,
    streamDecision: StreamDecision,
    playlistInfo: PlaylistInfo?,
    modifier: Modifier = Modifier,
) {
    val transcodeText =
        when (streamDecision.transcodeDecision) {
            TranscodeDecision.Transcode -> stringResource(R.string.transcode)

            is TranscodeDecision.ForcedTranscode ->
                stringResource(
                    R.string.force_transcode,
                ) + " " + streamDecision.transcodeDecision.streamLabel

            TranscodeDecision.DirectPlay -> stringResource(R.string.direct)

            TranscodeDecision.ForcedDirectPlay -> stringResource(R.string.force_direct)
        }
    val videoText = if (streamDecision.videoSupported) scene.videoCodec else "${scene.videoCodec} (unsupported)"
    val audioText = if (streamDecision.audioSupported) scene.audioCodec else "${scene.audioCodec} (unsupported)"
    val formatText = if (streamDecision.containerSupported) scene.format else "${scene.format} (unsupported)"
    val rows =
        listOfNotNull(
            TableRow.from(R.string.stashapp_scene_id, scene.id),
            TableRow.from(R.string.playback, transcodeText),
            TableRow.from(R.string.stashapp_video_codec, videoText),
            TableRow.from(R.string.stashapp_audio_codec, audioText),
            TableRow.from(R.string.format, formatText),
            playlistInfo?.let { TableRow.from("Playlist", it.readable) },
        )
    LazyColumn(
        modifier = modifier,
    ) {
        items(rows) { row ->
            TableRowComposable(
                row,
                keyWeight = 1f,
                valueWeight = 1f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

data class PlaylistInfo(
    val position: Int,
    val totalCount: Int,
    val loadedCount: Int,
) {
    val readable: String = "${position + 1} of $totalCount ($loadedCount)"
}
