package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter.PagerEntry
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

class TabbedGridViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val title: MutableLiveData<CharSequence?> = savedStateHandle.getLiveData("title", null)

    val tabs: MutableLiveData<List<PagerEntry>> = MutableLiveData()

    private val _currentServer = MutableLiveData<StashServer>()
    val currentServer: LiveData<StashServer> = _currentServer

    /**
     * Updates to the current server with populated server preferences
     */
    fun refreshServer() {
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            val server = StashServer.requireCurrentServer()
            server.updateServerPrefs()
            _currentServer.value = server
        }
    }
}
