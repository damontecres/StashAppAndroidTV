package com.github.damontecres.stashapp.ui

import android.content.Intent
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SearchActivity
import com.github.damontecres.stashapp.SettingsActivity
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashDefaultFilter
import com.github.damontecres.stashapp.util.StashServer

class DrawerPage(
    val route: String,
    @StringRes val iconString: Int,
    @StringRes val name: Int,
) {
    companion object {
        val HOME_PAGE = DrawerPage("home", R.string.fa_house, R.string.home)

        val SEARCH_PAGE =
            DrawerPage(
                "search",
                R.string.fa_magnifying_glass_plus,
                R.string.stashapp_actions_search,
            )

        val PAGES =
            buildList<DrawerPage> {
                add(SEARCH_PAGE)
                add(HOME_PAGE)
                addAll(
                    DataType.entries.map { dataType ->
                        DrawerPage(dataType.name, dataType.iconStringId, dataType.pluralStringId)
                    },
                )
            }
    }
}

class AppViewModel : ViewModel() {
    val currentServer = mutableStateOf<StashServer?>(StashServer.getCurrentStashServer())
}

@OptIn(ExperimentalFoundationApi::class)
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

    val collapsedDrawerItemWidth = 56.dp
    val paddingValue = 12.dp

    val navController = rememberNavController()

    NavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                Modifier
                    .focusGroup()
                    .fillMaxHeight()
                    .padding(4.dp)
                    .width(if (drawerState.currentValue == DrawerValue.Closed) collapsedDrawerItemWidth else Dp.Unspecified),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                NavigationDrawerItem(
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
                        modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.WhileFocused),
                        text = StashServer.getCurrentStashServer()?.url ?: "No server",
                    )
                }

                // Group of item with same padding

                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    modifier =
                        Modifier
                            .selectableGroup(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                ) {
                    Log.v("App", "DrawerPage.PAGES=${DrawerPage.PAGES}")
                    items(DrawerPage.PAGES, key = { it.route }) { page ->
                        NavigationDrawerItem(
                            selected = navController.currentDestination?.route == page.route,
                            onClick = {
                                if (page == DrawerPage.SEARCH_PAGE) {
                                    startActivity(
                                        context,
                                        Intent(context, SearchActivity::class.java),
                                        null,
                                    )
                                } else {
                                    drawerState.setValue(DrawerValue.Closed)
                                    navController.navigate(page.route) {
                                        // remove the previous Composable from the back stack
                                        popUpTo(navController.currentDestination?.route ?: "") {
                                            inclusive = true
                                        }
                                    }
                                }
                            },
                            leadingContent = {
                                Text(
                                    stringResource(id = page.iconString),
                                    fontFamily = fontFamily,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier,
                                )
                            },
                        ) {
                            Text(stringResource(id = page.name))
                        }
                    }
                }

                NavigationDrawerItem(
                    selected = false,
                    onClick = {
                        navController.navigate(DrawerPage.HOME_PAGE.route) {
                            popUpTo(navController.currentDestination?.route ?: "") {
                                inclusive = true
                            }
                        }
                        val intent = Intent(context, SettingsActivity::class.java)
                        startActivity(context, intent, null)
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.vector_settings),
                            contentDescription = null,
                        )
                    },
                ) {
                    Text(stringResource(id = R.string.stashapp_settings))
                }
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = DrawerPage.HOME_PAGE.route,
            modifier = Modifier,
//                    .fillMaxSize()
//                    .padding(start = collapsedDrawerItemWidth),
        ) {
            composable(route = DrawerPage.SEARCH_PAGE.route) {
                // TODO
            }
            composable(route = DrawerPage.HOME_PAGE.route) {
                HomePage()
            }
            DataType.entries.forEach { dataType ->
                composable(route = dataType.name) {
                    FilterGrid(StashDefaultFilter(dataType))
                }
            }
        }
    }
}
