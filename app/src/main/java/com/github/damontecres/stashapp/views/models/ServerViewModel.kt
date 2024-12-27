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
import java.util.Objects

/**
 * Tracks the current server
 */
open class ServerViewModel : ViewModel() {
    private val _currentServer = EqualityMutableLiveData<StashServer?>()
    val currentServer: LiveData<StashServer?> = _currentServer

    fun requireServer(): StashServer = currentServer.value!!

    private val _currentSettingsHash = EqualityMutableLiveData(computeSettingsHash())
    val currentSettingsHash: LiveData<Int> = _currentSettingsHash

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

    fun updateSettingsHash() {
        val newHash = computeSettingsHash()
        _currentSettingsHash.value = newHash
    }

    private fun computeSettingsHash(): Int {
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
        return Objects.hash(
            maxSearchResults,
            playVideoPreviews,
            columns,
            showRatings,
            imageCrop,
            videoDelay,
        )
    }

    fun init() {
        updateSettingsHash()
        val currentServer = StashServer.findConfiguredStashServer(StashApplication.getApplication())
        switchServer(currentServer)
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
}
