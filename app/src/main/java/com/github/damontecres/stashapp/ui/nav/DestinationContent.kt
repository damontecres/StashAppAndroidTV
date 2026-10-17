package com.github.damontecres.stashapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.di.server.CurrentServer
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.filter.CreateFilterScreen
import com.github.damontecres.stashapp.ui.components.server.ManageServers
import com.github.damontecres.stashapp.ui.pages.ChooseThemePage
import com.github.damontecres.stashapp.ui.pages.DebugPage
import com.github.damontecres.stashapp.ui.pages.FilterPage
import com.github.damontecres.stashapp.ui.pages.GalleryPage
import com.github.damontecres.stashapp.ui.pages.GroupPage
import com.github.damontecres.stashapp.ui.pages.ImagePage
import com.github.damontecres.stashapp.ui.pages.LicenseInfoPage
import com.github.damontecres.stashapp.ui.pages.MainPage
import com.github.damontecres.stashapp.ui.pages.MarkerPage
import com.github.damontecres.stashapp.ui.pages.MarkerTimestampPage
import com.github.damontecres.stashapp.ui.pages.PerformerPage
import com.github.damontecres.stashapp.ui.pages.PlaybackPage
import com.github.damontecres.stashapp.ui.pages.PlaylistPlaybackPage
import com.github.damontecres.stashapp.ui.pages.SceneDetailsPage
import com.github.damontecres.stashapp.ui.pages.SearchPage
import com.github.damontecres.stashapp.ui.pages.SettingsPage
import com.github.damontecres.stashapp.ui.pages.SettingsPinPage
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
    modifier: Modifier = Modifier,
) {
    when (destination) {
        is Destination.SettingsPin -> {
            SettingsPinPage(
                navigationManager = navManager,
                preferences = preferences,
                modifier = modifier,
            )
        }

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
                modifier = modifier,
            )
        }

        is Destination.ManageServers -> {
            ManageServers(
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
                modifier = modifier,
            )
        }

        is Destination.UpdateMarker -> {
            MarkerTimestampPage(
                uiConfig = composeUiConfig,
                markerId = destination.markerId,
                modifier = modifier,
            )
        }

        is Destination.Debug -> {
            DebugPage(
                currentServer = currentServer,
                uiConfig = composeUiConfig,
                modifier = modifier,
            )
        }

        is Destination.Main -> {
            MainPage(
                destination = destination,
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
                modifier = modifier,
            )
        }

        is Destination.Item -> {
            when (destination.dataType) {
                DataType.SCENE -> {
                    SceneDetailsPage(
                        modifier = modifier,
                        sceneId = destination.id,
                        uiConfig = composeUiConfig,
                    )
                }

                DataType.PERFORMER -> {
                    PerformerPage(
                        modifier = modifier,
                        id = destination.id,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                    )
                }

                DataType.TAG -> {
                    TagPage(
                        modifier = modifier,
                        id = destination.id,
                        includeSubTags = false,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                    )
                }

                DataType.STUDIO -> {
                    StudioPage(
                        modifier = modifier,
                        id = destination.id,
                        includeSubStudios = false,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                    )
                }

                DataType.GALLERY -> {
                    GalleryPage(
                        modifier = modifier,
                        id = destination.id,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                    )
                }

                DataType.GROUP -> {
                    GroupPage(
                        modifier = modifier,
                        id = destination.id,
                        includeSubGroups = false,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                    )
                }

                DataType.MARKER -> {
                    MarkerPage(
                        uiConfig = composeUiConfig,
                        markerId = destination.id,
                        modifier = modifier,
                    )
                }

                DataType.IMAGE -> {
                    throw IllegalArgumentException("Image not supported in Destination.Item")
                }
            }
        }

        Destination.LicenseInfo -> {
            LicenseInfoPage(modifier)
        }
    }
}
