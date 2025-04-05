package com.github.damontecres.stashapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.github.damontecres.stashapp.PreferenceScreenOption
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.NavDrawerFragment.Companion.TAG
import com.github.damontecres.stashapp.ui.components.DefaultLongClicker
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.pages.DialogParams
import com.github.damontecres.stashapp.ui.pages.FilterPage
import com.github.damontecres.stashapp.ui.pages.GalleryPage
import com.github.damontecres.stashapp.ui.pages.GroupPage
import com.github.damontecres.stashapp.ui.pages.ImagePage
import com.github.damontecres.stashapp.ui.pages.MainPage
import com.github.damontecres.stashapp.ui.pages.PerformerPage
import com.github.damontecres.stashapp.ui.pages.PlaybackPage
import com.github.damontecres.stashapp.ui.pages.SceneDetailsPage
import com.github.damontecres.stashapp.ui.pages.SearchPage
import com.github.damontecres.stashapp.ui.pages.StudioPage
import com.github.damontecres.stashapp.ui.pages.TagPage
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.views.models.ServerViewModel
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.rememberNavController

class NavDrawerFragment : Fragment(R.layout.compose_frame) {
    private val serverViewModel: ServerViewModel by activityViewModels()

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
                    val server by serverViewModel.currentServer.observeAsState()
                    val currDestination by serverViewModel.destination.observeAsState()
                    key(server) {
                        val navController = rememberNavController<Destination>(Destination.Main)
                        NavBackHandler(navController)

                        val navManager =
                            (serverViewModel.navigationManager as NavigationManagerCompose)
                        navManager.controller = navController
                        if (currDestination != Destination.Pin) {
                            CompositionLocalProvider(
                                LocalGlobalContext provides
                                    GlobalContext(
                                        server ?: StashServer("http://0.0.0.0", null),
                                        navManager,
                                    ),
                            ) {
                                FragmentContent(
                                    server = server ?: StashServer("http://0.0.0.0", null),
                                    navigationManager = navManager,
                                )
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FragmentContent(
    server: StashServer,
    navigationManager: NavigationManagerCompose,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val defaultSelection: DrawerPage = DrawerPage.HOME_PAGE
    var currentScreen by remember { mutableStateOf(defaultSelection) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val collapsedDrawerItemWidth = 48.dp
    val paddingValue = 12.dp

    val composeUiConfig = ComposeUiConfig.fromStashServer(server)
    val cardUiSettings by remember { mutableStateOf(ServerViewModel.createUiSettings()) }

    val itemOnClick =
        ItemOnClicker { item: Any, filterAndPosition ->
            when (item) {
                is FilterArgs -> {
                    navigationManager.navigate(
                        Destination.Filter(
                            item,
                            true,
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

                else -> TODO(item::class.qualifiedName.toString())
            }
        }

    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    val longClicker =
        remember {
            DefaultLongClicker(navigationManager, itemOnClick) { dialogParams = it }
        }
    setSingletonImageLoaderFactory { context ->
        ImageLoader
            .Builder(context)
            .crossfade(true)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            server.okHttpClient
                        },
                    ),
                )
            }.build()
    }

    val pages =
        buildList {
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

            add(DrawerPage.SETTINGS_PAGE)
        }

    val initialFocus = remember { FocusRequester() }

    NavHost(navigationManager.controller, modifier = modifier) { destination ->
        if (destination.fullScreen) {
            when (destination) {
                is Destination.Playback -> {
                    PlaybackPage(
                        server = server,
                        sceneId = destination.sceneId,
                        startPosition = destination.position,
                        playbackMode = destination.mode,
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> FragmentView(navigationManager, destination)
            }
        } else {
            NavigationDrawer(
                modifier = Modifier,
                drawerState = drawerState,
                drawerContent = {
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondaryContainer)
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
                                navigationManager.navigate(
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
                                text = server.url,
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
                                        navigationManager.navigate(
                                            page.destination,
                                        )
                                    },
                                    leadingContent = {
                                        if (page != DrawerPage.SETTINGS_PAGE) {
                                            val color =
                                                if (currentScreen == page) {
                                                    MaterialTheme.colorScheme.border
                                                } else {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                }
                                            Text(
                                                stringResource(id = page.iconString),
                                                fontFamily = FontAwesome,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier,
                                                color = color,
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
                CompositionLocalProvider(
                    LocalPlayerContext provides PlayerContext,
                ) {
                    NavDrawerContent(
                        navManager = navigationManager,
                        server = server,
                        destination = destination,
                        cardUiSettings = cardUiSettings,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        modifier = Modifier.fillMaxSize(),
                    )
                    LifecycleStartEffect(Unit) {
                        onStopOrDispose {
                            StashExoPlayer.releasePlayer()
                        }
                    }
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
    }
}

@Composable
fun NavDrawerContent(
    navManager: NavigationManagerCompose,
    server: StashServer,
    destination: Destination,
    cardUiSettings: ServerViewModel.CardUiSettings,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    composeUiConfig: ComposeUiConfig = ComposeUiConfig.fromStashServer(server),
) {
    when (destination) {
        Destination.Main -> {
            MainPage(
                server = server,
                uiConfig = composeUiConfig,
                cardUiSettings = cardUiSettings,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                modifier = modifier,
            )
        }

        is Destination.Filter -> {
            FilterPage(
                server = server,
                navigationManager = navManager,
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
                server = server,
                navigationManager = navManager,
                uiConfig = ComposeUiConfig.fromStashServer(server),
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                modifier = modifier,
            )
        }

        is Destination.Item -> {
            when (destination.dataType) {
                DataType.SCENE ->
                    SceneDetailsPage(
                        modifier = modifier,
                        server = server,
                        sceneId = destination.id,
                        itemOnClick = itemOnClick,
                        playOnClick = { position, mode ->
                            navManager.navigate(
                                Destination.Playback(
                                    destination.id,
                                    position,
                                    mode,
                                ),
                            )
                        },
                    )

                DataType.PERFORMER ->
                    PerformerPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                    )

                DataType.TAG ->
                    TagPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        includeSubTags = false,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                    )

                DataType.STUDIO ->
                    StudioPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        includeSubStudios = false,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                    )

                DataType.GALLERY ->
                    GalleryPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                    )

                DataType.GROUP ->
                    GroupPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        includeSubGroups = false,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                    )

                else -> FragmentView(navManager, destination, modifier)
            }
        }

        else -> {
            FragmentView(navManager, destination, modifier)
        }
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
                Destination.Settings(PreferenceScreenOption.BASIC),
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
