package com.github.damontecres.stashapp.playback

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.data.room.AppDatabase
import com.github.damontecres.stashapp.data.room.VideoFilter
import com.github.damontecres.stashapp.playback.PlaybackSceneFragment.Companion.DB_NAME
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoFilterViewModel : ViewModel() {
    private val db =
        Room.databaseBuilder(
            StashApplication.getApplication(),
            AppDatabase::class.java,
            DB_NAME,
        ).build()

    val videoFilter = MutableLiveData<VideoFilter?>()

    private lateinit var stashUrl: String
    private lateinit var sceneId: String

    fun initialize(
        server: StashServer,
        sceneId: String,
    ) {
        stashUrl = server.url
        this.sceneId = sceneId
    }

    fun saveFilter() {
        viewModelScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
        }
    }
}
