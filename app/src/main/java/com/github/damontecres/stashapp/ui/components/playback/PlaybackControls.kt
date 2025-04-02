package com.github.damontecres.stashapp.ui.components.playback

import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberNextButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPreviousButtonState
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.ui.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

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

// @Preview(uiMode = Configuration.UI_MODE_TYPE_TELEVISION, widthDp = 800)
// @Composable
// private fun PlaybackControlsPreview() {
//    MainTheme {
//        val focusRequester = remember { FocusRequester() }
//        PlaybackControls(
//            player =
//                object : Player {
//                    override fun seekBack() {
//                    }
//
//                    override fun seekForward() {
//                    }
//
//                    override fun seekToPrevious() {
//                    }
//
//                    override fun seekToNext() {
//                    }
//
//                    override fun pause() {
//                    }
//
//                    override fun play() {
//                    }
//                },
//            playbackState = PlaybackState(true, true, false),
//            modifier = Modifier.background(Color.DarkGray),
//            initialFocusRequester = focusRequester,
//            controllerViewState = ControllerViewState(4),
//        )
//    }
// }

@Composable
fun PlaybackControls(
    scene: Scene,
    player: Player,
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
        SeekBar(
            scene,
            player,
            controllerViewState,
            Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(.95f),
        )
        PlaybackButtons(
            player,
            initialFocusRequester,
            onControllerInteraction = { controllerViewState.showControls() },
            modifier = Modifier,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SeekBar(
    scene: Scene,
    player: Player,
    controllerViewState: ControllerViewState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val state = rememberSeekBarState(player, scope)
    var bufferedProgress by remember(player) { mutableFloatStateOf(player.bufferedPosition.toFloat() / player.duration) }
    var position by remember(player) { mutableLongStateOf(player.currentPosition) }
    var progress by remember(player) { mutableFloatStateOf(player.currentPosition.toFloat() / player.duration) }
    LaunchedEffect(player) {
        while (isActive) {
            bufferedProgress = player.bufferedPosition.toFloat() / player.duration
            position = player.currentPosition
            progress = player.currentPosition.toFloat() / player.duration
            delay(250L)
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val aspectRatio =
            if (scene.videoWidth != null && scene.videoHeight != null) scene.videoWidth.toFloat() / scene.videoHeight else 16f / 9
        SeekBarImpl(
            progress = progress,
            bufferedProgress = bufferedProgress,
            duration = player.duration,
            onSeek = state::onValueChange,
            controllerViewState = controllerViewState,
            intervals = 15,
            aspectRatio = aspectRatio,
            previewImageUrl = scene.spriteUrl,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = (position / 1000).seconds.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                modifier =
                    Modifier
                        .padding(8.dp),
            )
            Text(
                text = "-" + ((player.duration - position) / 1000).seconds.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                modifier =
                    Modifier
                        .padding(8.dp),
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlaybackButtons(
    player: Player,
    initialFocusRequester: FocusRequester,
    onControllerInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playPauseState = rememberPlayPauseButtonState(player)
    val previousState = rememberPreviousButtonState(player)
    val nextState = rememberNextButtonState(player)

    Row(
        modifier = modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PlaybackButton(
            iconRes = R.drawable.baseline_skip_previous_24,
            onClick = previousState::onClick,
            enabled = previousState.isEnabled,
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            iconRes = R.drawable.baseline_fast_rewind_24,
            onClick = { player.seekBack() },
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            modifier = Modifier.focusRequester(initialFocusRequester),
            iconRes = if (playPauseState.showPlay) R.drawable.baseline_play_arrow_24 else R.drawable.baseline_pause_24,
            onClick = playPauseState::onClick,
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            iconRes = R.drawable.baseline_fast_forward_24,
            onClick = { player.seekForward() },
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            iconRes = R.drawable.baseline_skip_next_24,
            onClick = nextState::onClick,
            enabled = nextState.isEnabled && player.hasNextMediaItem(),
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
    val selectedColor = MaterialTheme.colorScheme.border
    Button(
        onClick = onClick,
        shape = ButtonDefaults.shape(CircleShape),
        colors =
            ButtonDefaults.colors(
                containerColor = AppColors.TransparentBlack25,
                focusedContainerColor = selectedColor,
            ),
        contentPadding = PaddingValues(8.dp),
        modifier =
            modifier
                .padding(8.dp)
                .size(56.dp, 56.dp),
    ) {
        Icon(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(iconRes),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
