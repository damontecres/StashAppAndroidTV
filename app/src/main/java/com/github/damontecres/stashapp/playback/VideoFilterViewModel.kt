package com.github.damontecres.stashapp.playback

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.VideoFilter
import com.github.damontecres.stashapp.data.room.PlaybackEffect
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoFilterViewModel : ViewModel() {
    val videoFilter = MutableLiveData<VideoFilter?>()

    lateinit var dataType: DataType
    lateinit var fetchCurrentId: () -> String

    private val serverUrl = StashServer.getCurrentStashServer()?.url

    private val saveVideoFilter =
        PreferenceManager.getDefaultSharedPreferences(StashApplication.getApplication()).getBoolean(
            StashApplication.getApplication().getString(R.string.pref_key_playback_save_effects),
            true,
        )

    fun maybeGetSavedFilter() {
        if (saveVideoFilter && serverUrl != null) {
            val id = fetchCurrentId.invoke()
            viewModelScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                val vf =
                    StashApplication
                        .getDatabase()
                        .playbackEffectsDao()
                        .getPlaybackEffect(serverUrl, id, dataType)
                if (vf != null) {
                    Log.d(TAG, "Loaded VideoFilter for $dataType $id")
                }
                withContext(Dispatchers.Main) {
                    videoFilter.value = vf?.videoFilter
                }
            }
        } else {
            Log.d(TAG, "Not saving video filters")
            videoFilter.value = null
        }
    }

    /**
     * If saving video effects is enabled, save the current one to the database
     */
    fun maybeSaveFilter() {
        if (saveVideoFilter && serverUrl != null) {
            val id = fetchCurrentId.invoke()
            viewModelScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                val vf = videoFilter.value
                if (vf != null) {
                    StashApplication
                        .getDatabase()
                        .playbackEffectsDao()
                        .insert(PlaybackEffect(serverUrl, id, dataType, vf))
                    Log.d(TAG, "Saved VideoFilter for $dataType $id")
                    withContext(Dispatchers.Main) {
                        Toast
                            .makeText(
                                StashApplication.getApplication(),
                                "Saved",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            }
        }
    }

    fun init(
        dataType: DataType,
        fetchCurrentId: () -> String,
    ) {
        this.dataType = dataType
        this.fetchCurrentId = fetchCurrentId
    }

    companion object {
        private const val TAG = "VideoFilterViewModel"
    }
}
