package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.services.ServerLogger
import com.github.damontecres.stashapp.util.launchIO
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.coroutines.CoroutineContext

@KoinViewModel
class PlaybackPageViewModel(
    private val serverLogger: ServerLogger,
    private val queryEngine: QueryEngine,
    @InjectedParam private val sceneId: String,
) : ViewModel() {
    private val exceptionHandler =
        object : CoroutineExceptionHandler {
            override val key: CoroutineContext.Key<*>
                get() = CoroutineExceptionHandler

            override fun handleException(
                context: CoroutineContext,
                exception: Throwable,
            ) {
                Logger.e(exception) { "Exception" }
                viewModelScope.launchIO {
                    serverLogger.logException(exception, null)
                }
            }
        }

    val state = MutableStateFlow<PlaybackState?>(null)

    init {
        Log.d("PlaybackViewModel", "scene=$sceneId")
        viewModelScope.launch(exceptionHandler) {
            val fullScene = queryEngine.getScene(sceneId)
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
