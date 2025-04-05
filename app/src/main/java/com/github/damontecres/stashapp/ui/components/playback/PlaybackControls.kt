package com.github.damontecres.stashapp.ui.components.playback

import android.util.Log
import android.view.Gravity
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberNextButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPreviousButtonState
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.ui.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

sealed interface PlaybackAction {
    data object OCount : PlaybackAction

    data object ShowDebug : PlaybackAction

    data object CreateMarker : PlaybackAction
}

@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaybackControls(
    scene: FullSceneData,
    oCounter: Int,
    player: Player,
    controllerViewState: ControllerViewState,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    showDebugInfo: Boolean,
    onSeekProgress: (Float) -> Unit,
    modifier: Modifier = Modifier,
    initialFocusRequester: FocusRequester = remember { FocusRequester() },
    seekBarInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val onControllerInteraction = {
        scope.launch {
            bringIntoViewRequester.bringIntoView()
        }
        controllerViewState.pulseControls()
    }
    val playbackScene = remember(scene.id) { Scene.fromFullSceneData(scene) }
    LaunchedEffect(controllerViewState.controlsVisible) {
        if (controllerViewState.controlsVisible) {
            initialFocusRequester.requestFocus()
        }
    }
    Column(
        modifier = modifier.bringIntoViewRequester(bringIntoViewRequester),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SeekBar(
            scene = playbackScene,
            player = player,
            controllerViewState = controllerViewState,
            onSeekProgress = onSeekProgress,
            interactionSource = seekBarInteractionSource,
            modifier =
                Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(.95f),
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
        ) {
            LeftPlaybackButtons(
                onControllerInteraction = onControllerInteraction,
                onPlaybackActionClick = onPlaybackActionClick,
                showDebugInfo = showDebugInfo,
                modifier = Modifier,
            )
            PlaybackButtons(
                player,
                initialFocusRequester,
                onControllerInteraction = onControllerInteraction,
                modifier = Modifier,
            )
            RightPlaybackButtons(
                onControllerInteraction = onControllerInteraction,
                onPlaybackActionClick = onPlaybackActionClick,
                modifier = Modifier,
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SeekBar(
    scene: Scene,
    player: Player,
    controllerViewState: ControllerViewState,
    onSeekProgress: (Float) -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
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
            onSeek = {
                onSeekProgress(it)
                state.onValueChange(it)
            },
            controllerViewState = controllerViewState,
            intervals = 15,
            aspectRatio = aspectRatio,
            previewImageUrl = scene.spriteUrl,
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
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

private val buttonSpacing = 4.dp

@Composable
fun LeftPlaybackButtons(
    onControllerInteraction: () -> Unit,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    showDebugInfo: Boolean,
    modifier: Modifier = Modifier,
) {
    var showMoreOptions by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
    ) {
        // More options
        PlaybackButton(
            iconRes = R.drawable.vector_settings,
            onClick = { showMoreOptions = true },
            enabled = true,
            onControllerInteraction = onControllerInteraction,
        )
        // OCount
        PlaybackButton(
            iconRes = R.drawable.sweat_drops,
            onClick = { onPlaybackActionClick.invoke(PlaybackAction.OCount) },
            enabled = true,
            onControllerInteraction = onControllerInteraction,
        )
    }
    if (showMoreOptions) {
        val options =
            listOf(
                if (showDebugInfo) "Hide transcode info" else "Show transcode info",
                "Create Marker",
            )
        BottomDialog(
            choices = options,
            onDismissRequest = { showMoreOptions = false },
            onSelectChoice = {
                when (options.indexOf(it)) {
                    0 -> onPlaybackActionClick.invoke(PlaybackAction.ShowDebug)
                    1 -> onPlaybackActionClick.invoke(PlaybackAction.CreateMarker)
                    else -> Log.w(TAG, "Unknown more option: $it")
                }
            },
            gravity = Gravity.START,
        )
    }
}

@Composable
fun RightPlaybackButtons(
    onControllerInteraction: () -> Unit,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
    ) {
        // Captions
        PlaybackButton(
            iconRes = R.drawable.baseline_more_vert_96,
            onClick = { },
            enabled = true,
            onControllerInteraction = onControllerInteraction,
        )
        // Playback speed, etc
        PlaybackButton(
            iconRes = R.drawable.vector_settings,
            onClick = {},
            enabled = true,
            onControllerInteraction = onControllerInteraction,
        )
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
        horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
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
                .size(56.dp, 56.dp)
                .onFocusChanged { onControllerInteraction.invoke() },
    ) {
        Icon(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(iconRes),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BottomDialog(
    choices: List<String>,
    onDismissRequest: () -> Unit,
    onSelectChoice: (String) -> Unit,
    gravity: Int,
    currentChoice: String? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        dialogWindowProvider?.window?.let { window ->
            window.setGravity(Gravity.BOTTOM or gravity) // Move down, by default dialogs are in the centre
            window.setDimAmount(0f) // Remove dimmed background of ongoing playback
        }

        Box(
            modifier =
                Modifier
                    .wrapContentSize()
                    .padding(8.dp)
                    .background(Color.DarkGray),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                choices.forEach { choice ->
                    ListItem(
                        selected = false,
                        onClick = {
                            onDismissRequest()
                            onSelectChoice(choice)
                        },
                        headlineContent = {
                            var fontWeight = FontWeight(400)
                            if (choice == currentChoice) {
                                fontWeight = FontWeight(1000)
                            }
                            Text(
                                text = choice,
                                fontWeight = fontWeight,
                            )
                        },
                    )
                }
            }
        }
    }
}
