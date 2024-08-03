package com.github.damontecres.stashapp.ui

import android.content.Intent
import android.os.Parcelable
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.activity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import com.github.damontecres.stashapp.GalleryActivity
import com.github.damontecres.stashapp.ImageActivity
import com.github.damontecres.stashapp.MovieActivity
import com.github.damontecres.stashapp.PerformerActivity
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SceneDetailsActivity
import com.github.damontecres.stashapp.SceneDetailsFragment.Companion.POSITION_ARG
import com.github.damontecres.stashapp.SearchActivity
import com.github.damontecres.stashapp.SettingsActivity
import com.github.damontecres.stashapp.StudioActivity
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashDefaultFilter
import com.github.damontecres.stashapp.data.StashFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.playback.PlaybackActivity
import com.github.damontecres.stashapp.ui.details.ScenePage
import com.github.damontecres.stashapp.ui.details.TagPage
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getDataType
import com.github.damontecres.stashapp.util.getId
import com.github.damontecres.stashapp.util.secondsMs
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

private const val TAG = "Compose.App"

data class DrawerPage(
    val route: Route,
    @StringRes val iconString: Int,
    @StringRes val name: Int,
) {
    companion object {
        val HOME_PAGE = DrawerPage(Route.Home, R.string.fa_house, R.string.home)

        val SEARCH_PAGE =
            DrawerPage(
                Route.Search,
                R.string.fa_magnifying_glass_plus,
                R.string.stashapp_actions_search,
            )

        val SETTINGS_PAGE =
            DrawerPage(
                Route.Settings,
                R.string.fa_arrow_right_arrow_left, // Ignored
                R.string.stashapp_settings,
            )

        val DATA_TYPE_PAGES =
            buildMap {
                DataType.entries.forEach { dataType ->
                    put(
                        dataType,
                        DrawerPage(
                            Route.DataTypeRoute(dataType),
                            dataType.iconStringId,
                            dataType.pluralStringId,
                        ),
                    )
                }
            }

        fun dataType(dataType: DataType): DrawerPage {
            return DATA_TYPE_PAGES[dataType]!!
        }

        val PAGES =
            buildList<DrawerPage> {
                add(SEARCH_PAGE)
                add(HOME_PAGE)
                addAll(DATA_TYPE_PAGES.values)
                add(SETTINGS_PAGE)
            }
    }
}

@Serializable
@Parcelize
sealed class Route : Parcelable {
    @Serializable
    data class DataTypeRoute(val dataType: DataType, val id: String? = null) : Route()

    @Serializable
    data class Playback(val id: String, val position: Long = 0) : Route()

    @Serializable
    data class Filter(val filter: StashFilter) : Route()

    @Serializable
    data object Home : Route()

    @Serializable
    data object Search : Route()

    @Serializable
    data object Settings : Route()
}

