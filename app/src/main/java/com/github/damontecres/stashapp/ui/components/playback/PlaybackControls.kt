package com.github.damontecres.stashapp.ui.components.playback

import android.util.Log
import android.view.Gravity
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.TrackSupport
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

sealed interface PlaybackAction {
    data object OCount : PlaybackAction

    data object ShowDebug : PlaybackAction

    data object CreateMarker : PlaybackAction

    data object ShowPlaylist : PlaybackAction

    data object ShowVideoFilterDialog : PlaybackAction

    data class ToggleCaptions(
        val index: Int,
    ) : PlaybackAction

    data class ToggleAudio(
        val index: Int,
    ) : PlaybackAction

    data class PlaybackSpeed(
        val value: Float,
    ) : PlaybackAction

    data class Scale(
        val scale: ContentScale,
    ) : PlaybackAction
}

@OptIn(UnstableApi::class)
@Composable
fun PlaybackControls(
    scene: Scene,
    captions: List<TrackSupport>,
    oCounter: Int,
    playerControls: PlayerControls,
    controllerViewState: ControllerViewState,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    showDebugInfo: Boolean,
    onSeekProgress: (Float) -> Unit,
    showPlay: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    seekEnabled: Boolean,
    moreButtonOptions: MoreButtonOptions,
    subtitleIndex: Int?,
    audioIndex: Int?,
    audioOptions: List<String>,
    playbackSpeed: Float,
    scale: ContentScale,
    modifier: Modifier = Modifier,
    initialFocusRequester: FocusRequester = remember { FocusRequester() },
    seekBarInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val scope = rememberCoroutineScope()

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val onControllerInteraction = {
        scope.launch(StashCoroutineExceptionHandler()) {
            bringIntoViewRequester.bringIntoView()
        }
        controllerViewState.pulseControls()
    }
    val onControllerInteractionForDialog = {
        scope.launch(StashCoroutineExceptionHandler()) {
            bringIntoViewRequester.bringIntoView()
        }
        controllerViewState.pulseControls(Int.MAX_VALUE)
    }
    LaunchedEffect(controllerViewState.controlsVisible) {
        if (controllerViewState.controlsVisible) {
            initialFocusRequester.tryRequestFocus()
        }
    }
    Column(
        modifier = modifier.bringIntoViewRequester(bringIntoViewRequester),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SeekBar(
            scene = scene,
            player = playerControls,
            controllerViewState = controllerViewState,
            onSeekProgress = onSeekProgress,
            interactionSource = seekBarInteractionSource,
            isEnabled = seekEnabled,
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
                oCount = oCounter,
                moreButtonOptions = moreButtonOptions,
                modifier = Modifier,
            )
            PlaybackButtons(
                player = playerControls,
                initialFocusRequester = initialFocusRequester,
                onControllerInteraction = onControllerInteraction,
                showPlay = showPlay,
                previousEnabled = previousEnabled,
                nextEnabled = nextEnabled,
                modifier = Modifier,
            )
            RightPlaybackButtons(
                modifier = Modifier,
                captions = captions,
                onControllerInteraction = onControllerInteraction,
                onControllerInteractionForDialog = onControllerInteractionForDialog,
                onPlaybackActionClick = onPlaybackActionClick,
                subtitleIndex = subtitleIndex,
                audioOptions = audioOptions,
                audioIndex = audioIndex,
                playbackSpeed = playbackSpeed,
                scale = scale,
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SeekBar(
    scene: Scene,
    player: PlayerControls,
    isEnabled: Boolean,
    controllerViewState: ControllerViewState,
    onSeekProgress: (Float) -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val context = LocalContext.current
    val intervals =
        remember {
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .getInt(context.getString(R.string.pref_key_playback_seek_count), 16)
        }
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
            },
            controllerViewState = controllerViewState,
            intervals = intervals,
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
    oCount: Int,
    moreButtonOptions: MoreButtonOptions,
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
            onClick = {
                onControllerInteraction.invoke()
                showMoreOptions = true
            },
            enabled = true,
            onControllerInteraction = onControllerInteraction,
        )
        // OCount
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaybackButton(
                iconRes = R.drawable.sweat_drops,
                onClick = {
                    onControllerInteraction.invoke()
                    onPlaybackActionClick.invoke(PlaybackAction.OCount)
                },
                enabled = true,
                onControllerInteraction = onControllerInteraction,
            )
            Text(
                text = oCount.toString(),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
            )
        }
    }
    if (showMoreOptions) {
        // TODO options need context about what to display
        val options =
            buildList {
                addAll(moreButtonOptions.options.keys)
                add(if (showDebugInfo) "Hide transcode info" else "Show transcode info")
            }
        BottomDialog(
            choices = options,
            onDismissRequest = { showMoreOptions = false },
            onSelectChoice = { index, choice ->
                val action = moreButtonOptions.options[choice] ?: PlaybackAction.ShowDebug
                onPlaybackActionClick.invoke(action)
            },
            gravity = Gravity.START,
        )
    }
}

