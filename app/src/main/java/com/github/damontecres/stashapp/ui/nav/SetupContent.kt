package com.github.damontecres.stashapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.navigation.SetupDestination
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.GlobalContext
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.components.LoadingPage
import com.github.damontecres.stashapp.ui.components.server.InitialSetup
import com.github.damontecres.stashapp.ui.components.server.ManageServers

@Composable
fun SetupContent(
    destination: SetupDestination,
    preferences: StashPreferences,
    navigationManager: NavigationManager,
    serverRepository: ServerRepository,
    onChangeTheme: (String?) -> Unit,
    onCorrectPin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        SetupDestination.InitialSetup -> {
            InitialSetup(modifier)
        }

        SetupDestination.Loading -> {
            LoadingPage(modifier)
        }

        SetupDestination.ServerList -> {
            ManageServers(
                modifier = modifier,
            )
        }

        is SetupDestination.AppContent -> {
            val currentServer by serverRepository.currentServer.collectAsState()
            if (currentServer.server == destination.server) {
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
                        onChangeTheme = onChangeTheme,
                        modifier = modifier,
                    )
                }
            } else {
                LoadingPage(modifier)
            }
        }
    }
}
