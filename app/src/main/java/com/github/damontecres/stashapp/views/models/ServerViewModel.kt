package com.github.damontecres.stashapp.views.models

import android.content.Context
import android.util.Log
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
import com.github.damontecres.stashapp.views.models.ServerViewModel.ServerConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    private val _state = MutableStateFlow(ServerPageState())
    val state: StateFlow<ServerPageState> = _state

    fun switchServer(newServer: StashServer?) {
        _state.update { it.copy(serverConnection = ServerConnection.Pending) }
        if (newServer != null) {
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                try {
                    serverRepository.setCurrentStashServer(newServer)
                    _state.update {
                        it.copy(
                            currentServer = newServer,
                            serverConnection = ServerConnection.Success,
                        )
                    }
                    setupNavigationManager.navigateTo(SetupDestination.AppContent(newServer))
                } catch (ex: Exception) {
                    Log.e(TAG, "Error switching servers", ex)
                    _state.update {
                        it.copy(
                            currentServer = null,
                            serverConnection = ServerConnection.Failure(newServer, ex),
                        )
                    }
                }
            }
        } else {
            _state.update {
                it.copy(
                    currentServer = null,
                    serverConnection = ServerConnection.NotConfigured,
                )
            }
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

data class ServerPageState(
    val currentServer: StashServer? = null,
    val serverConnection: ServerConnection = ServerConnection.Pending,
)
