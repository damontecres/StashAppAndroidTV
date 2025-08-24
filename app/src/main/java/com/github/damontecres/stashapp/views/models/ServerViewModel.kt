package com.github.damontecres.stashapp.views.models

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.components.prefs.SharedPreferencesListener
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.getStringNotNull
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Tracks the current server
 */
open class ServerViewModel : ViewModel() {
    private lateinit var preferenceListener: SharedPreferences.OnSharedPreferenceChangeListener
    private val _currentServer = EqualityMutableLiveData<StashServer?>()
    val currentServer: LiveData<StashServer?> = _currentServer

    private val _serverConnection = MutableLiveData<ServerConnection>(ServerConnection.Pending)
    val serverConnection: LiveData<ServerConnection> = _serverConnection

    fun requireServer(): StashServer = currentServer.value!!

    private val _cardUiSettings = EqualityMutableLiveData(createUiSettings())
    val cardUiSettings: LiveData<CardUiSettings> = _cardUiSettings

    lateinit var navigationManager: NavigationManager
    private val _destination = MutableLiveData<Destination>()
    val destination: LiveData<Destination> = _destination

    fun setCurrentDestination(destination: Destination) {
        _destination.value = destination
    }

    fun switchServer(newServer: StashServer?) {
        _serverConnection.value = ServerConnection.Pending
        if (newServer != null) {
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                try {
                    newServer.updateServerPrefs()
                    StashServer.setCurrentStashServer(StashApplication.getApplication(), newServer)
                    _currentServer.value = newServer
                    _serverConnection.value = ServerConnection.Success
                    submit(Destination.Main, true)
                } catch (ex: Exception) {
                    _currentServer.setValueNoCheck(null)
                    _serverConnection.value = ServerConnection.Failure(newServer, ex)
                }
            }
        } else {
            _currentServer.setValueNoCheck(null)
            _serverConnection.value = ServerConnection.NotConfigured
        }
    }

    fun updateUiSettings() {
        val newHash = createUiSettings()
        _cardUiSettings.value = newHash
    }

    fun init(
        context: Context,
        currentServer: StashServer,
        useCompose: Boolean,
    ) {
        if (!this::preferenceListener.isInitialized) {
            preferenceListener = SharedPreferencesListener(context, viewModelScope)
        } else {
            Log.d(TAG, "Removing shared preference listener")
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .unregisterOnSharedPreferenceChangeListener(preferenceListener)
        }
        if (!useCompose) {
            Log.d(TAG, "Registering shared preference listener")
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(preferenceListener)
        }
        updateUiSettings()
        switchServer(currentServer)
    }

    fun updateServerPreferences() {
        currentServer.value?.let { server ->
            viewModelScope.launch(StashCoroutineExceptionHandler()) {
                try {
                    server.updateServerPrefs()
                } catch (ex: QueryEngine.QueryException) {
                    Log.w(TAG, "Error updating server preferences", ex)
                    val result =
                        testStashConnection(
                            StashApplication.getApplication(),
                            false,
                            server.apolloClient,
                        )
                    if (result !is TestResult.Success) {
                        Toast
                            .makeText(
                                StashApplication.getApplication(),
                                "Error connecting to ${server.url}",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                }
            }
        }
    }

    fun maybeShowUpdate(context: Context) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        if (!pref.getBoolean("autoCheckForUpdates", true)) {
            return
        }
        val updateUrl =
            pref.getStringNotNull(
                "updateCheckUrl",
                context.getString(R.string.app_update_url),
            )
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            UpdateChecker.maybeShowUpdateToast(context, updateUrl, false)
        }
    }

    fun <T> withLiveData(liveData: LiveData<T?>): LiveData<Pair<StashServer, T?>> =
        currentServer
            .asFlow()
            .combine(liveData.asFlow()) { server, item ->
                server!! to item
            }.asLiveData()

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

    // For compose navigation
    val command = MutableLiveData<NavigationCommand?>(null)

    fun submit(
        destination: Destination,
        popUpToMain: Boolean = false,
    ) {
        command.value = NavigationCommand(destination, popUpToMain)
    }
}

@Serializable
data class NavigationCommand(
    val destination: Destination,
    val popUpToMain: Boolean,
)
