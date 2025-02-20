package com.github.damontecres.stashapp.ui.components.playback

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.MainTheme

interface PlayerControls {
    fun seekBack()

    fun seekForward()

    fun seekToPrevious()

    fun seekToNext()

    fun pause()

    fun play()
}

class ExoPlayerControls(
    private val player: Player,
) : PlayerControls {
    override fun seekBack() {
        player.seekBack()
    }

    override fun seekForward() {
        player.seekForward()
    }

    override fun seekToPrevious() {
        player.seekToPrevious()
    }

    override fun seekToNext() {
        player.seekToNext()
    }

    override fun pause() {
        player.pause()
    }

    override fun play() {
        player.play()
    }
}

@Preview(uiMode = Configuration.UI_MODE_TYPE_TELEVISION, widthDp = 800)
@Composable
private fun PlaybackControlsPreview() {
    MainTheme {
        val focusRequester = remember { FocusRequester() }
        PlaybackControls(
            player =
                object : PlayerControls {
                    override fun seekBack() {
                    }

                    override fun seekForward() {
                    }

                    override fun seekToPrevious() {
                    }

                    override fun seekToNext() {
                    }

                    override fun pause() {
                    }

                    override fun play() {
                    }
                },
            playbackState = PlaybackState(true, true, false),
            modifier = Modifier.background(Color.Magenta),
            initialFocusRequester = focusRequester,
            controllerViewState = ControllerViewState(4),
        )
    }
}

@Composable
fun PlaybackControls(
    player: PlayerControls,
    playbackState: PlaybackState,
    controllerViewState: ControllerViewState,
    modifier: Modifier = Modifier,
    initialFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    LaunchedEffect(controllerViewState.controlsVisible) {
        if (controllerViewState.controlsVisible) {
            initialFocusRequester.requestFocus()
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SeekBar(player, playbackState, Modifier)
        PlaybackButtons(
            player,
            playbackState,
            initialFocusRequester,
            onControllerInteraction = { controllerViewState.showControls() },
            modifier = Modifier,
        )
    }
}

@Composable
fun SeekBar(
    player: PlayerControls,
    playbackState: PlaybackState,
    modifier: Modifier = Modifier,
) {
}

@Composable
fun PlaybackButtons(
    player: PlayerControls,
    playbackState: PlaybackState,
    initialFocusRequester: FocusRequester,
    onControllerInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PlaybackButton(
            iconRes = R.drawable.baseline_skip_previous_24,
            onClick = { player.seekToPrevious() },
            enabled = playbackState.hasPrevious,
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            iconRes = R.drawable.baseline_fast_rewind_24,
            onClick = { player.seekBack() },
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            modifier = Modifier.focusRequester(initialFocusRequester),
            iconRes = if (playbackState.isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24,
            onClick = { if (playbackState.isPlaying) player.pause() else player.play() },
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            iconRes = R.drawable.baseline_fast_forward_24,
            onClick = { player.seekForward() },
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            iconRes = R.drawable.baseline_skip_next_24,
            onClick = { player.seekToNext() },
            enabled = playbackState.hasNext,
            onControllerInteraction = onControllerInteraction,
        )
    }
}

@Composable
fun PlaybackButton(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    onControllerInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var color by remember { mutableStateOf(AppColors.TransparentBlack25) }
    val selectedColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier =
            modifier
                .padding(8.dp)
                .size(50.dp, 50.dp)
                .clickable(onClick = {
                    onControllerInteraction.invoke()
                    onClick.invoke()
                })
                .onFocusChanged {
                    color =
                        if (it.isFocused) {
                            onControllerInteraction.invoke()
                            selectedColor
                        } else {
                            AppColors.TransparentBlack25
                        }
                }.focusable(),
        shape = CircleShape,
        colors = SurfaceDefaults.colors(containerColor = color, contentColor = color),
    ) {
        Icon(
            modifier =
                Modifier
                    .fillMaxSize(.9f)
                    .align(Alignment.Center),
            painter = painterResource(iconRes),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
