package com.github.damontecres.stashapp.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.filter.CreateFilterScreen
import com.github.damontecres.stashapp.ui.components.server.ManageServers
import com.github.damontecres.stashapp.ui.pages.ChooseThemePage
import com.github.damontecres.stashapp.ui.pages.DebugPage
import com.github.damontecres.stashapp.ui.pages.ImagePage
import com.github.damontecres.stashapp.ui.pages.MarkerTimestampPage
import com.github.damontecres.stashapp.ui.pages.PlaybackPage
import com.github.damontecres.stashapp.ui.pages.PlaylistPlaybackPage
import com.github.damontecres.stashapp.ui.pages.SettingsPage
import com.github.damontecres.stashapp.util.StashServer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun FullScreenContent(
    server: StashServer,
    destination: Destination,
    navigationManager: NavigationManagerCompose,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    composeUiConfig: ComposeUiConfig,
    onChangeTheme: (String?) -> Unit,
    onSwitchServer: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        is Destination.Pin -> throw UnsupportedOperationException("Destination.Pin")
//                    PinEntryPage(
//                        pinPreference = R.string.pref_key_pin_code,
//                        title = stringResource(R.string.enter_pin),
//                        onCorrectPin = {
//                            if (navigationManager.controller.backstack.entries.size > 1) {
//                                navigationManager.goBack()
//                            } else {
//                                navigationManager.goToMain()
//                            }
//                        },
//                        preventBack = true,
//                        modifier = Modifier.fillMaxSize(),
//                    )

        is Destination.SettingsPin -> throw UnsupportedOperationException("Destination.SettingsPin")
//                    PinEntryPage(
//                        pinPreference = R.string.pref_key_read_only_mode_pin,
//                        title = stringResource(R.string.enter_settings_pin),
//                        onCorrectPin = {
//                            // Pop Destination.SettingsPin off the stack and go to settings
//                            // This prevents showing the PIN entry again when going back from settings
//                            navigationManager.goBack()
//                            navigationManager.navigate(Destination.Settings(PreferenceScreenOption.BASIC))
//                        },
//                        preventBack = false,
//                        modifier = Modifier.fillMaxSize(),
//                    )

        is Destination.Settings -> {
            SettingsPage(
                server = server,
                navigationManager = navigationManager,
                preferenceScreenOption = destination.screenOption,
                uiConfig = composeUiConfig,
                modifier = modifier,
            )
        }

        is Destination.ManageServers -> {
            ManageServers(
                currentServer = server,
                onSwitchServer = onSwitchServer,
                modifier = modifier,
            )
        }

        is Destination.Playback -> {
            PlaybackPage(
                server = server,
                sceneId = destination.sceneId,
                startPosition = destination.position,
                playbackMode = destination.mode,
                uiConfig = composeUiConfig,
                itemOnClick = itemOnClick,
                modifier = modifier,
            )
        }

        is Destination.Playlist -> {
            PlaylistPlaybackPage(
                server = server,
                uiConfig = composeUiConfig,
                filterArgs = destination.filterArgs,
                startIndex = destination.position,
                clipDuration = destination.duration?.milliseconds ?: 30.seconds,
                itemOnClick = itemOnClick,
                modifier = modifier,
            )
        }

        is Destination.Slideshow -> {
            ImagePage(
                server = server,
                navigationManager = navigationManager,
                filter = destination.filterArgs,
                startPosition = destination.position,
                startSlideshow = destination.automatic,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                uiConfig = composeUiConfig,
                modifier = modifier,
            )
        }

        Destination.ChooseTheme ->
            ChooseThemePage(
                server = server,
                navigationManager = navigationManager,
                uiConfig = composeUiConfig,
                onChooseTheme = onChangeTheme,
                modifier = modifier,
            )

        is Destination.CreateFilter ->
            CreateFilterScreen(
                uiConfig = composeUiConfig,
                dataType = destination.dataType,
                initialFilter = destination.startingFilter,
                navigationManager = navigationManager,
                modifier = modifier,
            )

        is Destination.UpdateMarker ->
            MarkerTimestampPage(
                server = server,
                navigationManager = navigationManager,
                uiConfig = composeUiConfig,
                markerId = destination.markerId,
                modifier = Modifier.fillMaxSize(),
            )

        is Destination.Debug ->
            DebugPage(
                server = server,
                uiConfig = composeUiConfig,
                modifier = Modifier.fillMaxSize(),
            )

        else -> FragmentView(navigationManager, destination)
    }
}
