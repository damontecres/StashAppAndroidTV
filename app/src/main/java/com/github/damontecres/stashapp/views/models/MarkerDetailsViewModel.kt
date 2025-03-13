package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

class MarkerDetailsViewModel : ViewModel() {
    val seconds = MutableLiveData<Double>()

    private val _item = EqualityMutableLiveData<FullMarkerData?>()
    val item: LiveData<FullMarkerData?> = _item

    fun init(id: String) {
        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
            val queryEngine = QueryEngine(StashServer.requireCurrentServer())
            val marker = queryEngine.getMarker(id)
            _item.value = marker
            seconds.value = marker?.seconds
        }
    }

    fun setMarker(marker: FullMarkerData) {
        _item.value = marker
    }

    companion object {
        private const val TAG = "MarkerDetailsViewModel"
    }
}
