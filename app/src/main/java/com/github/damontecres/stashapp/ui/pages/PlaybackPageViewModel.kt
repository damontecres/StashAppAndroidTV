package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PlaybackPageViewModel : ViewModel() {
    private lateinit var server: StashServer
    private lateinit var sceneId: String

    val state = MutableStateFlow<PlaybackState?>(null)

    fun init(
        server: StashServer,
        sceneId: String,
    ) {
        this.server = server
        this.sceneId = sceneId
        Log.d("PlaybackViewModel", "scene=$sceneId")
        viewModelScope.launch(
            LoggingCoroutineExceptionHandler(
                server,
                viewModelScope,
                toastMessage = "Error fetching scene",
            ),
        ) {
            val fullScene = QueryEngine(server).getScene(sceneId)
            if (fullScene != null) {
                val scene = Scene.fromFullSceneData(fullScene)
                state.value = PlaybackState(fullScene, scene)
            } else {
                Log.w("PlaybackViewModel", "Scene $sceneId not found")
                Toast.makeText(StashApplication.getApplication(), "Scene $sceneId not found", Toast.LENGTH_LONG).show()
            }
        }
    }
}

data class PlaybackState(
    val fullScene: FullSceneData,
    val scene: Scene,
)
