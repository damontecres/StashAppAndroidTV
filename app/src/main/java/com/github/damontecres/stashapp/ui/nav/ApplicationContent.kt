package com.github.damontecres.stashapp.ui.nav

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import co.touchlab.kermit.Logger
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.di.server.CurrentServer
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.compat.isTvDevice
import com.github.damontecres.stashapp.ui.components.DefaultLongClicker
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.MarkerDurationDialog
import com.github.damontecres.stashapp.ui.pages.DialogParams
import com.github.damontecres.stashapp.util.PreferenceScreenOption

/**
 * Shows the actual compose content of the application
 *
 * This is a Navigation Drawer and its content or a full screen destination
 */
@Composable
fun ApplicationContent(
    currentServer: CurrentServer,
    preferences: StashPreferences,
    navigationManager: NavigationManager,
    onChangeTheme: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var composeUiConfig by remember(currentServer, preferences) {
        mutableStateOf(
            ComposeUiConfig.fromStashServer(
                preferences,
                currentServer.serverPreferences,
            ),
        )
    }

    val scrollToNextPage = preferences.interfacePreferences.scrollNextViewAll

    val itemOnClick =
        ItemOnClicker { item: Any, filterAndPosition ->
            when (item) {
                is FilterArgs -> {
                    navigationManager.navigate(
                        Destination.Filter(
                            item,
                            scrollToNextPage,
                        ),
                    )
                }

                is ImageData -> {
                    val (filter, position) = filterAndPosition!!
                    navigationManager.navigate(
                        Destination.Slideshow(
                            filter,
                            position,
                            false,
                        ),
                    )
                }

                is StashData -> {
                    navigationManager.navigate(
                        Destination.fromStashData(item),
                    )
                }

                else -> {
                    Toast
                        .makeText(
                            context,
                            "Unknown item. This is probably a bug",
                            Toast.LENGTH_SHORT,
                        ).show()
                    Logger.e { "Unknown item type: ${item::class.qualifiedName}" }
                }
            }
        }

    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var showMarkerDialog by remember { mutableStateOf<FilterAndPosition?>(null) }
    val longClicker =
        remember {
            DefaultLongClicker(
                navigationManager,
                itemOnClick,
                currentServer.serverPreferences.alwaysStartFromBeginning,
                markerPlayAllOnClick = { showMarkerDialog = it },
            ) { dialogParams = it }
        }

    val pages =
        buildList {
            add(DrawerPage.SearchPage)
            add(DrawerPage.HomePage)
            addAll(
                DataType.entries
                    .filter { it in currentServer.serverPreferences.menuItems }
                    .map { DrawerPage.DataTypePage(it) },
            )
            add(DrawerPage.SettingPage)
        }
    val defaultSelection: DrawerPage = DrawerPage.HomePage
    var selectedScreen by rememberSaveable { mutableStateOf<DrawerPage?>(defaultSelection) }

    NavDisplay(
        backStack = navigationManager.backStack,
        onBack = { navigationManager.goBack() },
        modifier = modifier,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider = { destination ->
            NavEntry(destination, contentKey = destination.toString()) {
                LaunchedEffect(Unit) {
                    // TODO Refresh server preferences on each page change
                }
                val fullScreen =
                    if (isTvDevice) destination.fullScreen else destination.fullScreenTouch

                if (fullScreen) {
                    DestinationContent(
                        currentServer = currentServer,
                        preferences = preferences,
                        navManager = navigationManager,
                        destination = destination,
                        composeUiConfig = composeUiConfig,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        onChangeTheme = onChangeTheme,
                        modifier = Modifier.fillMaxSize(),
                        onUpdateTitle = null,
                    )
                } else {
                    // Highlight on the nav drawer as user navigates around the app
                    selectedScreen =
                        when (destination) {
                            is Destination.Main -> {
                                DrawerPage.HomePage
                            }

                            Destination.Search -> {
                                DrawerPage.SearchPage
                            }

                            Destination.SettingsPin,
                            is Destination.Settings,
                            -> {
                                DrawerPage.SettingPage
                            }

                            is Destination.Item -> {
                                pages.firstOrNull {
                                    it is DrawerPage.DataTypePage && it.dataType == destination.dataType
                                }
                            }

                            is Destination.MarkerDetails -> {
                                pages.firstOrNull {
                                    it is DrawerPage.DataTypePage && it.dataType == DataType.MARKER
                                }
                            }

                            is Destination.Filter -> {
                                pages.firstOrNull {
                                    it is DrawerPage.DataTypePage && it.dataType == destination.filterArgs.dataType
                                }
                            }

                            else -> {
                                null
                            }
                        }

                    val onSelectScreen = { page: DrawerPage ->
                        val refreshMain =
                            selectedScreen == DrawerPage.HomePage && page == DrawerPage.HomePage
                        Logger.v { "Navigating to $page" }
                        selectedScreen = page
                        if (refreshMain) {
                            navigationManager.goToMain()
                        } else {
                            val pageDest =
                                when (page) {
                                    DrawerPage.HomePage -> {
                                        val id =
                                            (navigationManager.backStack.firstOrNull() as? Destination.Main)?.id
                                                ?: 0
                                        Destination.Main(id + 1)
                                    }

                                    DrawerPage.SearchPage -> {
                                        Destination.Search
                                    }

                                    DrawerPage.SettingPage -> {
                                        if (composeUiConfig.readOnlyModeDisabled) {
                                            Destination.Settings(
                                                PreferenceScreenOption.BASIC,
                                            )
                                        } else {
                                            Destination.SettingsPin
                                        }
                                    }

                                    is DrawerPage.DataTypePage -> {
                                        Destination.Filter(
                                            currentServer.serverPreferences.defaultFilters[page.dataType]!!,
                                        )
                                    }
                                }
                            navigationManager.navigateToFromDrawer(pageDest)
                        }
                    }

                    if (isTvDevice) {
                        NavDrawer(
                            preferences = preferences,
                            currentServer = currentServer,
                            navigationManager = navigationManager,
                            composeUiConfig = composeUiConfig,
                            destination = destination,
                            selectedScreen = selectedScreen,
                            pages = pages,
                            itemOnClick = itemOnClick,
                            longClicker = longClicker,
                            onSelectScreen = onSelectScreen,
                            onChangeTheme = onChangeTheme,
                            modifier = Modifier,
                        )
                    } else {
                        NavScaffold(
                            preferences = preferences,
                            currentServer = currentServer,
                            navigationManager = navigationManager,
                            composeUiConfig = composeUiConfig,
                            destination = destination,
                            selectedScreen = selectedScreen,
                            pages = pages,
                            itemOnClick = itemOnClick,
                            longClicker = longClicker,
                            onSelectScreen = onSelectScreen,
                            onChangeTheme = onChangeTheme,
                            modifier = Modifier,
                        )
                    }
                }
                dialogParams?.let { params ->
                    DialogPopup(
                        showDialog = true,
                        title = params.title,
                        dialogItems = params.items,
                        onDismissRequest = { dialogParams = null },
                        dismissOnClick = true,
                        waitToLoad = true,
                        properties = DialogProperties(),
                    )
                }
                if (showMarkerDialog != null) {
                    MarkerDurationDialog(
                        onDismissRequest = { showMarkerDialog = null },
                        onClick = {
                            showMarkerDialog?.let { filterAndPosition ->
                                val dest =
                                    Destination.Playlist(
                                        filterAndPosition.filter,
                                        filterAndPosition.position,
                                        it,
                                    )
                                navigationManager.navigate(dest)
                            }
                            showMarkerDialog = null
                        },
                    )
                }
            }
        },
    )
}
