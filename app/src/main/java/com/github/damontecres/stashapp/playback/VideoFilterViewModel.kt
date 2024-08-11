package com.github.damontecres.stashapp.playback

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.data.room.AppDatabase
import com.github.damontecres.stashapp.data.room.PlaybackEffect
import com.github.damontecres.stashapp.data.room.VideoFilter
import com.github.damontecres.stashapp.playback.PlaybackSceneFragment.Companion.DB_NAME
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoFilterViewModel : ViewModel() {
    private val db =
        Room.databaseBuilder(
            StashApplication.getApplication(),
            AppDatabase::class.java,
            DB_NAME,
        )
            .fallbackToDestructiveMigration()
            .build()

    val videoFilter = MutableLiveData<VideoFilter?>()

    private lateinit var serverUrl: String
    private lateinit var sceneId: String
    private var saveVideoFilter = true

    fun initialize(
        server: StashServer,
        sceneId: String,
        saveVideoFilter: Boolean,
    ) {
        serverUrl = server.url
        this.sceneId = sceneId
        this.saveVideoFilter = saveVideoFilter
        if (saveVideoFilter) {
            viewModelScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                val vf = db.playbackEffectsDao().getPlaybackEffect(serverUrl, sceneId)
                if (vf != null) {
                    Log.d(TAG, "Loaded VideoFilter for scene $sceneId")
                    withContext(Dispatchers.Main) {
                        videoFilter.value = vf.videoFilter
                    }
                }
            }
        }
    }

    /**
     * If saving video effects is enabled, save the current one to the database
     */
    fun maybeSaveFilter() {
        if (saveVideoFilter) {
            viewModelScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                val vf = videoFilter.value
                if (vf != null) {
                    db.playbackEffectsDao()
                        .insert(PlaybackEffect(serverUrl, sceneId, vf))
                    Log.d(TAG, "Saved VideoFilter for scene $sceneId")
                }
            }
        }
    }

    companion object {
        private const val TAG = "VideoFilterViewModel"
    }
}
