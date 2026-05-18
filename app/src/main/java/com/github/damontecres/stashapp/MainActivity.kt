package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.di.AuthHttpClient
import com.github.damontecres.stashapp.di.server.CurrentServer
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.NavDrawerFragment.Companion.TAG
import com.github.damontecres.stashapp.ui.chooseColorScheme
import com.github.damontecres.stashapp.ui.defaultColorSchemeSet
import com.github.damontecres.stashapp.ui.nav.ApplicationContent
import com.github.damontecres.stashapp.ui.nav.CoilConfig
import com.github.damontecres.stashapp.ui.readThemeJson
import com.github.damontecres.stashapp.util.launchDefault
import com.github.damontecres.stashapp.util.preferences
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val navigationManager: NavigationManager by inject()
    private val serverRepository: ServerRepository by inject()

    @AuthHttpClient
    private val httpClient: OkHttpClient = get()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchDefault {
            if (serverRepository.restore()) {
                // Success
            } else {
                // TODO
                navigationManager.navigate(Destination.Setup)
            }
        }
        setContent {
            val preferences by this@MainActivity.preferences.data.collectAsState(null)
            preferences?.let { preferences ->
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
                    val currentServer by serverRepository.currentServer.collectAsState()
                    if (currentServer != CurrentServer.UNSET) {
                        key(currentServer) {
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
                                        Log.i(TAG, "Updated theme")
                                    } catch (ex: Exception) {
                                        Log.e(TAG, "Exception changing theme", ex)
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
