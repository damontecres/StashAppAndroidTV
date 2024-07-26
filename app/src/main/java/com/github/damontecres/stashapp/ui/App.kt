package com.github.damontecres.stashapp.ui

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Button
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.ui.DrawerPage.Home
import com.github.damontecres.stashapp.ui.DrawerPage.Scenes

sealed class DrawerPage(
    val route: String,
    @StringRes val iconString: Int,
    @StringRes val name: Int,
) {
    data object Home : DrawerPage("home", R.string.fa_house, R.string.home)

    data object Scenes : DrawerPage("scenes", DataType.SCENE.iconStringId, DataType.SCENE.pluralStringId)
}

val PAGES = listOf(Home, Scenes)

@Suppress("ktlint:standard:function-naming")
@Composable
fun App() {
    val fontFamily =
        FontFamily(
            Font(
                resId = R.font.fa_solid_900,
            ),
        )

    val defaultSelection: DrawerPage = DrawerPage.Home

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
                    .fillMaxHeight()
                    .padding(12.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
//                NavigationDrawerItem(
//                    selected = currentScreen == DrawerScreen.UserAccount,
//                    onClick = {
//                        currentScreen = DrawerScreen.UserAccount
//                    },
//                    leadingContent = {
//                        Icon(
//                            imageVector = DrawerScreen.UserAccount.icon,
//                            contentDescription = null,
//                        )
//                    },
//                ) {
//                    Text(stringResource(id = DrawerScreen.UserAccount.title))
//                }

                // Group of item with same padding

                TvLazyColumn(
                    Modifier
                        .selectableGroup(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                ) {
                    items(PAGES, key = { it.route }) { page ->
                        NavigationDrawerItem(
                            selected = navController.currentDestination?.route == page.route,
                            onClick = {
                                drawerState.setValue(DrawerValue.Closed)
                                navController.navigate(page.route) {
                                    // remove the previous Composable from the back stack
                                    popUpTo(navController.currentDestination?.route ?: "") {
                                        inclusive = true
                                    }
                                }
                            },
                            leadingContent = {
                                Text(stringResource(id = page.iconString), fontFamily = fontFamily)
                            },
                        ) {
                            Text(stringResource(id = page.name))
                        }
                    }
                }

                // Separated item from other for better UI purpose
//                NavigationDrawerItem(
//                    selected = currentScreen == DrawerScreen.SettingsScreen,
//                    onClick = {
//                        currentScreen = DrawerScreen.SettingsScreen
//                    },
//                    leadingContent = {
//                        Icon(
//                            imageVector = DrawerScreen.SettingsScreen.icon,
//                            contentDescription = null,
//                        )
//                    },
//                ) {
//                    Text(stringResource(id = DrawerScreen.SettingsScreen.title))
//                }
            }
        },
    ) {
        // content

        val context = LocalContext.current

        NavHost(
            navController = navController,
            startDestination = DrawerPage.Home.route,
            modifier =
            Modifier,
//                    .fillMaxSize()
//                    .padding(start = collapsedDrawerItemWidth),
        ) {
            composable(route = DrawerPage.Home.route) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Home clicked", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text(text = "Home")
                }
            }
            composable(route = DrawerPage.Scenes.route) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Scenes clicked", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text(text = "Scenes")
                }
            }
        }
    }
}
