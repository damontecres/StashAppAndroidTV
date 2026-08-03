package com.github.damontecres.stashapp.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.navigation.SetupDestination
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.GlobalContext
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.components.LoadingPage
import com.github.damontecres.stashapp.ui.components.server.InitialSetup
import com.github.damontecres.stashapp.ui.components.server.ManageServers
import com.github.damontecres.stashapp.ui.pages.PinEntryPage

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
                onUpdateTitle = {},
                modifier = modifier,
            )
        }

        SetupDestination.PinRequired -> {
            PinEntryPage(
                requiredPin = preferences.pinPreferences.pin,
                title = stringResource(R.string.enter_pin),
                onCorrectPin = onCorrectPin,
                preventBack = true,
                autoSubmit = preferences.pinPreferences.autoSubmit,
                modifier = Modifier.fillMaxSize(),
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
                        navigationManager = navigationManager,
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
