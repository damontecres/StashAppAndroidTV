package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

class PerformerViewModel : ViewModel() {
    val performer = MutableLiveData<PerformerData>()

    fun update(performerId: String) {
        val queryEngine = QueryEngine(StashServer.getCurrentStashServer()!!)
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            val perf = queryEngine.getPerformer(performerId)
            if (perf != null) {
                performer.value = perf!!
            }
        }
    }
}
