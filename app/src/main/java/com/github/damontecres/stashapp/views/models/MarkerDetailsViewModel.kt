package com.github.damontecres.stashapp.views.models

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.playback.buildMediaItem
import com.github.damontecres.stashapp.playback.getStreamDecision
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarkerDetailsViewModel : ViewModel() {
    private val retriever = MediaMetadataRetriever()
    private var job: Job? = null

    val seconds = MutableLiveData<Double>()

    private val _item = EqualityMutableLiveData<FullMarkerData?>()
    val item: LiveData<FullMarkerData?> = _item

    val screenshot = MutableLiveData<ImageLoadState>(ImageLoadState.Initializing)

    fun init(id: String) {
        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
            val queryEngine = QueryEngine(StashServer.requireCurrentServer())
            val marker = queryEngine.getMarker(id)
            _item.value = marker
            seconds.value = marker?.seconds
            if (marker?.seconds != null) {
                val context = StashApplication.getApplication()
                val scene = Scene.fromVideoSceneData(item.value!!.scene.videoSceneData)
                val streamDecision =
                    getStreamDecision(context, scene, PlaybackMode.FORCED_DIRECT_PLAY)
                val mediaItem = buildMediaItem(context, streamDecision, scene)
                try {
                    retriever.setDataSource(mediaItem.localConfiguration!!.uri.toString())
                    screenshot.value = ImageLoadState.Initialized
                } catch (ex: Exception) {
                    Log.w(TAG, "Exception initializing", ex)
                    screenshot.value = ImageLoadState.Error(ex.message)
                }
            }
        }
    }

    fun setMarker(marker: FullMarkerData) {
        _item.value = marker
    }

    fun getImageFor(
        positionMs: Long,
        delayMs: Long,
    ) {
        job?.cancel()
        if (screenshot.value == ImageLoadState.Initializing) {
            return
        }
        job =
            viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
                Log.v(TAG, "getImageFor: positionMs=$positionMs")
                delay(delayMs)
                screenshot.value = ImageLoadState.Loading
                screenshot.value =
                    withContext(Dispatchers.IO) {
                        ImageLoadState.Success(
                            retriever.getFrameAtTime(
                                positionMs * 1000,
                                MediaMetadataRetriever.OPTION_CLOSEST,
                            ),
                        )
                    }
                Log.v(TAG, "getImageFor: positionMs=$positionMs DONE")
            }
    }

    sealed interface ImageLoadState {
        data object Initializing : ImageLoadState

        data object Initialized : ImageLoadState

        data object Loading : ImageLoadState

        data class Success(
            val image: Bitmap?,
        ) : ImageLoadState

        data class Error(
            val message: String?,
        ) : ImageLoadState
    }

    companion object {
        private const val TAG = "MarkerDetailsViewModel"
    }
}
