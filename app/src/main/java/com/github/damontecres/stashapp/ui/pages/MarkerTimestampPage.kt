package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.type.SceneMarkerUpdateInput
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.playback.buildMediaItem
import com.github.damontecres.stashapp.playback.getStreamDecision
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.SwitchWithLabel
import com.github.damontecres.stashapp.ui.components.TimestampPicker
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.util.toLongMilliseconds
import com.github.damontecres.stashapp.util.toSeconds
import com.github.damontecres.stashapp.views.models.MarkerDetailsViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val TAG = "MarkerTimestampPage"

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(FlowPreview::class)
@Composable
fun MarkerTimestampPage(
    server: StashServer,
    navigationManager: NavigationManager,
    uiConfig: ComposeUiConfig,
    markerId: String,
    modifier: Modifier = Modifier,
    viewModel: MarkerDetailsViewModel = viewModel(),
) {
    LaunchedEffect(markerId) { viewModel.init(server, markerId) }

    val scope = rememberCoroutineScope()
    val marker by viewModel.item.observeAsState()
    marker?.let { marker ->
        val start by viewModel.start.observeAsState(marker.seconds.seconds)
        val end by viewModel.end.observeAsState((marker.end_seconds ?: marker.seconds).seconds)
        val derivedNewStart by remember { derivedStateOf { start } }

        var setEndTimestamp by remember { mutableStateOf(marker.end_seconds != null) }
        var saving by remember { mutableStateOf(false) }

        val maxDuration =
            remember(marker) {
                marker.scene.videoSceneData.files
                    .firstOrNull()
                    ?.videoFile
                    ?.duration
                    ?.seconds
            }
        if (maxDuration != null) {
            val context = LocalContext.current
            val mediaItem =
                remember {
                    val scene = Scene.fromVideoSceneData(marker.scene.videoSceneData)
                    val streamDecision =
                        getStreamDecision(
                            context,
                            scene,
                            PlaybackMode.Choose,
                            uiConfig.preferences.playbackPreferences.streamChoice,
                            uiConfig.preferences.playbackPreferences.transcodeAboveResolution,
                        )
                    buildMediaItem(context, streamDecision, scene)
                }

            val player =
                remember {
                    StashExoPlayer.getInstance(context, server).apply {
                        playWhenReady = false
                    }
                }
            var timestampChanged by remember { mutableStateOf(false) }
            val presentationState = rememberPresentationState(player)
            val playPause = rememberPlayPauseButtonState(player)
            val scaledModifier =
                Modifier.resizeWithContentScale(ContentScale.Fit, presentationState.videoSizeDp)

            LaunchedEffect(Unit) {
                viewModel.start.asFlow().debounce { 500L }.collect { ts ->
                    if (player.currentPosition != ts.inWholeMilliseconds) {
                        player.setMediaItem(mediaItem, ts.inWholeMilliseconds)
                        player.prepare()
                    }
                    timestampChanged = false
                }
            }

            LaunchedEffect(Unit) {
                viewModel.end.asFlow().debounce { 500L }.collectIndexed { index, ts ->
                    if (index > 0) {
                        if (player.currentPosition != ts.inWholeMilliseconds) {
                            player.setMediaItem(mediaItem, ts.inWholeMilliseconds)
                            player.prepare()
                        }
                        timestampChanged = false
                    }
                }
            }

            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.tryRequestFocus() }

            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = modifier,
            ) {
                PlayerSurface(
                    player = player,
                    surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                    modifier = scaledModifier,
                )
                if (presentationState.coverSurface || timestampChanged) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color.Black),
                    ) {
                        CircularProgress(
                            modifier =
                                Modifier
                                    .size(80.dp)
                                    .align(Alignment.Center),
                            false,
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth(.66f)
                            .background(AppColors.TransparentBlack50),
                ) {
                    MarkerTimestampHeader(marker)
                    TimestampPicker(
                        timestamp = marker.seconds.seconds,
                        maxDuration = maxDuration,
                        onValueChange = { viewModel.start.value = it },
                        modifier =
                            Modifier
                                .fillMaxWidth(.8f)
                                .focusRequester(focusRequester),
                    )
                    AnimatedVisibility(setEndTimestamp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Set end timestamp to...",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TimestampPicker(
                                timestamp = (marker.end_seconds ?: marker.seconds).seconds,
                                maxDuration = maxDuration,
                                onValueChange = { viewModel.end.value = it },
                                modifier = Modifier.fillMaxWidth(.8f),
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp),
                    ) {
                        SwitchWithLabel(
                            label = "Set end timestamp?",
                            checked = setEndTimestamp,
                            onStateChange = { setEndTimestamp = it },
                            modifier = Modifier,
                        )
                        Button(
                            onClick = { playPause.onClick() },
                            enabled = playPause.isEnabled,
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        if (playPause.showPlay) {
                                            R.drawable.baseline_play_arrow_24
                                        } else {
                                            R.drawable.baseline_pause_24
                                        },
                                    ),
                                contentDescription = null,
                            )
                        }
                        Button(
                            onClick = {
                                if (setEndTimestamp && start > end) {
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.stashapp_validation_end_time_before_start_time),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                } else {
                                    saving = true
                                    scope.launch(
                                        StashCoroutineExceptionHandler(
                                            autoToast = true,
                                        ),
                                    ) {
                                        try {
                                            val mutationEngine = MutationEngine(server)
                                            val newEnd =
                                                if (setEndTimestamp) {
                                                    Optional.present(end.inWholeMilliseconds.toSeconds)
                                                } else {
                                                    Optional.present(null)
                                                }
                                            val result =
                                                mutationEngine.updateMarker(
                                                    SceneMarkerUpdateInput(
                                                        id = marker.id,
                                                        scene_id =
                                                            Optional.present(
                                                                viewModel.item.value!!
                                                                    .scene.videoSceneData.id,
                                                            ),
                                                        seconds = Optional.present(start.inWholeMilliseconds.toSeconds),
                                                        end_seconds = newEnd,
                                                    ),
                                                )
                                            Log.v(
                                                TAG,
                                                "newSeconds=${result?.seconds}, newEnd=${result?.end_seconds}",
                                            )

                                            navigationManager.goBack()
                                        } finally {
                                            saving = false
                                        }
                                    }
                                }
                            },
                        ) {
                            if (saving) {
                                CircularProgress()
                            } else {
                                Text(
                                    text = stringResource(R.string.stashapp_actions_save),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "No video file",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
fun MarkerTimestampHeader(
    marker: FullMarkerData,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        marker.scene.videoSceneData.titleOrFilename?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        val title = "${marker.primary_tag.tagData.name} - ${
            marker.seconds.toLongMilliseconds.toDuration(DurationUnit.MILLISECONDS)
        }"
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Set start timestamp to...",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
