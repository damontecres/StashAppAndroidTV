package com.github.damontecres.stashapp.views.models

import android.graphics.Bitmap
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
import wseemann.media.FFmpegMediaMetadataRetriever

class MarkerDetailsViewModel : ViewModel() {
    private var retriever: FFmpegMediaMetadataRetriever? = null
    private var job: Job? = null

    val seconds = MutableLiveData<Double>()

    private val _item = EqualityMutableLiveData<FullMarkerData?>()
    val item: LiveData<FullMarkerData?> = _item

    val screenshot = MutableLiveData<ImageLoadState>(ImageLoadState.Initializing)

    fun init(
        id: String,
        initializeRetriever: Boolean,
    ) {
        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
            val queryEngine = QueryEngine(StashServer.requireCurrentServer())
            val marker = queryEngine.getMarker(id)
            _item.value = marker
            seconds.value = marker?.seconds
            if (initializeRetriever && marker?.seconds != null) {
                val context = StashApplication.getApplication()
                val scene = Scene.fromVideoSceneData(item.value!!.scene.videoSceneData)
                val streamDecision =
                    getStreamDecision(context, scene, PlaybackMode.FORCED_DIRECT_PLAY)
                val mediaItem = buildMediaItem(context, streamDecision, scene)
                try {
                    retriever = FFmpegMediaMetadataRetriever()
                    retriever!!.setDataSource(mediaItem.localConfiguration!!.uri.toString())
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
        if (!canFetch(screenshot.value)) {
            return
        }
        job =
            viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
                Log.v(TAG, "getImageFor: positionMs=$positionMs")
                delay(delayMs)
                screenshot.value = ImageLoadState.Loading
                screenshot.value =
                    withContext(Dispatchers.IO) {
                        try {
                            val image =
                                retriever?.getFrameAtTime(
                                    positionMs * 1000,
                                    FFmpegMediaMetadataRetriever.OPTION_CLOSEST,
                                )
                            if (image != null) {
                                ImageLoadState.Success(image)
                            } else {
                                ImageLoadState.Error("No image")
                            }
                        } catch (ex: Exception) {
                            Log.w(TAG, "Error extracting frame", ex)
                            ImageLoadState.Error(ex.message)
                        }
                    }
                Log.v(TAG, "getImageFor: positionMs=$positionMs DONE")
            }
    }

    fun release() {
        retriever?.release()
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

    private fun canFetch(state: ImageLoadState?): Boolean =
        state == ImageLoadState.Initialized || state == ImageLoadState.Loading || state is ImageLoadState.Success

    companion object {
        private const val TAG = "MarkerDetailsViewModel"
    }
}
