package com.github.damontecres.stashapp.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.di.server.CurrentServer
import com.github.damontecres.stashapp.di.server.StashServer
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.filter.CreateFilterScreen
import com.github.damontecres.stashapp.ui.components.server.InitialSetup
import com.github.damontecres.stashapp.ui.components.server.ManageServers
import com.github.damontecres.stashapp.ui.pages.ChooseThemePage
import com.github.damontecres.stashapp.ui.pages.DebugPage
import com.github.damontecres.stashapp.ui.pages.FilterPage
import com.github.damontecres.stashapp.ui.pages.GalleryPage
import com.github.damontecres.stashapp.ui.pages.GroupPage
import com.github.damontecres.stashapp.ui.pages.ImagePage
import com.github.damontecres.stashapp.ui.pages.MainPage
import com.github.damontecres.stashapp.ui.pages.MarkerPage
import com.github.damontecres.stashapp.ui.pages.MarkerTimestampPage
import com.github.damontecres.stashapp.ui.pages.PerformerPage
import com.github.damontecres.stashapp.ui.pages.PlaybackPage
import com.github.damontecres.stashapp.ui.pages.PlaylistPlaybackPage
import com.github.damontecres.stashapp.ui.pages.SceneDetailsPage
import com.github.damontecres.stashapp.ui.pages.SearchPage
import com.github.damontecres.stashapp.ui.pages.SettingsPage
import com.github.damontecres.stashapp.ui.pages.StudioPage
import com.github.damontecres.stashapp.ui.pages.TagPage
import com.github.damontecres.stashapp.ui.pages.UpdateAppPage
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Composable function to display the content of a destination independent of full screen/nav drawer/scaffold
 */
@Composable
fun DestinationContent(
    preferences: StashPreferences,
    currentServer: CurrentServer,
    navManager: NavigationManager,
    destination: Destination,
    composeUiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    onChangeTheme: (String?) -> Unit,
    onSwitchServer: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
    onUpdateTitle: ((AnnotatedString) -> Unit)? = null,
) {
    when (destination) {
        is Destination.Pin -> {
            throw UnsupportedOperationException("Destination.Pin")
        }

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

        is Destination.SettingsPin -> {
            throw UnsupportedOperationException("Destination.SettingsPin")
        }

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

        is Destination.UpdateApp -> {
            UpdateAppPage(
                composeUiConfig = composeUiConfig,
                navigationManager = navManager,
                modifier = modifier,
            )
        }

        is Destination.Settings -> {
            SettingsPage(
                preferenceScreenOption = destination.screenOption,
                uiConfig = composeUiConfig,
                onUpdateTitle = onUpdateTitle,
                modifier = modifier,
            )
        }

        is Destination.ManageServers -> {
            ManageServers(
                onUpdateTitle = onUpdateTitle,
                modifier = modifier,
            )
        }

        is Destination.Playback -> {
            PlaybackPage(
                preferences = preferences,
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
                preferences = preferences,
                currentServer = currentServer,
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
                currentServer = currentServer,
                filter = destination.filterArgs,
                startPosition = destination.position,
                startSlideshow = destination.automatic,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                uiConfig = composeUiConfig,
                modifier = modifier,
            )
        }

        Destination.ChooseTheme -> {
            ChooseThemePage(
                navigationManager = navManager,
                uiConfig = composeUiConfig,
                onChooseTheme = onChangeTheme,
                modifier = modifier,
            )
        }

        is Destination.CreateFilter -> {
            CreateFilterScreen(
                uiConfig = composeUiConfig,
                dataType = destination.dataType,
                initialFilter = destination.startingFilter,
                onUpdateTitle = onUpdateTitle,
                modifier = modifier,
            )
        }

        is Destination.UpdateMarker -> {
            MarkerTimestampPage(
                uiConfig = composeUiConfig,
                markerId = destination.markerId,
                modifier = Modifier.fillMaxSize(),
            )
        }

        is Destination.Debug -> {
            DebugPage(
                currentServer = currentServer,
                uiConfig = composeUiConfig,
                modifier = Modifier.fillMaxSize(),
            )
        }

        is Destination.Main -> {
            MainPage(
                uiConfig = composeUiConfig,
                longClicker = longClicker,
                modifier = modifier,
            )
        }

        is Destination.Filter -> {
            FilterPage(
                initialFilter = destination.filterArgs,
                scrollToNextPage = destination.scrollToNextPage,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                uiConfig = composeUiConfig,
                onUpdateTitle = onUpdateTitle,
                modifier = modifier,
            )
        }

        is Destination.Search -> {
            SearchPage(
                uiConfig = composeUiConfig,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                modifier = modifier,
            )
        }

        is Destination.MarkerDetails -> {
            MarkerPage(
                uiConfig = composeUiConfig,
                markerId = destination.markerId,
                onUpdateTitle = onUpdateTitle,
            )
        }

        is Destination.Item -> {
            when (destination.dataType) {
                DataType.SCENE -> {
                    SceneDetailsPage(
                        modifier = modifier,
                        sceneId = destination.id,
                        uiConfig = composeUiConfig,
                        onUpdateTitle = onUpdateTitle,
                    )
                }

                DataType.PERFORMER -> {
                    PerformerPage(
                        modifier = modifier,
                        id = destination.id,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                        onUpdateTitle = onUpdateTitle,
                    )
                }

                DataType.TAG -> {
                    TagPage(
                        modifier = modifier,
                        id = destination.id,
                        includeSubTags = false,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                        onUpdateTitle = onUpdateTitle,
                    )
                }

                DataType.STUDIO -> {
                    StudioPage(
                        modifier = modifier,
                        id = destination.id,
                        includeSubStudios = false,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                        onUpdateTitle = onUpdateTitle,
                    )
                }

                DataType.GALLERY -> {
                    GalleryPage(
                        modifier = modifier,
                        id = destination.id,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                        onUpdateTitle = onUpdateTitle,
                    )
                }

                DataType.GROUP -> {
                    GroupPage(
                        modifier = modifier,
                        id = destination.id,
                        includeSubGroups = false,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                        onUpdateTitle = onUpdateTitle,
                    )
                }

                DataType.MARKER -> {
                    MarkerPage(
                        uiConfig = composeUiConfig,
                        markerId = destination.id,
                        onUpdateTitle = onUpdateTitle,
                    )
                }

                DataType.IMAGE -> {
                    throw IllegalArgumentException("Image not supported in Destination.Item")
                }
            }
        }

        Destination.LicenseInfo -> {
            TODO()
        }

        Destination.Setup -> {
            InitialSetup(modifier)
        }
    }
}
