package com.github.damontecres.stashapp.ui.components.playback

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.playback.buildMediaItem
import com.github.damontecres.stashapp.playback.getStreamDecision
import com.github.damontecres.stashapp.util.StashServer

private const val TAG = "PlaybackPageContent"

@OptIn(UnstableApi::class)
@Composable
fun PlaybackPageContent(
    server: StashServer,
    scene: Scene,
    startPosition: Long,
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

    var showControls by remember { mutableStateOf(true) }
    var currentContentScaleIndex by remember { mutableIntStateOf(0) }
    val contentScale = ContentScale.Fit

    val presentationState = rememberPresentationState(player)
    val scaledModifier =
        Modifier.resizeWithContentScale(contentScale, presentationState.videoSizeDp)

    var contentCurrentPosition by remember { mutableLongStateOf(0L) }

    LaunchedEffect(server, scene, player) {
        val streamDecision = getStreamDecision(context, scene, playbackMode)
        val media = buildMediaItem(context, streamDecision, scene)
        player.setMediaItem(media, startPosition.coerceAtLeast(0L))
        player.prepare()
    }

    val controllerViewState =
        remember { ControllerViewState(3) }.also {
            LaunchedEffect(it) {
                it.observe()
            }
        }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Box(
        modifier
            .background(Color.Black)
            .onKeyEvent {
                var result = true
                if (it.type != KeyEventType.KeyUp) {
                    result = false
                } else if (isDpad(it)) {
                    if (!controllerViewState.controlsVisible) {
                        if (it.key == Key.DirectionLeft) {
                            player.seekBack()
                        } else if (it.key == Key.DirectionRight) {
                            player.seekForward()
                        } else {
                            controllerViewState.showControls()
                        }
                    } else {
                        controllerViewState.pulseControls()
                    }
                } else if (isMedia(it)) {
                    when (it.key) {
                        Key.MediaPlay -> player.play()
                        Key.MediaPause -> player.pause()
                        Key.MediaPlayPause -> if (player.isPlaying) player.pause() else player.play()
                        Key.MediaFastForward, Key.MediaSkipForward -> player.seekForward()
                        Key.MediaRewind, Key.MediaSkipBackward -> player.seekBack()
                        Key.MediaNext -> player.seekToNext()
                        Key.MediaPrevious -> player.seekToPrevious()
                        else -> result = false
                    }
                } else {
                    controllerViewState.pulseControls()
                    result = false
                }
                result
            }.focusRequester(focusRequester)
            .focusable(),
    ) {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = scaledModifier.clickable(enabled = false) { showControls = !showControls },
        )
        if (presentationState.coverSurface) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black),
            )
        }

        PlaybackOverlay(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
            server = server,
            scene = scene,
            player = player,
            controllerViewState = controllerViewState,
        )
    }
}
