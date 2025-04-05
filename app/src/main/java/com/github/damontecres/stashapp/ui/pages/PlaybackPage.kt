package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.ui.components.playback.PlaybackPageContent
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer

@Composable
fun PlaybackPage(
    server: StashServer,
    sceneId: String,
    startPosition: Long,
    playbackMode: PlaybackMode,
    modifier: Modifier = Modifier,
) {
    var scene by remember { mutableStateOf<FullSceneData?>(null) }
    LaunchedEffect(server, sceneId) {
        val fullScene = QueryEngine(server).getScene(sceneId)
        if (fullScene != null) {
            scene = fullScene
        } else {
            Log.w("PlaybackPage", "Scene $sceneId not found")
        }
    }
    Log.i("PlaybackPage", "scene=$scene")
    scene?.let {
        PlaybackPageContent(
            server = server,
            scene = it,
            startPosition = startPosition,
            playbackMode = playbackMode,
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
        )
    }
}
