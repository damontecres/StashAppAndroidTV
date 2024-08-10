package com.github.damontecres.stashapp.playback

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.damontecres.stashapp.data.room.VideoFilter

class VideoFilterViewModel : ViewModel() {
//    val filterActive = MutableLiveData(false)
    val videoFilter = MutableLiveData<VideoFilter?>()
}
