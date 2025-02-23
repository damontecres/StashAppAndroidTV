package com.github.damontecres.stashapp.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.damontecres.stashapp.ui.components.playback.ControllerViewState
import com.github.damontecres.stashapp.ui.components.playback.PlaybackControls
import com.github.damontecres.stashapp.ui.components.playback.PlaybackState
import com.github.damontecres.stashapp.ui.components.playback.PlayerControls

@Preview(uiMode = Configuration.UI_MODE_TYPE_TELEVISION)
@Composable
private fun PlaybackControlsPreview() {
    Box {
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
            modifier = Modifier.align(Alignment.BottomCenter),
            controllerViewState = ControllerViewState(3),
        )
    }
}
