package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.DataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavBackStack
import androidx.tv.material3.MaterialTheme
import co.touchlab.kermit.Logger
import com.github.damontecres.stashapp.di.AuthHttpClient
import com.github.damontecres.stashapp.di.server.CurrentServer
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.GlobalContext
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.chooseColorScheme
import com.github.damontecres.stashapp.ui.components.LoadingPage
import com.github.damontecres.stashapp.ui.defaultColorSchemeSet
import com.github.damontecres.stashapp.ui.nav.ApplicationContent
import com.github.damontecres.stashapp.ui.nav.CoilConfig
import com.github.damontecres.stashapp.ui.pages.PinEntryPage
import com.github.damontecres.stashapp.ui.readThemeJson
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.launchDefault
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.qualifier
import timber.log.Timber

class MainActivity : AppCompatActivity() {
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
    }

    override fun onResume() {
        super.onResume()
        Logger.i { "onResume" }
        setContent {
            LoadingPage(Modifier.fillMaxSize())
        }

        lifecycleScope.launchDefault {
            val prefs = preferences.data.first()
            val hasPin = prefs.pinPreferences.pin.isNotNullOrBlank()
            if (serverRepository.restore()) {
                // Success
                showContent(hasPin)
            } else {
                // TODO
                navigationManager.navigate(Destination.Setup)
            }
        }
    }

    fun showContent(hasPin: Boolean) {
        Logger.i { "showContent: hasPin=$hasPin" }
        setContent {
            val preferences by preferences.data.collectAsState(null)
            preferences?.let { preferences ->
                var pinActive by remember(hasPin) { mutableStateOf(hasPin) }
                val isSystemInDarkTheme = isSystemInDarkTheme()
                CoilConfig(httpClient, preferences)
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
                AppTheme(colorScheme = colorScheme) {
                    if (pinActive) {
                        PinEntryPage(
                            requiredPin = preferences.pinPreferences.pin,
                            title = stringResource(R.string.enter_pin),
                            onCorrectPin = { pinActive = false },
                            preventBack = true,
                            autoSubmit = preferences.pinPreferences.autoSubmit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val currentServer by serverRepository.currentServer.collectAsState()
                        if (currentServer != CurrentServer.UNSET) {
                            key(currentServer) {
                                CompositionLocalProvider(
                                    LocalGlobalContext provides
                                        GlobalContext(
                                            currentServer,
                                            navigationManager,
                                            preferences,
                                        ),
                                ) {
                                    ApplicationContent(
                                        currentServer = currentServer,
                                        preferences = preferences,
                                        navigationManager = navigationManager,
                                        onSwitchServer = {
                                            TODO()
                                        },
                                        onChangeTheme = { name ->
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
                                        },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.background),
                                        // TODO could use onKeyEvent here to make focus/movement sounds everywhere
                                        // But it wouldn't know if the focus would actually change
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_BACK_STACK = "backStack"
    }
}
