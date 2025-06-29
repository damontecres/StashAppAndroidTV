package com.github.damontecres.stashapp.ui

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.github.damontecres.stashapp.PreferenceScreenOption
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.NavDrawerFragment.Companion.TAG
import com.github.damontecres.stashapp.ui.components.DefaultLongClicker
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.MarkerDurationDialog
import com.github.damontecres.stashapp.ui.components.filter.CreateFilterScreen
import com.github.damontecres.stashapp.ui.pages.ChooseThemePage
import com.github.damontecres.stashapp.ui.pages.DialogParams
import com.github.damontecres.stashapp.ui.pages.FilterPage
import com.github.damontecres.stashapp.ui.pages.GalleryPage
import com.github.damontecres.stashapp.ui.pages.GroupPage
import com.github.damontecres.stashapp.ui.pages.ImagePage
import com.github.damontecres.stashapp.ui.pages.MainPage
import com.github.damontecres.stashapp.ui.pages.MarkerPage
import com.github.damontecres.stashapp.ui.pages.PerformerPage
import com.github.damontecres.stashapp.ui.pages.PlaybackPage
import com.github.damontecres.stashapp.ui.pages.PlaylistPlaybackPage
import com.github.damontecres.stashapp.ui.pages.SceneDetailsPage
import com.github.damontecres.stashapp.ui.pages.SearchPage
import com.github.damontecres.stashapp.ui.pages.StudioPage
import com.github.damontecres.stashapp.ui.pages.TagPage
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.ui.util.playOnClickSound
import com.github.damontecres.stashapp.ui.util.playSoundOnFocus
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.views.models.ServerViewModel
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okhttp3.Call
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NavDrawerFragment : Fragment(R.layout.compose_frame) {
    private val serverViewModel: ServerViewModel by activityViewModels()

    var navController: NavController<Destination>? = null

    @OptIn(ExperimentalCoilApi::class)
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
                val server by serverViewModel.currentServer.observeAsState()
                setSingletonImageLoaderFactory { ctx ->
                    val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
                    val cacheLogging = prefs.getBoolean("networkCacheLogging", false)
                    val diskCacheSize =
                        prefs
                            .getInt(context.getString(R.string.pref_key_image_cache_size), 100)
                            .coerceAtLeast(10)
                    ImageLoader
                        .Builder(context)
                        .diskCache(
                            DiskCache
                                .Builder()
                                .directory(context.cacheDir.resolve("coil3_image_cache"))
                                .maxSizeBytes(diskCacheSize * 1024 * 1024L)
                                .build(),
                        ).crossfade(true)
                        .logger(if (cacheLogging) DebugLogger() else null)
                        .components {
                            add(
                                OkHttpNetworkFetcherFactory(
                                    cacheStrategy = { CacheControlCacheStrategy() },
                                    callFactory = {
                                        Call.Factory { request ->
                                            // TODO this seems hacky?
                                            serverViewModel.requireServer().okHttpClient.newCall(
                                                request,
                                            )
                                        }
                                    },
                                ),
                            )
                        }.build()
                }

                val isSystemInDarkTheme = isSystemInDarkTheme()
                var colorScheme by
                    remember { mutableStateOf(getTheme(requireContext(), false, isSystemInDarkTheme)) }
                MaterialTheme(colorScheme = colorScheme.tvColorScheme) {
                    key(server) {
                        val navController = rememberNavController<Destination>(Destination.Main)
                        this@NavDrawerFragment.navController = navController
                        NavBackHandler(navController)
                        val navManager =
                            (serverViewModel.navigationManager as NavigationManagerCompose)
//                        navManager.controller = navController

                        val navCommand by serverViewModel.command.observeAsState()
                        LaunchedEffect(navCommand) {
                            navCommand?.let { cmd ->
                                Log.v(TAG, "cmd=$cmd, server=$server")
                                if (cmd.popUpToMain) {
                                    navController.popUpTo { it == Destination.Main }
                                }
                                navController.navigate(cmd.destination)
                            }
                        }
                        server?.let { currentServer ->
                            Log.v(TAG, "currentServer=$currentServer")
                            CompositionLocalProvider(
                                LocalGlobalContext provides
                                    GlobalContext(
                                        currentServer,
                                        navManager,
                                    ),
                            ) {
                                FragmentContent(
                                    server = currentServer,
                                    navigationManager = navManager,
                                    navController = navController,
                                    onChangeTheme = { name ->
                                        try {
                                            colorScheme =
                                                chooseColorScheme(
                                                    requireContext(),
                                                    isSystemInDarkTheme,
                                                    if (name.isNullOrBlank() || name == "default") {
                                                        defaultColorSchemeSet
                                                    } else {
                                                        readThemeJson(requireContext(), name)
                                                    },
                                                )
                                            Log.i(TAG, "Updated theme")
                                        } catch (ex: Exception) {
                                            Log.e(TAG, "Exception changing theme", ex)
                                            Toast
                                                .makeText(
                                                    requireContext(),
                                                    "Error changing theme: ${ex.localizedMessage}",
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                        }
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                                    // TODO could use onKeyEvent here to make focus/movement sounds everywhere
                                    // But it wouldn't know if the focus would actually change
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

@Composable
fun FragmentContent(
    server: StashServer,
    navigationManager: NavigationManagerCompose,
    navController: NavController<Destination>,
    onChangeTheme: (String?) -> Unit,
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

    // TODO this works, but sometimes requires restart when changed and going back from settings is awkward
    val settingsPage =
        DrawerPage(
            if (composeUiConfig.readOnlyModeDisabled) Destination.Settings(PreferenceScreenOption.BASIC) else Destination.SettingsPin,
            R.string.fa_arrow_right_arrow_left, // Ignored
            R.string.stashapp_settings,
        )

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

            add(settingsPage)
        }
    val visiblePages = remember { mutableMapOf<DrawerPage, Boolean>() }

    val initialFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val defaultSelection: DrawerPage = DrawerPage.HOME_PAGE
    var currentScreen by rememberSaveable { mutableStateOf(defaultSelection) }
    var selectedScreen by rememberSaveable { mutableStateOf<DrawerPage?>(defaultSelection) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    NavHost(navController, modifier = modifier) { destination ->
        LaunchedEffect(Unit) {
            scope.launch(StashCoroutineExceptionHandler()) {
                // Refresh server preferences on each page change
                server.updateServerPrefs()
                composeUiConfig = ComposeUiConfig.fromStashServer(context, server)
            }
            navigationManager.serverViewModel.setCurrentDestination(destination)
        }

        if (destination.fullScreen) {
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

                is Destination.Playback -> {
                    PlaybackPage(
                        server = server,
                        sceneId = destination.sceneId,
                        startPosition = destination.position,
                        playbackMode = destination.mode,
                        uiConfig = composeUiConfig,
                    )
                }

                is Destination.Playlist -> {
                    PlaylistPlaybackPage(
                        server = server,
                        uiConfig = composeUiConfig,
                        filterArgs = destination.filterArgs,
                        startIndex = destination.position,
                        clipDuration = destination.duration?.milliseconds ?: 30.seconds,
                        modifier = Modifier,
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Destination.ChooseTheme ->
                    ChooseThemePage(
                        server = server,
                        navigationManager = navigationManager,
                        uiConfig = composeUiConfig,
                        onChooseTheme = onChangeTheme,
                        modifier = Modifier.fillMaxSize(),
                    )

                is Destination.CreateFilter ->
                    CreateFilterScreen(
                        uiConfig = composeUiConfig,
                        dataType = destination.dataType,
                        initialFilter = destination.startingFilter,
                        navigationManager = navigationManager,
                        modifier = Modifier.fillMaxSize(),
                    )

                else -> FragmentView(navigationManager, destination)
            }
        } else {
            // Highlight on the nav drawer as user navigates around the app
            selectedScreen =
                when (destination) {
                    Destination.Main -> DrawerPage.HOME_PAGE
                    Destination.Search -> DrawerPage.SEARCH_PAGE

                    is Destination.Item ->
                        pages.firstOrNull {
                            val dest = it.destination
                            dest is Destination.Filter && dest.filterArgs.dataType == destination.dataType
                        }

                    is Destination.MarkerDetails ->
                        pages.firstOrNull {
                            val dest = it.destination
                            dest is Destination.Filter && dest.filterArgs.dataType == DataType.MARKER
                        }

                    is Destination.Filter ->
                        pages.firstOrNull {
                            val dest = it.destination
                            dest is Destination.Filter && dest.filterArgs.dataType == destination.filterArgs.dataType
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
                modifier = Modifier.focusRequester(drawerFocusRequester),
                drawerState = drawerState,
                drawerContent = {
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(0.dp),
                            modifier =
                                Modifier
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
                                    .selectableGroup(),
                            state = listState,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            item {
                                NavigationDrawerItem(
                                    modifier =
                                        Modifier
                                            .onFocusChanged {
                                                serverFocused = it.isFocused
                                            }.playSoundOnFocus(composeUiConfig.playSoundOnFocus),
                                    selected = false,
                                    onClick = {
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
                            }
                            items(
                                pages,
                                key = null,
                            ) { page ->
                                NavigationDrawerItem(
                                    modifier =
                                        Modifier
                                            .ifElse(
                                                selectedScreen == page,
                                                Modifier
                                                    .focusRequester(initialFocus),
                                            ).isElementVisible { visiblePages[page] = it }
                                            .playSoundOnFocus(composeUiConfig.playSoundOnFocus),
                                    selected = selectedScreen == page && drawerState.currentValue == DrawerValue.Open,
                                    onClick = {
                                        if (composeUiConfig.playSoundOnFocus) {
                                            playOnClickSound(
                                                context,
                                            )
                                        }
                                        val refreshMain =
                                            selectedScreen == DrawerPage.HOME_PAGE && page == DrawerPage.HOME_PAGE
                                        currentScreen = page
                                        selectedScreen = page

                                        drawerState.setValue(DrawerValue.Closed)
                                        Log.v(
                                            TAG,
                                            "Navigating to ${page.destination}",
                                        )
                                        if (refreshMain) {
                                            navigationManager.goToMain()
                                        } else {
                                            val pageDest =
                                                if (page.destination is Destination.Filter) {
                                                    page.destination.copy(filterArgs = page.destination.filterArgs.withResolvedRandom())
                                                } else {
                                                    page.destination
                                                }
                                            navigationManager.navigateFromNavDrawer(pageDest)
                                        }
                                    },
                                    leadingContent = {
                                        if (page != settingsPage) {
                                            val color =
                                                if (selectedScreen == page) {
                                                    MaterialTheme.colorScheme.border
                                                } else {
                                                    Color.Unspecified
                                                }
                                            Text(
                                                stringResource(id = page.iconString),
                                                fontFamily = FontAwesome,
                                                textAlign = TextAlign.Center,
                                                modifier =
                                                    Modifier
                                                        // Centers the icon for some reason
                                                        .padding(top = 4.dp),
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

@Composable
fun NavDrawerContent(
    navManager: NavigationManagerCompose,
    server: StashServer,
    destination: Destination,
    composeUiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        Destination.Main -> {
            MainPage(
                server = server,
                uiConfig = composeUiConfig,
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
                uiConfig = composeUiConfig,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                modifier = modifier,
            )
        }

        is Destination.MarkerDetails ->
            MarkerPage(
                server = server,
                uiConfig = composeUiConfig,
                markerId = destination.markerId,
                itemOnClick = itemOnClick,
            )

        is Destination.Item -> {
            when (destination.dataType) {
                DataType.SCENE ->
                    SceneDetailsPage(
                        modifier = modifier,
                        server = server,
                        sceneId = destination.id,
                        itemOnClick = itemOnClick,
                        uiConfig = composeUiConfig,
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
                        uiConfig = composeUiConfig,
                    )

                DataType.TAG ->
                    TagPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        includeSubTags = false,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                    )

                DataType.STUDIO ->
                    StudioPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        includeSubStudios = false,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                    )

                DataType.GALLERY ->
                    GalleryPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                    )

                DataType.GROUP ->
                    GroupPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        includeSubGroups = false,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                    )

                DataType.MARKER ->
                    MarkerPage(
                        server = server,
                        uiConfig = composeUiConfig,
                        markerId = destination.id,
                        itemOnClick = itemOnClick,
                    )

                else -> FragmentView(navManager, destination, modifier)
            }
        }

        else -> {
            FragmentView(navManager, destination, modifier)
        }
    }
}

@Parcelize
data class DrawerPage(
    val destination: Destination,
    @StringRes val iconString: Int,
    @StringRes val name: Int,
) : Parcelable {
    companion object {
        val HOME_PAGE = DrawerPage(Destination.Main, R.string.fa_house, R.string.home)

        val SEARCH_PAGE =
            DrawerPage(
                Destination.Search,
                R.string.fa_magnifying_glass_plus,
                R.string.stashapp_actions_search,
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
            navManager.navigate(destination)
        },
    )
}
