package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getInt
import java.util.Objects

class ServerViewModel : ViewModel() {
    private val _currentServer =
        EqualityMutableLiveData<StashServer?>(StashServer.getCurrentStashServer(StashApplication.getApplication()))
    val currentServer: LiveData<StashServer?> = _currentServer

    private val _currentSettingsHash = EqualityMutableLiveData(computeSettingsHash())
    val currentSettingsHash: LiveData<Int> = _currentSettingsHash

    fun switchServer(newServer: StashServer) {
        StashServer.setCurrentStashServer(StashApplication.getApplication(), newServer)
        _currentServer.value = newServer
    }

    private fun computeSettingsHash(): Int {
        val context = StashApplication.getApplication()
        val manager = PreferenceManager.getDefaultSharedPreferences(context)
        val maxSearchResults = manager.getInt("maxSearchResults", 0)
        val playVideoPreviews = manager.getBoolean("playVideoPreviews", true)
        val columns = manager.getInt("cardSize", context.getString(R.string.card_size_default))
        val showRatings = manager.getBoolean(context.getString(R.string.pref_key_show_rating), true)
        return Objects.hash(maxSearchResults, playVideoPreviews, columns, showRatings)
    }

    fun refresh() {
        val newHash = computeSettingsHash()
        _currentSettingsHash.value = newHash

        val currentServer = StashServer.getCurrentStashServer(StashApplication.getApplication())
        _currentServer.value = currentServer
    }
}
