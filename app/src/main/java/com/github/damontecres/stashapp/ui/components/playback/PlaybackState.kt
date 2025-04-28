package com.github.damontecres.stashapp.ui.components.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

@Composable
fun rememberPlaybackState(player: PlayerControls) {
}

class PlaybackState(
    player: PlayerControls,
) {
    var isPlaying = mutableStateOf<Boolean>(false)
        private set
}
