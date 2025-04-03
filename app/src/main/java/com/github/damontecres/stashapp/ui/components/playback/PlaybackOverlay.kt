package com.github.damontecres.stashapp.ui.components.playback

import androidx.annotation.IntRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.StreamDecision
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce

class ControllerViewState internal constructor(
    @IntRange(from = 0)
    private val hideSeconds: Int,
    val controlsEnabled: Boolean,
) {
    private val channel = Channel<Int>(CONFLATED)
    private var _controlsVisible by mutableStateOf(false)
    val controlsVisible get() = _controlsVisible

    fun showControls(seconds: Int = hideSeconds) {
        if (controlsEnabled) {
            _controlsVisible = true
        }
        pulseControls(seconds)
    }

    fun pulseControls(seconds: Int = hideSeconds) = channel.trySend(seconds)

    @OptIn(FlowPreview::class)
    suspend fun observe() {
        channel
            .consumeAsFlow()
            .debounce { it.toLong() * 1000 }
            .collect {
//                Log.i("ControllerViewState", "collect")
                _controlsVisible = false
            }
    }
}

@Composable
fun PlaybackOverlay(
    server: StashServer,
    scene: Scene,
    streamDecision: StreamDecision,
    oCounter: Int,
    player: Player,
    controllerViewState: ControllerViewState,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    showDebugInfo: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        controllerViewState.controlsVisible,
        Modifier,
        slideInVertically { it },
        slideOutVertically { it },
    ) {
        Box(
            modifier,
        ) {
            if (showDebugInfo) {
                PlaybackDebugInfo(
                    scene = scene,
                    streamDecision = streamDecision,
                    modifier =
                        Modifier
                            .padding(8.dp)
                            .background(AppColors.TransparentBlack50)
                            .align(Alignment.TopStart)
                            // TODO the width isn't be used correctly
                            .width(280.dp),
                )
            }
            PlaybackControls(
                modifier = Modifier.align(Alignment.BottomCenter),
                scene = scene,
                oCounter = oCounter,
                player = player,
                onPlaybackActionClick = onPlaybackActionClick,
                controllerViewState = controllerViewState,
                showDebugInfo = showDebugInfo,
            )
        }
    }
}
