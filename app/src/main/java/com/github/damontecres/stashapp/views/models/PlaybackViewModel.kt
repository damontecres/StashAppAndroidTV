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

    fun setScene(id: String) {
        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
            val queryEngine = QueryEngine(StashServer.requireCurrentServer())
            val scenes = queryEngine.findScenes(ids = listOf(id))
            _scene.value = scenes.firstOrNull()?.let { Scene.fromSlimSceneData(it) }
        }
    }
}
