package com.github.damontecres.stashapp.playback

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.damontecres.stashapp.suppliers.FilterArgs

class PlaylistViewModel : ViewModel() {
    val filterArgs = MutableLiveData<FilterArgs>()
}
