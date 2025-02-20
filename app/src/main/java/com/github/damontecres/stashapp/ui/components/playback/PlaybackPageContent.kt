package com.github.damontecres.stashapp.ui.components.playback

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.playback.buildMediaItem
import com.github.damontecres.stashapp.playback.getStreamDecision
import com.github.damontecres.stashapp.util.StashServer

data class PlaybackState(
    val isPlaying: Boolean,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
)

@Composable
fun PlaybackPageContent(
    server: StashServer,
    scene: Scene,
    playbackMode: PlaybackMode,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player =
        remember {
            StashExoPlayer.getInstance(context, server).apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
        }

    var contentCurrentPosition by remember { mutableLongStateOf(0L) }
    var playbackState by remember { mutableStateOf(PlaybackState(false, false, false)) }

    LaunchedEffect(server, scene, player) {
        StashExoPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playbackState = playbackState.copy(isPlaying = isPlaying)
                }

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    playbackState =
                        playbackState.copy(
                            hasPrevious = player.hasPreviousMediaItem(),
                            hasNext = player.hasNextMediaItem(),
                        )
                }
            },
        )

        val streamDecision = getStreamDecision(context, scene, playbackMode)
        val media = buildMediaItem(context, streamDecision, scene)
        player.setMediaItem(media)
        player.prepare()
    }

    val controllerViewState =
        remember { ControllerViewState(3) }.also {
            LaunchedEffect(it) {
                it.observe()
            }
        }

    Box(
        modifier
            .background(Color.Black)
            .onKeyEvent {
                if (!controllerViewState.controlsVisible) {
                    if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP &&
                        it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
                    ) {
                        controllerViewState.showControls()
                    }
                    true
                } else {
                    false
                }
            }.focusable(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(context).apply {
                    useController = false
                }
            },
            update = { it.player = player },
            onRelease = { player.release() },
        )

        PlaybackOverlay(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
            server = server,
            scene = scene,
            player = player,
            playbackState = playbackState,
            controllerViewState = controllerViewState,
        )
    }
}
