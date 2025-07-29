package com.github.damontecres.stashapp.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.util.StashServer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavScaffold(
    server: StashServer,
    navigationManager: NavigationManagerCompose,
    composeUiConfig: ComposeUiConfig,
    destination: Destination,
    selectedScreen: DrawerPage?,
    pages: List<DrawerPage>,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    onSelectScreen: (DrawerPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf<AnnotatedString?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    when (selectedScreen) {
                        is DrawerPage.HomePage,
                        is DrawerPage.SearchPage,
                        ->
                            Text(
                                text = stringResource(selectedScreen.name),
                            )

                        else ->
                            title?.let {
                                Text(
                                    text = it,
                                )
                            }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedScreen != DrawerPage.HomePage) {
                            navigationManager.goBack()
                        } else {
                            navigationManager.goToMain()
                        }
                    }) {
                        Icon(
                            imageVector =
                                if (selectedScreen != DrawerPage.HomePage) {
                                    Icons.AutoMirrored.Filled.ArrowBack
                                } else {
                                    Icons.Default.Home
                                },
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navigationManager.navigate(
                            Destination.ManageServers(false),
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                        )
                    }
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                        )
                    }
                    NavDropdownMenu(
                        expanded = expanded,
                        pages = pages,
                        onDismissRequest = { expanded = false },
                        onClick = {
                            expanded = false
                            onSelectScreen.invoke(it)
                        },
                        selectedScreen = selectedScreen,
                        modifier = Modifier,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NavDrawerContent(
                navManager = navigationManager,
                server = server,
                destination = destination,
                composeUiConfig = composeUiConfig,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                modifier = Modifier.fillMaxSize(),
                onUpdateTitle = { title = it },
            )
        }
    }
}

@Composable
fun NavDropdownMenu(
    selectedScreen: DrawerPage?,
    expanded: Boolean,
    pages: List<DrawerPage>,
    onDismissRequest: () -> Unit,
    onClick: (DrawerPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        pages.forEach { page ->
            DropdownMenuItem(
                text = { Text(stringResource(page.name)) },
                leadingIcon = {
                    if (page !is DrawerPage.SettingPage) {
                        val color =
                            if (selectedScreen == page) {
                                MaterialTheme.colorScheme.border
                            } else {
                                Color.Unspecified
                            }
                        Text(
                            text = stringResource(page.iconString),
                            fontFamily = FontAwesome,
                            fontSize = 24.sp,
                            color = color,
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.vector_settings),
                            contentDescription = null,
                        )
                    }
                },
                onClick = { onClick.invoke(page) },
            )
        }
    }
}
