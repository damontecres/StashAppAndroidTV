package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.tv.material3.MaterialTheme
import co.touchlab.kermit.Logger
import com.github.damontecres.stashapp.di.AuthHttpClient
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.di.services.SetupNavigationManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.SetupDestination
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.chooseColorScheme
import com.github.damontecres.stashapp.ui.components.LoadingPage
import com.github.damontecres.stashapp.ui.defaultColorSchemeSet
import com.github.damontecres.stashapp.ui.nav.CoilConfig
import com.github.damontecres.stashapp.ui.nav.SetupContent
import com.github.damontecres.stashapp.ui.readThemeJson
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.launchDefault
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.annotation.KoinViewModel
import org.koin.core.qualifier.qualifier
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModel()
    private val setupNavigationManager: SetupNavigationManager by inject()
    private val navigationManager: NavigationManager by inject()
    private val serverRepository: ServerRepository by inject()
    private val preferences: DataStore<StashPreferences> by inject()

    private val httpClient: OkHttpClient = get(qualifier<AuthHttpClient>())

    private val json =
        Json {
            classDiscriminator = "_type"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        Logger.i { "onCreate: has savedInstanceState?=${savedInstanceState != null} " }

        val backStackStr = savedInstanceState?.getString(KEY_BACK_STACK)
        if (backStackStr != null) {
            Timber.d("Restoring back stack")
            val backStack = json.decodeFromString<List<Destination>>(backStackStr)
            navigationManager.backStack = NavBackStack(*backStack.toTypedArray())
        } else {
            navigationManager.backStack = NavBackStack(Destination.Main())
        }
        showContent()
    }

    override fun onResume() {
        super.onResume()
        Logger.i { "onResume" }
        viewModel.appStart(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Timber.d("onSaveInstanceState")
        val str = json.encodeToString(navigationManager.backStack.toList())
        outState.putString(KEY_BACK_STACK, str)
    }

    fun showContent() {
        Logger.i { "showContent" }
        setContent {
            val preferences by preferences.data.collectAsState(null)
            if (preferences == null) {
                LoadingPage(Modifier.fillMaxSize())
            } else {
                preferences?.let { preferences ->
                    CoilConfig(httpClient, preferences)
                    val isSystemInDarkTheme = isSystemInDarkTheme()
                    var colorScheme by
                        remember {
                            mutableStateOf(
                                com.github.damontecres.stashapp.ui.getTheme(
                                    this@MainActivity,
                                    preferences.interfacePreferences.themeStyle,
                                    preferences.interfacePreferences.theme,
                                    isSystemInDarkTheme,
                                ),
                            )
                        }
                    val onChangeTheme = { name: String? ->
                        try {
                            colorScheme =
                                chooseColorScheme(
                                    preferences.interfacePreferences.themeStyle,
                                    isSystemInDarkTheme,
                                    if (name.isNullOrBlank() || name == "default") {
                                        defaultColorSchemeSet
                                    } else {
                                        readThemeJson(
                                            this@MainActivity,
                                            name,
                                        )
                                    },
                                )
                            Logger.i { "Updated theme" }
                        } catch (ex: Exception) {
                            Logger.e(ex) { "Exception changing theme" }
                            Toast
                                .makeText(
                                    this@MainActivity,
                                    "Error changing theme: ${ex.localizedMessage}",
                                    Toast.LENGTH_LONG,
                                ).show()
                        }
                    }

                    AppTheme(colorScheme = colorScheme) {
                        NavDisplay(
                            backStack = setupNavigationManager.backStack,
                            onBack = { setupNavigationManager.backStack.removeLastOrNull() },
                            entryDecorators =
                                listOf(
                                    rememberSaveableStateHolderNavEntryDecorator(),
                                    rememberViewModelStoreNavEntryDecorator(),
                                ),
                            entryProvider = { key ->
                                NavEntry(key) {
                                    SetupContent(
                                        destination = key,
                                        preferences = preferences,
                                        navigationManager = navigationManager,
                                        serverRepository = serverRepository,
                                        onChangeTheme = onChangeTheme,
                                        onCorrectPin = { viewModel.appStart(false) },
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.background),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_BACK_STACK = "backStack"
    }
}

@KoinViewModel
class MainViewModel(
    private val serverRepository: ServerRepository,
    private val setupNavigationManager: SetupNavigationManager,
    private val navigationManager: NavigationManager,
    private val preferences: DataStore<StashPreferences>,
) : ViewModel() {
    fun appStart(enforcePin: Boolean) {
        viewModelScope.launchDefault {
            Logger.d { "appState: enforcePin=$enforcePin" }
            val prefs = preferences.data.first()
            val hasPin = prefs.pinPreferences.pin.isNotNullOrBlank()
            val destination =
                if (hasPin && enforcePin) {
                    Logger.v { "Pin Required" }
                    SetupDestination.PinRequired
                } else {
                    val currentServer = serverRepository.currentServer.first().server
                    val restoredServer = serverRepository.restore()
                    if (currentServer != restoredServer) {
                        Logger.v { "A different server was restored" }
                        navigationManager.reloadMain()
                    }
                    if (restoredServer != null) {
                        Logger.v { "App content" }
                        SetupDestination.AppContent(restoredServer)
                    } else if (serverRepository.getAll().isEmpty()) {
                        Logger.v { "No servers found, starting initial setup" }
                        SetupDestination.InitialSetup
                    } else {
                        Logger.v { "Server list" }
                        SetupDestination.ServerList
                    }
                }
            setupNavigationManager.navigateTo(destination)
        }
    }
}