class AppViewModel : ViewModel() {
    val currentServer = mutableStateOf<StashServer?>(StashServer.getCurrentStashServer())
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun App() {
    val context = LocalContext.current

    val appViewModel = viewModel<AppViewModel>()
    appViewModel.currentServer.value = StashServer.getCurrentStashServer()

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

    val navController = rememberNavController()
    val focusRequester = remember { FocusRequester() }
    val focusRequesters = remember { DrawerPage.PAGES.associateWith { FocusRequester() } }

    NavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                Modifier
                    .focusGroup()
                    .fillMaxHeight()
                    .padding(4.dp)
                    .width(if (drawerState.currentValue == DrawerValue.Closed) collapsedDrawerItemWidth else Dp.Unspecified)
                    .focusRequester(focusRequester)
                    .focusProperties {
                        enter = { focusDirection ->
                            if (focusDirection == FocusDirection.Left) {
//                                val currentPage =
//                                    DrawerPage.PAGES.firstOrNull { page ->
//                                        navController.currentDestination?.route?.startsWith(
//                                            page.route,
//                                        ) ?: false
//                                    }
//                                Log.v(TAG, "focus enter currentPage=$currentPage")
//                                if (currentPage != null) {
//                                    focusRequesters[currentPage]!!
//                                } else {
//                                    focusRequesters[DrawerPage.HOME_PAGE]!!
//                                }
                                FocusRequester.Default
                            } else {
                                FocusRequester.Default
                            }
                        }
                    },
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
                        // TODO
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
                        text = StashServer.getCurrentStashServer()?.url ?: "No server",
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
                    Log.v(TAG, "DrawerPage.PAGES=${DrawerPage.PAGES}")
                    items(DrawerPage.PAGES, key = { it.route }) { page ->
                        NavigationDrawerItem(
                            modifier =
                                Modifier
                                    .focusRequester(focusRequesters[page]!!),
                            //                            shape = NavigationDrawerItemDefaults.shape(shape = RoundedCornerShape(50)),
//                            glow = NavigationDrawerItemDefaults.glow(Glow.None),
                            selected = false,
//                                navController.currentDestination?.route?.startsWith(page.route)
//                                    ?: false,
                            onClick = {
                                drawerState.setValue(DrawerValue.Closed)
                                Log.v(TAG, "Navigating to ${page.route}")
                                navController.navigate(page.route) {
                                    // remove the previous Composable from the back stack
                                    popUpTo(navController.currentDestination?.route ?: "") {
                                        inclusive = true
                                    }
                                }
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
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = Modifier,
//                    .fillMaxSize()
//                    .padding(start = collapsedDrawerItemWidth),
        ) {
            val typeMap =
                mapOf(
                    typeOf<DataType>() to NavType.EnumType(DataType::class.java),
                )

            val itemOnClick = { item: Any ->
                val dataType = getDataType(item)

                val route =
                    if (dataType == DataType.MARKER) {
                        item as MarkerData
                        Route.Playback(item.scene.videoSceneData.id, item.secondsMs)
                    } else if (dataType != null) {
                        Route.DataTypeRoute(dataType, getId(item))
                    } else if (item is StashFilter) {
                        item
                    } else {
                        throw IllegalArgumentException("Unknown item clicked: $item")
                    }
                navController.navigate(route = route) {
                }
            }

            composable<Route.Home> {
                HomePage(itemOnClick)
            }
            activity<Route.Search> {
                activityClass = SearchActivity::class
                argument(Constants.USE_NAV_CONTROLLER) {
                    type = NavType.BoolType
                    defaultValue = true
                }
            }
            activity<Route.Settings> {
                activityClass = SettingsActivity::class
                argument(Constants.USE_NAV_CONTROLLER) {
                    type = NavType.BoolType
                    defaultValue = true
                }
            }

            val composedDataTypes = listOf(DataType.SCENE, DataType.TAG)

            composable<Route.DataTypeRoute>(typeMap) {
                val dataTypeRoute = it.toRoute<Route.DataTypeRoute>()
                Log.v(TAG, "dataTypeRoute=$dataTypeRoute")
                if (dataTypeRoute.dataType == DataType.MARKER) {
                    throw IllegalArgumentException("Cannot pass DataType.Marker in a DataTypeRoute")
                }
                if (dataTypeRoute.id.isNullOrBlank()) {
                    FilterGrid(StashDefaultFilter(dataTypeRoute.dataType), itemOnClick)
                } else {
                    when (dataTypeRoute.dataType) {
                        DataType.SCENE ->
                            ScenePage(
                                sceneId = dataTypeRoute.id,
                                itemOnClick = itemOnClick,
                                playbackCallback = { position ->
                                    navController.navigate(
                                        Route.Playback(
                                            dataTypeRoute.id,
                                            position,
                                        ),
                                    )
                                },
                            )

                        DataType.TAG -> TagPage(dataTypeRoute.id, itemOnClick = itemOnClick)

                        DataType.PERFORMER -> {
                            context.startActivity(
                                Intent(
                                    context,
                                    PerformerActivity::class.java,
                                ).putExtra("id", dataTypeRoute.id)
                                    .putExtra(Constants.USE_NAV_CONTROLLER, true),
                            )
                        }

                        DataType.STUDIO -> {
                            context.startActivity(
                                Intent(
                                    context,
                                    StudioActivity::class.java,
                                ).putExtra("id", dataTypeRoute.id)
                                    .putExtra(Constants.USE_NAV_CONTROLLER, true),
                            )
                        }

                        DataType.MOVIE -> {
                            context.startActivity(
                                Intent(
                                    context,
                                    MovieActivity::class.java,
                                ).putExtra("id", dataTypeRoute.id)
                                    .putExtra(Constants.USE_NAV_CONTROLLER, true),
                            )
                        }

                        DataType.IMAGE -> {
                            context.startActivity(
                                Intent(
                                    context,
                                    ImageActivity::class.java,
                                ).putExtra("id", dataTypeRoute.id)
                                    .putExtra(Constants.USE_NAV_CONTROLLER, true),
                            )
                        }

                        DataType.GALLERY -> {
                            context.startActivity(
                                Intent(
                                    context,
                                    GalleryActivity::class.java,
                                ).putExtra("id", dataTypeRoute.id)
                                    .putExtra(Constants.USE_NAV_CONTROLLER, true),
                            )
                        }

                        DataType.MARKER -> throw IllegalStateException()
                    }
                }
            }

            activity<Route.Playback> {
                argument(SceneDetailsActivity.MOVIE_ID) {
                    type = NavType.StringType
                    nullable = false
                }
                argument(POSITION_ARG) {
                    type = NavType.LongType
                    nullable = false
                    defaultValue = 0L
                }
                argument(Constants.USE_NAV_CONTROLLER) {
                    type = NavType.BoolType
                    defaultValue = true
                }
                activityClass = PlaybackActivity::class
            }
            composable<StashCustomFilter>(typeMap = typeMap) { backStackEntry ->
                val filter: StashCustomFilter = backStackEntry.toRoute()
                FilterGrid(startingFilter = filter, itemOnClick = itemOnClick)
            }
            composable<StashSavedFilter>(typeMap = typeMap) { backStackEntry ->
                val filter: StashSavedFilter = backStackEntry.toRoute()
                FilterGrid(startingFilter = filter, itemOnClick = itemOnClick)
            }
        }
    }
}
