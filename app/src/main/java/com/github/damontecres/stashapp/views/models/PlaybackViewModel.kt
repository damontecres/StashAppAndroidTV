package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

class PlaybackViewModel : ViewModel() {
    private val _scene = MutableLiveData<Scene>()
    val scene: LiveData<Scene?> = _scene

    fun setScene(
        server: StashServer,
        id: String,
    ) {
        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
            val queryEngine = QueryEngine(server)
            val result = queryEngine.getVideoScene(id)
            _scene.value = result?.let { Scene.fromVideoSceneData(it) }
        }
    }
}
