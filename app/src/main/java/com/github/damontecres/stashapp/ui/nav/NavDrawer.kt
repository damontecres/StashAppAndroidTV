package com.github.damontecres.stashapp.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.di.server.CurrentServer
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.server.StashServer
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.di.services.PlayerFactory
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalPlayerContext
import com.github.damontecres.stashapp.ui.PlayerContext
import com.github.damontecres.stashapp.ui.compat.isNotTvDevice
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.ui.util.playOnClickSound
import com.github.damontecres.stashapp.ui.util.playSoundOnFocus
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class NavDrawerViewModel(
    private val serverRepository: ServerRepository,
    private val playerFactory: PlayerFactory,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val player =
        serverRepository.currentServer.mapLatest {
            playerFactory.createPlayerForCard(it.server)
        }
}

@Composable
fun NavDrawer(
    preferences: StashPreferences,
    currentServer: CurrentServer,
    navigationManager: NavigationManager,
    composeUiConfig: ComposeUiConfig,
    destination: Destination,
    selectedScreen: DrawerPage?,
    pages: List<DrawerPage>,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    onSelectScreen: (DrawerPage) -> Unit,
    onChangeTheme: (String?) -> Unit,
    onSwitchServer: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NavDrawerViewModel = koinViewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val visiblePages = remember { mutableMapOf<DrawerPage, Boolean>() }
    val initialFocus = remember { FocusRequester() }

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

    val serverUrlInteractionSource = remember { MutableInteractionSource() }
    val serverFocused = serverUrlInteractionSource.collectIsFocusedAsState().value

    val player by viewModel.player.collectAsState(null)
    CompositionLocalProvider(
        LocalPlayerContext provides PlayerContext(player),
    ) {
        NavigationDrawer(
            modifier =
                modifier
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
                                    .playSoundOnFocus(composeUiConfig.playSoundOnFocus),
                            selected = false,
                            onClick = onClick,
                            leadingContent = {
                                Icon(
                                    painterResource(id = R.drawable.stash_logo),
                                    contentDescription = null,
                                )
                            },
                            interactionSource = serverUrlInteractionSource,
                        ) {
                            Text(
                                modifier =
                                    Modifier
                                        .enableMarquee(serverFocused)
                                        .ifElse(
                                            isNotTvDevice,
                                            Modifier.clickable(onClick = onClick),
                                        ),
                                text = currentServer.server.url,
                                maxLines = 1,
                            )
                        }
                    }
                    items(
                        pages,
                        key = null,
                    ) { page ->
                        NavDrawerListItem(
                            page = page,
                            selectedScreen = selectedScreen,
                            initialFocus = initialFocus,
                            composeUiConfig = composeUiConfig,
                            drawerOpen = drawerState.currentValue == DrawerValue.Open,
                            onClick = {
                                if (composeUiConfig.playSoundOnFocus) {
                                    playOnClickSound(
                                        context,
                                    )
                                }
                                drawerState.setValue(DrawerValue.Closed)
                                onSelectScreen(page)
                            },
                            onVisible = { visiblePages[page] = it },
                            modifier = Modifier,
                        )
                    }
                }
            },
        ) {
            DestinationContent(
                preferences = preferences,
                currentServer = currentServer,
                navManager = navigationManager,
                destination = destination,
                composeUiConfig = composeUiConfig,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                onChangeTheme = onChangeTheme,
                onSwitchServer = onSwitchServer,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
            )
        }
    }
}
