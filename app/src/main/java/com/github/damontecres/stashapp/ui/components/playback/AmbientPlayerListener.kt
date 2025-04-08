package com.github.damontecres.stashapp.ui.components.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import com.github.damontecres.stashapp.util.findActivity
import com.github.damontecres.stashapp.util.keepScreenOn

@Composable
fun AmbientPlayerListener(player: Player) {
    val context = LocalContext.current
    DisposableEffect(player) {
        val listener =
            object : Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    context.findActivity()?.keepScreenOn(isPlaying)
                }
            }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            context.findActivity()?.keepScreenOn(false)
        }
    }
}
