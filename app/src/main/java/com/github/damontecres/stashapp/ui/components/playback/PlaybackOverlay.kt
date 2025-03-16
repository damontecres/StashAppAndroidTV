package com.github.damontecres.stashapp.ui.components.playback

import android.util.Log
import androidx.annotation.IntRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce

class ControllerViewState internal constructor(
    @IntRange(from = 0)
    private val hideSeconds: Int,
) {
    private val channel = Channel<Int>(CONFLATED)
    private var _controlsVisible by mutableStateOf(false)
    val controlsVisible get() = _controlsVisible

    fun showControls(seconds: Int = hideSeconds) {
        _controlsVisible = true
        pulseControls(seconds)
    }

    fun pulseControls(seconds: Int = hideSeconds) = channel.trySend(seconds)

    @OptIn(FlowPreview::class)
    suspend fun observe() {
        channel
            .consumeAsFlow()
            .debounce { it.toLong() * 1000 }
            .collect {
                Log.i("ControllerViewState", "collect")
                _controlsVisible = false
            }
    }
}

@Composable
fun PlaybackOverlay(
    server: StashServer,
    scene: Scene,
    player: Player,
    playbackState: PlaybackState,
    controllerViewState: ControllerViewState,
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
            PlaybackControls(
                modifier = Modifier.align(Alignment.BottomCenter),
                player = ExoPlayerControls(player),
                playbackState = playbackState,
                controllerViewState = controllerViewState,
            )
        }
    }
}
