package com.github.damontecres.stashapp.views.models

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.server.StashServer
import com.github.damontecres.stashapp.di.services.SetupNavigationManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.SetupDestination
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.getInt
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.annotation.KoinViewModel

/**
 * Tracks the current server
 */
@KoinViewModel
open class ServerViewModel(
    private val serverRepository: ServerRepository,
    private val setupNavigationManager: SetupNavigationManager,
) : ViewModel() {
    private val _currentServer = EqualityMutableLiveData<StashServer?>()
    val currentServer: LiveData<StashServer?> = _currentServer

    private val _serverConnection = MutableLiveData<ServerConnection>(ServerConnection.Pending)
    val serverConnection: LiveData<ServerConnection> = _serverConnection

    fun switchServer(newServer: StashServer?) {
        _serverConnection.value = ServerConnection.Pending
        if (newServer != null) {
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                try {
                    serverRepository.setCurrentStashServer(newServer)
                    _currentServer.value = newServer
                    _serverConnection.value = ServerConnection.Success
                    setupNavigationManager.navigateTo(SetupDestination.AppContent(newServer))
                } catch (ex: Exception) {
                    Log.e(TAG, "Error switching servers", ex)
                    _currentServer.setValueNoCheck(null)
                    _serverConnection.value = ServerConnection.Failure(newServer, ex)
                }
            }
        } else {
            _currentServer.setValueNoCheck(null)
            _serverConnection.value = ServerConnection.NotConfigured
        }
    }

    sealed interface ServerConnection {
        data object Pending : ServerConnection

        data object Success : ServerConnection

        data class Failure(
            val server: StashServer,
            val exception: Exception,
        ) : ServerConnection

        data object NotConfigured : ServerConnection
    }

    companion object {
        private const val TAG = "ServerViewModel"

        fun createUiSettings(context: Context = StashApplication.getApplication()): CardUiSettings {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            val maxSearchResults = manager.getInt("maxSearchResults", 25)
            val playVideoPreviews = manager.getBoolean("playVideoPreviews", true)
            val videoPreviewAudio = manager.getBoolean("videoPreviewAudio", false)
            val columns =
                manager.getInt(
                    context.getString(R.string.pref_key_card_size),
                    context.getString(R.string.card_size_default),
                )
            val showRatings =
                manager.getBoolean(context.getString(R.string.pref_key_show_rating), true)
            val imageCrop =
                manager.getBoolean(context.getString(R.string.pref_key_crop_card_images), true)
            val videoDelay =
                manager
                    .getInt(
                        context.getString(R.string.pref_key_ui_card_overlay_delay),
                        context.resources.getInteger(R.integer.pref_key_ui_card_overlay_delay_default),
                    ).toLong()
            return CardUiSettings(
                maxSearchResults,
                playVideoPreviews,
                videoPreviewAudio,
                columns,
                showRatings,
                imageCrop,
                videoDelay,
            )
        }

        val StashPreferences.cardSettings: CardUiSettings
            get() =
                CardUiSettings(
                    maxSearchResults = searchPreferences.maxResults,
                    playVideoPreviews = interfacePreferences.playVideoPreviews,
                    videoPreviewAudio = interfacePreferences.videoPreviewAudio,
                    columns = interfacePreferences.cardSize,
                    showRatings = interfacePreferences.showRatingOnCards,
                    imageCrop = true,
                    videoDelay = interfacePreferences.cardPreviewDelayMs,
                )
    }
}

@Serializable
data class NavigationCommand(
    val destination: Destination,
    val popUpToMain: Boolean,
)