private val speedOptions = listOf(".25", ".5", ".75", "1.0", "1.25", "1.5", "2.0")

@Composable
fun RightPlaybackButtons(
    captions: List<TrackSupport>,
    onControllerInteraction: () -> Unit,
    onControllerInteractionForDialog: () -> Unit,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    subtitleIndex: Int?,
    audioOptions: List<String>,
    audioIndex: Int?,
    playbackSpeed: Float,
    scale: ContentScale,
    modifier: Modifier = Modifier,
) {
    var showCaptionDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showScaleDialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
    ) {
        // Captions
        PlaybackButton(
            enabled = captions.isNotEmpty(),
            iconRes = R.drawable.captions_svgrepo_com,
            onClick = {
                onControllerInteractionForDialog.invoke()
                showCaptionDialog = true
            },
            onControllerInteraction = onControllerInteraction,
        )
        // Playback speed, etc
        PlaybackButton(
            iconRes = R.drawable.vector_settings,
            onClick = {
                onControllerInteractionForDialog.invoke()
                showOptionsDialog = true
            },
            enabled = true,
            onControllerInteraction = onControllerInteraction,
        )
    }
    if (showCaptionDialog) {
        val context = LocalContext.current
        val options = captions.map { it.displayString(context) }
        Log.v(TAG, "subtitleIndex=$subtitleIndex, options=$options")
        BottomDialog(
            choices = options,
            currentChoice = if (subtitleIndex != null) options[subtitleIndex] else null,
            onDismissRequest = {
                onControllerInteraction.invoke()
                showCaptionDialog = false
            },
            onSelectChoice = { index, _ ->
                onPlaybackActionClick.invoke(PlaybackAction.ToggleCaptions(index))
            },
            gravity = Gravity.END,
        )
    }
    if (showOptionsDialog) {
        val options = listOf("Audio Track", "Playback Speed", "Video Scale")
        BottomDialog(
            choices = options,
            currentChoice = null,
            onDismissRequest = {
                onControllerInteraction.invoke()
                showOptionsDialog = false
            },
            onSelectChoice = { index, _ ->
                when (index) {
                    0 -> showAudioDialog = true
                    1 -> showSpeedDialog = true
                    2 -> showScaleDialog = true
                }
            },
            gravity = Gravity.END,
        )
    }
    if (showAudioDialog) {
        BottomDialog(
            choices = audioOptions,
            currentChoice = if (audioIndex != null && audioIndex in audioOptions.indices) audioOptions[audioIndex] else null,
            onDismissRequest = {
                onControllerInteraction.invoke()
                showAudioDialog = false
            },
            onSelectChoice = { index, _ ->
                onPlaybackActionClick.invoke(PlaybackAction.ToggleAudio(index))
            },
            gravity = Gravity.END,
        )
    }
    if (showSpeedDialog) {
        BottomDialog(
            choices = speedOptions,
            currentChoice = playbackSpeed.toString(),
            onDismissRequest = {
                onControllerInteraction.invoke()
                showSpeedDialog = false
            },
            onSelectChoice = { _, value ->
                onPlaybackActionClick.invoke(PlaybackAction.PlaybackSpeed(value.toFloat()))
            },
            gravity = Gravity.END,
        )
    }
    if (showScaleDialog) {
        BottomDialog(
            choices = playbackScaleOptions.values.toList(),
            currentChoice = playbackScaleOptions[scale],
            onDismissRequest = {
                onControllerInteraction.invoke()
                showScaleDialog = false
            },
            onSelectChoice = { index, _ ->
                onPlaybackActionClick.invoke(PlaybackAction.Scale(playbackScaleOptions.keys.toList()[index]))
            },
            gravity = Gravity.END,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlaybackButtons(
    player: PlayerControls,
    initialFocusRequester: FocusRequester,
    onControllerInteraction: () -> Unit,
    showPlay: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
    ) {
        PlaybackButton(
            iconRes = R.drawable.baseline_skip_previous_24,
            onClick = {
                onControllerInteraction.invoke()
                player.seekToPrevious()
            },
            enabled = previousEnabled,
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            iconRes = R.drawable.baseline_fast_rewind_24,
            onClick = {
                onControllerInteraction.invoke()
                player.seekBack()
            },
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            modifier = Modifier.focusRequester(initialFocusRequester),
            iconRes = if (showPlay) R.drawable.baseline_play_arrow_24 else R.drawable.baseline_pause_24,
            onClick = {
                onControllerInteraction.invoke()
                player.playOrPause()
            },
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            iconRes = R.drawable.baseline_fast_forward_24,
            onClick = {
                onControllerInteraction.invoke()
                player.seekForward()
            },
            onControllerInteraction = onControllerInteraction,
        )
        PlaybackButton(
            iconRes = R.drawable.baseline_skip_next_24,
            onClick = {
                onControllerInteraction.invoke()
                player.seekToNext()
            },
            enabled = nextEnabled,
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
        enabled = enabled,
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
    onSelectChoice: (Int, String) -> Unit,
    gravity: Int,
    currentChoice: String? = null,
) {
    // TODO enforcing a width ends up ignore the gravity
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
//                        .widthIn(max = 240.dp)
                        .wrapContentWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                choices.forEachIndexed { index, choice ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val focused = interactionSource.collectIsFocusedAsState().value
                    val color =
                        if (focused) {
                            MaterialTheme.colorScheme.inverseOnSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    ListItem(
                        selected = choice == currentChoice,
                        onClick = {
                            onDismissRequest()
                            onSelectChoice(index, choice)
                        },
                        leadingContent = {
                            if (choice == currentChoice) {
                                Box(
                                    modifier =
                                        Modifier
                                            .padding(horizontal = 4.dp)
                                            .clip(CircleShape)
                                            .align(Alignment.Center)
                                            .background(color)
                                            .size(8.dp),
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                text = choice,
                                color = color,
                            )
                        },
                        interactionSource = interactionSource,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PlaybackButtonsPreview() {
    AppTheme {
        PlaybackButtons(
            player = FakePlayerControls,
            initialFocusRequester = FocusRequester(),
            onControllerInteraction = {},
            showPlay = true,
            previousEnabled = true,
            nextEnabled = true,
        )
    }
}

@Preview
@Composable
private fun RightPlaybackButtonsPreview() {
    AppTheme {
        RightPlaybackButtons(
            captions = listOf(),
            onControllerInteraction = {},
            onControllerInteractionForDialog = {},
            onPlaybackActionClick = {},
            subtitleIndex = 1,
            modifier = Modifier,
            audioOptions = listOf(),
            audioIndex = null,
            playbackSpeed = 1.0f,
            scale = ContentScale.Fit,
        )
    }
}

data class MoreButtonOptions(
    val options: Map<String, PlaybackAction>,
)
