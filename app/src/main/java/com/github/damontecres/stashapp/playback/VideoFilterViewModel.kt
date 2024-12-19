package com.github.damontecres.stashapp.playback

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.data.room.PlaybackEffect
import com.github.damontecres.stashapp.data.room.VideoFilter
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoFilterViewModel : ViewModel() {
    val videoFilter = MutableLiveData<VideoFilter?>()

    private val serverUrl = StashServer.getCurrentStashServer()?.url
    private lateinit var sceneId: String
    private val saveVideoFilter =
        PreferenceManager.getDefaultSharedPreferences(StashApplication.getApplication()).getBoolean(
            StashApplication.getApplication().getString(R.string.pref_key_playback_save_effects),
            true,
        )

    fun maybeGetSavedFilter(sceneId: String) {
        this.sceneId = sceneId
        if (saveVideoFilter && serverUrl != null) {
            viewModelScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                val vf =
                    StashApplication
                        .getDatabase()
                        .playbackEffectsDao()
                        .getPlaybackEffect(serverUrl, sceneId)
                if (vf != null) {
                    Log.d(TAG, "Loaded VideoFilter for scene $sceneId")
                }
                withContext(Dispatchers.Main) {
                    videoFilter.value = vf?.videoFilter
                }
            }
        } else {
            Log.d(TAG, "No saving video filters")
            videoFilter.value = null
        }
    }

    /**
     * If saving video effects is enabled, save the current one to the database
     */
    fun maybeSaveFilter() {
        if (saveVideoFilter && serverUrl != null) {
            viewModelScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                val vf = videoFilter.value
                if (vf != null) {
                    StashApplication
                        .getDatabase()
                        .playbackEffectsDao()
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
