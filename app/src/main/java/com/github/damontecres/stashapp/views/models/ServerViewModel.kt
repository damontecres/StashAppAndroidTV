package com.github.damontecres.stashapp.views.models

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.getInt
import kotlinx.coroutines.launch

/**
 * Tracks the current server
 */
open class ServerViewModel : ViewModel() {
    private val _currentServer = EqualityMutableLiveData<StashServer?>()
    val currentServer: LiveData<StashServer?> = _currentServer

    fun requireServer(): StashServer = currentServer.value!!

    private val _cardUiSettings = EqualityMutableLiveData(createUiSettings())
    val cardUiSettings: LiveData<CardUiSettings> = _cardUiSettings

    lateinit var navigationManager: NavigationManager

    fun switchServer(newServer: StashServer?) {
        if (newServer != null) {
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                newServer.updateServerPrefs()
                StashServer.setCurrentStashServer(StashApplication.getApplication(), newServer)
                _currentServer.value = newServer
            }
        } else {
            _currentServer.value = null
        }
    }

    fun updateUiSettings() {
        val newHash = createUiSettings()
        _cardUiSettings.value = newHash
    }

    private fun createUiSettings(): CardUiSettings {
        val context = StashApplication.getApplication()
        val manager = PreferenceManager.getDefaultSharedPreferences(context)
        val maxSearchResults = manager.getInt("maxSearchResults", 0)
        val playVideoPreviews = manager.getBoolean("playVideoPreviews", true)
        val columns = manager.getInt("cardSize", context.getString(R.string.card_size_default))
        val showRatings = manager.getBoolean(context.getString(R.string.pref_key_show_rating), true)
        val imageCrop =
            manager.getBoolean(context.getString(R.string.pref_key_crop_card_images), true)
        val videoDelay =
            manager.getInt(
                context.getString(R.string.pref_key_ui_card_overlay_delay),
                context.resources.getInteger(R.integer.pref_key_ui_card_overlay_delay_default),
            )
        return CardUiSettings(
            maxSearchResults,
            playVideoPreviews,
            columns,
            showRatings,
            imageCrop,
            videoDelay,
        )
    }

    fun init(currentServer: StashServer) {
        updateUiSettings()
        switchServer(currentServer)
    }

    fun updateServerPreferences() {
        currentServer.value?.let { server ->
            viewModelScope.launch(StashCoroutineExceptionHandler()) {
                server.updateServerPrefs()
            }
        }
    }

    fun maybeShowUpdate(context: Context) {
        val checkForUpdates =
            PreferenceManager
                .getDefaultSharedPreferences(StashApplication.getApplication())
                .getBoolean("autoCheckForUpdates", true)
        if (checkForUpdates) {
            viewModelScope.launch(StashCoroutineExceptionHandler()) {
                UpdateChecker.checkForUpdate(context, false)
            }
        }
    }

    /**
     * Basic UI settings that affect the cards
     */
    data class CardUiSettings(
        val maxSearchResults: Int,
        val playVideoPreviews: Boolean,
        val columns: Int,
        val showRatings: Boolean,
        val imageCrop: Boolean,
        val videoDelay: Int,
    )
}
