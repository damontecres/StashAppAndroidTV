package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

class PerformerViewModel : ViewModel() {
    private val _performer = MutableLiveData<PerformerData>()
    val performer: LiveData<PerformerData?> = _performer

    fun init(id: String) {
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            val queryEngine = QueryEngine(StashServer.requireCurrentServer())
            _performer.value = queryEngine.getPerformer(id)
        }
    }
}
