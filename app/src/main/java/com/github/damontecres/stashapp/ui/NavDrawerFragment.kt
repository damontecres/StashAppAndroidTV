package com.github.damontecres.stashapp.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.tv.material3.MaterialTheme
import coil3.annotation.ExperimentalCoilApi
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.ui.components.server.InitialSetup
import com.github.damontecres.stashapp.ui.components.server.ManageServers
import com.github.damontecres.stashapp.ui.nav.ApplicationContent
import com.github.damontecres.stashapp.ui.nav.CoilConfig
import com.github.damontecres.stashapp.util.preferences
import com.github.damontecres.stashapp.views.models.ServerViewModel
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlin.time.ExperimentalTime

/**
 * Main fragment for the compose UI
 */
class NavDrawerFragment : Fragment(R.layout.compose_frame) {
    private val serverViewModel: ServerViewModel by activityViewModels()

    var navController: NavController<Destination>? = null

    @OptIn(ExperimentalCoilApi::class, ExperimentalTime::class)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        composeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val server by serverViewModel.currentServer.observeAsState()
                val preferences by context.preferences.data.collectAsState(null)
                CoilConfig(serverViewModel)
                val isSystemInDarkTheme = isSystemInDarkTheme()
                preferences?.let { preferences ->
                    var colorScheme by
                        remember {
                            mutableStateOf(
                                getTheme(
                                    requireContext(),
                                    preferences.interfacePreferences.themeStyle,
                                    preferences.interfacePreferences.theme,
                                    isSystemInDarkTheme,
                                ),
                            )
                        }
                    AppTheme(colorScheme = colorScheme) {
                        key(server) {
                            val navController = rememberNavController<Destination>(Destination.Main)
                            this@NavDrawerFragment.navController = navController
                            NavBackHandler(navController)
                            val navManager =
                                (serverViewModel.navigationManager as NavigationManagerCompose)
//                        navManager.controller = navController

                            val navCommand by serverViewModel.command.observeAsState()
                            LaunchedEffect(navCommand) {
                                navCommand?.let { cmd ->
                                    Log.v(TAG, "cmd=$cmd, server=$server")
                                    if (cmd.popUpToMain) {
                                        navController.popUpTo { it == Destination.Main }
                                    }
                                    navController.navigate(cmd.destination)
                                }
                            }
                            if (server == null && serverViewModel.destination.value is Destination.Setup) {
                                InitialSetup(
                                    onServerConfigure = { serverViewModel.switchServer(it) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else if (server == null &&
                                (navCommand?.destination is Destination.ManageServers || navCommand?.destination is Destination.Main)
                            ) {
                                ManageServers(
                                    currentServer = null,
                                    onSwitchServer = serverViewModel::switchServer,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                server?.let { currentServer ->
                                    Log.v(TAG, "currentServer=$currentServer")
                                    CompositionLocalProvider(
                                        LocalGlobalContext provides
                                            GlobalContext(
                                                currentServer,
                                                navManager,
                                                preferences,
                                            ),
                                    ) {
                                        ApplicationContent(
                                            server = currentServer,
                                            preferences = preferences,
                                            navigationManager = navManager,
                                            navController = navController,
                                            onSwitchServer = { serverViewModel.switchServer(it) },
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
                                                                    requireContext(),
                                                                    name,
                                                                )
                                                            },
                                                        )
                                                    Log.i(TAG, "Updated theme")
                                                } catch (ex: Exception) {
                                                    Log.e(TAG, "Exception changing theme", ex)
                                                    Toast
                                                        .makeText(
                                                            requireContext(),
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
    }

    companion object {
        const val TAG = "NavDrawerFragment"
    }
}
