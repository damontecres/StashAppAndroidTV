package com.github.damontecres.stashapp.ui.nav

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.preference.PreferenceManager
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import com.github.damontecres.stashapp.PreferenceScreenOption
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalPlayerContext
import com.github.damontecres.stashapp.ui.NavDrawerFragment.Companion.TAG
import com.github.damontecres.stashapp.ui.PlayerContext
import com.github.damontecres.stashapp.ui.compat.isNotTvDevice
import com.github.damontecres.stashapp.ui.components.DefaultLongClicker
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.MarkerDurationDialog
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.ui.pages.DialogParams
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.ui.util.playOnClickSound
import com.github.damontecres.stashapp.ui.util.playSoundOnFocus
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavHost
import kotlinx.coroutines.launch

/**
 * Shows the actual compose content of the application
 *
 * This is a Navigation Drawer and its content or a full screen destination
 */
@Composable
fun ApplicationContent(
    server: StashServer,
    navigationManager: NavigationManagerCompose,
    navController: NavController<Destination>,
    onChangeTheme: (String?) -> Unit,
    onSwitchServer: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var composeUiConfig by remember(server) {
        mutableStateOf(
            ComposeUiConfig.fromStashServer(
                context,
                server,
            ),
        )
    }

    val scrollToNextPage =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getBoolean("scrollToNextResult", true)

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
                    Log.e(TAG, "Unknown item type: ${item::class.qualifiedName}")
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
                server.serverPreferences.alwaysStartFromBeginning,
                markerPlayAllOnClick = { showMarkerDialog = it },
            ) { dialogParams = it }
        }

    val pages =
        buildList {
            add(DrawerPage.SearchPage)
            add(DrawerPage.HomePage)
            addAll(
                DataType.entries
                    .filter { server.serverPreferences.showMenuItem(it) }
                    .map { DrawerPage.DataTypePage(it) },
            )
            add(DrawerPage.SettingPage)
        }
    val visiblePages = remember { mutableMapOf<DrawerPage, Boolean>() }

    val initialFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val defaultSelection: DrawerPage = DrawerPage.HomePage
    var selectedScreen by rememberSaveable { mutableStateOf<DrawerPage?>(defaultSelection) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    NavHost(navController, modifier = modifier) { destination ->
        LaunchedEffect(Unit) {
            // Refresh server preferences on each page change
            navigationManager.serverViewModel.updateServerPreferences()
            composeUiConfig = ComposeUiConfig.fromStashServer(context, server)

            navigationManager.previousDestination = destination
            navigationManager.serverViewModel.setCurrentDestination(destination)
        }

        if (destination.fullScreen) {
            FullScreenContent(
                server = server,
                destination = destination,
                navigationManager = navigationManager,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                composeUiConfig = composeUiConfig,
                onChangeTheme = onChangeTheme,
                onSwitchServer = onSwitchServer,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Highlight on the nav drawer as user navigates around the app
            selectedScreen =
                when (destination) {
                    Destination.Main -> DrawerPage.HomePage

                    Destination.Search -> DrawerPage.SearchPage

                    Destination.SettingsPin,
                    is Destination.Settings,
                    -> DrawerPage.SettingPage

                    is Destination.Item ->
                        pages.firstOrNull {
                            it is DrawerPage.DataTypePage && it.dataType == destination.dataType
                        }

                    is Destination.MarkerDetails ->
                        pages.firstOrNull {
                            it is DrawerPage.DataTypePage && it.dataType == DataType.MARKER
                        }

                    is Destination.Filter ->
                        pages.firstOrNull {
                            it is DrawerPage.DataTypePage && it.dataType == destination.filterArgs.dataType
                        }

                    else -> null
                }

            // If the page is not currently visible, scroll the list so that it is
            LaunchedEffect(selectedScreen) {
                if (visiblePages[selectedScreen] == false) {
                    listState.animateScrollToItem(pages.indexOf(selectedScreen))
                }
            }

            val drawerFocusRequester = remember { FocusRequester() }
            BackHandler(enabled = (drawerState.currentValue == DrawerValue.Closed && destination == Destination.Main)) {
                drawerState.setValue(DrawerValue.Open)
                drawerFocusRequester.requestFocus()
            }
            // TODO should each "root" page have a separate back stack so pressing back on the "root" scene filter page does:
            // 1. opens drawer (instead of going back to main), 2. back again goes to main
            var serverFocused by remember { mutableStateOf(false) }
            NavigationDrawer(
                modifier =
                    Modifier
                        .focusRequester(drawerFocusRequester),
                drawerState = drawerState,
                drawerContent = {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.background)
                                .focusGroup()
                                .focusProperties {
                                    onExit = {
                                        val selectedIndex = pages.indexOf(selectedScreen)
                                        if (selectedIndex !in listState.layoutInfo.visibleItemsInfo.map { it.index }) {
                                            scope.launch(StashCoroutineExceptionHandler()) {
                                                listState.animateScrollToItem(selectedIndex)
                                            }
                                        }
                                    }
                                    onEnter = {
                                        initialFocus.tryRequestFocus()
                                    }
                                }
//                                    .focusRestorer(initialFocus)
                                .selectableGroup()
                                .ifElse(
                                    isNotTvDevice,
                                    Modifier.clickable(true) {
                                        if (drawerState.currentValue == DrawerValue.Open) {
                                            drawerState.setValue(DrawerValue.Closed)
                                        } else {
                                            drawerState.setValue(DrawerValue.Open)
                                        }
                                    },
                                ),
                    ) {
                        item {
                            val onClick = {
                                if (composeUiConfig.playSoundOnFocus) {
                                    playOnClickSound(
                                        context,
                                    )
                                }
                                navigationManager.navigate(
                                    Destination.ManageServers(
                                        false,
                                    ),
                                )
                            }
                            NavigationDrawerItem(
                                modifier =
                                    Modifier
                                        .onFocusChanged {
                                            serverFocused = it.isFocused
                                        }.playSoundOnFocus(composeUiConfig.playSoundOnFocus),
                                selected = false,
                                onClick = onClick,
                                leadingContent = {
                                    Icon(
                                        painterResource(id = R.mipmap.stash_logo),
                                        contentDescription = null,
                                    )
                                },
                            ) {
                                Text(
                                    modifier =
                                        Modifier
                                            .enableMarquee(serverFocused)
                                            .ifElse(
                                                isNotTvDevice,
                                                Modifier.clickable(onClick = onClick),
                                            ),
                                    text = server.url,
                                    maxLines = 1,
                                )
                            }
                        }
                        items(
                            pages,
                            key = null,
                        ) { page ->
                            val onClick = {
                                if (composeUiConfig.playSoundOnFocus) {
                                    playOnClickSound(
                                        context,
                                    )
                                }
                                val refreshMain =
                                    selectedScreen == DrawerPage.HomePage && page == DrawerPage.HomePage
                                selectedScreen = page

                                drawerState.setValue(DrawerValue.Closed)
                                Log.v(
                                    TAG,
                                    "Navigating to $page",
                                )
                                if (refreshMain) {
                                    navigationManager.goToMain()
                                } else {
                                    val pageDest =
                                        when (page) {
                                            DrawerPage.HomePage -> Destination.Main
                                            DrawerPage.SearchPage -> Destination.Search
                                            DrawerPage.SettingPage ->
                                                if (composeUiConfig.readOnlyModeDisabled) {
                                                    Destination.Settings(
                                                        PreferenceScreenOption.BASIC,
                                                    )
                                                } else {
                                                    Destination.SettingsPin
                                                }

                                            is DrawerPage.DataTypePage ->
                                                Destination.Filter(
                                                    server.serverPreferences.getDefaultFilter(
                                                        page.dataType,
                                                    ),
                                                )
                                        }
                                    navigationManager.navigateFromNavDrawer(pageDest)
                                }
                            }
                            NavDrawerListItem(
                                page = page,
                                selectedScreen = selectedScreen,
                                initialFocus = initialFocus,
                                composeUiConfig = composeUiConfig,
                                drawerOpen = drawerState.currentValue == DrawerValue.Open,
                                onClick = onClick,
                                onVisible = { visiblePages[page] = it },
                                modifier = Modifier,
                            )
                        }
                    }
                },
            ) {
                CompositionLocalProvider(
                    LocalPlayerContext provides PlayerContext,
                ) {
                    NavDrawerContent(
                        navManager = navigationManager,
                        server = server,
                        destination = destination,
                        composeUiConfig = composeUiConfig,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                    )
                }
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
}
