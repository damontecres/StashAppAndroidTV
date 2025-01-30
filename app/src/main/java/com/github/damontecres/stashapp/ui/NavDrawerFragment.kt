package com.github.damontecres.stashapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.MainPage
import com.github.damontecres.stashapp.ui.components.PerformerPage
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.views.models.ServerViewModel
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.rememberNavController

class NavDrawerFragment : Fragment(R.layout.compose_frame) {
    private val serverViewModel: ServerViewModel by activityViewModels()

    @OptIn(ExperimentalComposeUiApi::class)
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
                MainTheme {
                    val navController = rememberNavController<Destination>(Destination.Main)
                    NavBackHandler(navController)

                    val navManager = (serverViewModel.navigationManager as NavigationManagerCompose)
                    navManager.controller = navController

                    val fontFamily =
                        FontFamily(
                            Font(
                                resId = R.font.fa_solid_900,
                            ),
                        )
                    val defaultSelection: DrawerPage = DrawerPage.HOME_PAGE
                    var currentScreen by remember { mutableStateOf(defaultSelection) }
                    val drawerState = rememberDrawerState(DrawerValue.Closed)

                    val collapsedDrawerItemWidth = 48.dp
                    val paddingValue = 12.dp

                    val server: StashServer? by serverViewModel.currentServer.observeAsState()
                    val cardUiSettings by serverViewModel.cardUiSettings.observeAsState()
                    val composeUiConfig = server?.let { ComposeUiConfig.fromStashServer(it) }

                    val itemOnClick = { item: Any ->
                        serverViewModel.navigationManager.navigate(Destination.fromStashData(item as StashData))
                    }
                    val longClicker =
                        remember {
                            LongClicker.default {
                                TODO()
                            }
                        }

                    val pages =
                        buildList {
                            if (server != null) {
                                add(DrawerPage.SEARCH_PAGE)
                                add(DrawerPage.HOME_PAGE)
                                val serverPrefs = server?.serverPreferences
                                if (serverPrefs != null) {
                                    DataType.entries
                                        .mapNotNull { dataType ->
                                            if (serverPrefs.showMenuItem(dataType)) {
                                                val filter =
                                                    serverPrefs.defaultFilters[dataType]
                                                        ?: FilterArgs(dataType)
                                                DrawerPage(
                                                    Destination.Filter(filter, false),
                                                    dataType.iconStringId,
                                                    dataType.pluralStringId,
                                                )
                                            } else {
                                                null
                                            }
                                        }.forEach { add(it) }
                                }
                            }
                            add(DrawerPage.SETTINGS_PAGE)
                        }

                    val initialFocus = remember { FocusRequester() }

                    NavHost(navController) { destination ->
                        if (destination.fullScreen) {
                            FragmentView(navManager, destination)
                        } else {
                            NavigationDrawer(
                                modifier = Modifier.padding(4.dp),
                                drawerState = drawerState,
                                drawerContent = {
                                    Column(
                                        Modifier
                                            .fillMaxHeight()
                                            .padding(4.dp)
                                            .width(
                                                if (drawerState.currentValue == DrawerValue.Closed) {
                                                    collapsedDrawerItemWidth
                                                } else {
                                                    Dp.Unspecified
                                                },
                                            ),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        var serverFocused by remember { mutableStateOf(false) }
                                        NavigationDrawerItem(
                                            modifier =
                                                Modifier
                                                    .onFocusChanged {
                                                        serverFocused = it.isFocused
                                                    },
                                            selected = false,
                                            onClick = {
                                                serverViewModel.navigationManager.navigate(
                                                    Destination.ManageServers(
                                                        false,
                                                    ),
                                                )
                                            },
                                            leadingContent = {
                                                Icon(
                                                    painterResource(id = R.mipmap.stash_logo),
                                                    contentDescription = null,
                                                )
                                            },
                                        ) {
                                            Text(
                                                modifier =
                                                    Modifier.enableMarquee(serverFocused),
                                                text = server?.url ?: "No server",
                                                maxLines = 1,
                                            )
                                        }

                                        // Group of item with same padding

                                        LazyColumn(
                                            contentPadding = PaddingValues(0.dp),
                                            modifier =
                                                Modifier
                                                    .focusGroup()
                                                    .focusRestorer { initialFocus }
                                                    .selectableGroup(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement =
                                                Arrangement.spacedBy(
                                                    6.dp,
                                                    Alignment.CenterVertically,
                                                ),
                                        ) {
                                            items(
                                                pages,
                                                key = null,
                                            ) { page ->
                                                val mod =
                                                    if (currentScreen == page) {
                                                        Modifier.focusRequester(initialFocus)
                                                    } else {
                                                        Modifier
                                                    }
                                                NavigationDrawerItem(
                                                    modifier = mod,
                                                    selected = currentScreen == page,
                                                    onClick = {
                                                        currentScreen = page
                                                        drawerState.setValue(DrawerValue.Closed)
                                                        Log.v(
                                                            TAG,
                                                            "Navigating to ${page.destination}",
                                                        )
                                                        serverViewModel.navigationManager.navigate(
                                                            page.destination,
                                                        )
                                                    },
                                                    leadingContent = {
                                                        if (page != DrawerPage.SETTINGS_PAGE) {
                                                            Text(
                                                                stringResource(id = page.iconString),
                                                                fontFamily = fontFamily,
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier,
                                                            )
                                                        } else {
                                                            Icon(
                                                                painter = painterResource(id = R.drawable.vector_settings),
                                                                contentDescription = null,
                                                            )
                                                        }
                                                    },
                                                ) {
                                                    Text(stringResource(id = page.name))
                                                }
                                            }
                                        }
                                    }
                                },
                            ) {
                                if (server != null) {
                                    when (destination) {
                                        Destination.Main -> {
                                            MainPage(
                                                server = server!!,
                                                uiConfig = composeUiConfig!!,
                                                cardUiSettings = cardUiSettings!!,
                                                itemOnClick = itemOnClick,
                                                longClicker = longClicker,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }

                                        is Destination.Item -> {
                                            if (destination.dataType == DataType.PERFORMER) {
                                                PerformerPage(
                                                    modifier = Modifier.fillMaxSize(),
                                                    server = server!!,
                                                    id = destination.id,
                                                    itemOnClick = itemOnClick,
                                                    longClicker = longClicker,
                                                )
                                            } else {
                                                FragmentView(navManager, destination)
                                            }
                                        }

                                        else -> {
                                            FragmentView(navManager, destination)
                                        }
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
        private const val TAG = "NavDrawerFragment"
    }
}

data class DrawerPage(
    val destination: Destination,
    @StringRes val iconString: Int,
    @StringRes val name: Int,
) {
    companion object {
        val HOME_PAGE = DrawerPage(Destination.Main, R.string.fa_house, R.string.home)

        val SEARCH_PAGE =
            DrawerPage(
                Destination.Search,
                R.string.fa_magnifying_glass_plus,
                R.string.stashapp_actions_search,
            )

        val SETTINGS_PAGE =
            DrawerPage(
                Destination.Settings,
                R.string.fa_arrow_right_arrow_left, // Ignored
                R.string.stashapp_settings,
            )
    }
}

@Composable
fun FragmentView(
    navManager: NavigationManagerCompose,
    destination: Destination,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            LayoutInflater
                .from(context)
                .inflate(
                    R.layout.root_fragment_layout,
                    null,
                )
        },
        update = { view ->
            navManager.composeNavigate(destination)
        },
    )
}
