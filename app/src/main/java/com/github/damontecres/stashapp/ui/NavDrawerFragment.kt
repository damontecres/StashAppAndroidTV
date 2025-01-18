package com.github.damontecres.stashapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.views.models.ServerViewModel

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
                AppTheme {
                    val fontFamily =
                        FontFamily(
                            Font(
                                resId = R.font.fa_solid_900,
                            ),
                        )
                    val defaultSelection: DrawerPage = DrawerPage.HOME_PAGE
                    var currentScreen by remember { mutableStateOf(defaultSelection) }
                    val drawerState = rememberDrawerState(DrawerValue.Closed)

                    val collapsedDrawerItemWidth = 44.dp
                    val paddingValue = 12.dp

                    val server: StashServer? by serverViewModel.currentServer.observeAsState()

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

                    val focusRequester = remember { FocusRequester() }
                    val focusRequesters = remember { pages.associateWith { FocusRequester() } }

                    NavigationDrawer(
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
                                        Modifier.onFocusChanged {
                                            serverFocused = it.isFocused
                                        },
                                    selected = false,
                                    onClick = {
                                        serverViewModel.navigationManager.navigate(Destination.ManageServers(false))
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
                                            .selectableGroup(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                                ) {
                                    items(pages, key = { it.destination.fragmentTag }) { page ->
                                        NavigationDrawerItem(
                                            modifier = Modifier,
//                                                    .focusRequester(focusRequesters[page]!!),
                                            selected = currentScreen == page,
                                            onClick = {
                                                currentScreen = page
                                                drawerState.setValue(DrawerValue.Closed)
                                                Log.v(TAG, "Navigating to ${page.destination}")
                                                serverViewModel.navigationManager.navigate(page.destination)
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
                        AndroidView(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(),
                            factory = { context ->
                                LayoutInflater
                                    .from(context)
                                    .inflate(R.layout.root_fragment_layout, null)
                            },
                            update = { view ->
                            },
                        )
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
