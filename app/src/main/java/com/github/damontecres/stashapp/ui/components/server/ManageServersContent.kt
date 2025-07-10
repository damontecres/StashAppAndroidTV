package com.github.damontecres.stashapp.ui.components.server

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.filter.SimpleListItem
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.StashServer

@Composable
fun ManageServersContent(
    currentServer: StashServer?,
    onSwitchServer: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageServersViewModel = viewModel(),
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val allServers by viewModel.allServers.observeAsState(listOf())
    val serverStatus by viewModel.serverStatus.observeAsState(mapOf())
    val serversWithOutCurrent = allServers.toMutableList().apply { remove(currentServer) }
    val serverStatusWithOutCurrent = serverStatus.toMutableMap().apply { remove(currentServer) }

    var showSwitchServer by remember { mutableStateOf(false) }
    var showRemoveServer by remember { mutableStateOf(false) }
    var showAddServer by remember { mutableStateOf(false) }

    val listWidth = 280.dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Manage Servers",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
        )

        LazyRow(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                ManageServersButtons(
                    currentServer = currentServer,
                    serversWithOutCurrent = serversWithOutCurrent,
                    onSwitchClick = { showSwitchServer = true },
                    onAddClick = { showAddServer = true },
                    onRemoveClick = { showRemoveServer = true },
                    switchSelected = showSwitchServer,
                    removeSelected = showRemoveServer,
                    modifier = Modifier.width(listWidth),
                )
            }
            if (showSwitchServer) {
                item {
                    BackHandler {
                        showSwitchServer = false
                    }
                    ServerList(
                        servers = serverStatusWithOutCurrent,
                        action = ManageServerAction.Switch,
                        onClick = onSwitchServer,
                        modifier =
                            Modifier
                                .widthIn(min = listWidth, max = listWidth * 2)
                                .animateItem()
                                .focusProperties { onExit = { showSwitchServer = false } },
                    )
                }
            } else if (showRemoveServer) {
                item {
                    BackHandler {
                        showRemoveServer = false
                    }
                    ServerList(
                        servers = serverStatusWithOutCurrent,
                        action = ManageServerAction.Remove,
                        onClick = {
                            if (allServers.size == 1) {
                                showRemoveServer = false
                            } else {
                                focusManager.moveFocus(FocusDirection.Previous)
                            }
                            viewModel.removeServer(it)
                        },
                        modifier =
                            Modifier
                                .widthIn(min = listWidth, max = listWidth * 2)
                                .animateItem()
                                .focusProperties { onExit = { showRemoveServer = false } },
                    )
                }
            }
        }
    }
    if (showAddServer) {
        Dialog(
            onDismissRequest = { showAddServer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val elevatedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            AddServer(
                currentServerUrls = allServers.map { it.url },
                onSubmit = {
                    viewModel.addServer(it)
                    onSwitchServer.invoke(it)
                },
                modifier =
                    Modifier
                        .width(480.dp)
                        .graphicsLayer {
                            this.clip = true
                            this.shape = RoundedCornerShape(28.0.dp)
                        }.drawBehind { drawRect(color = elevatedContainerColor) },
            )
        }
    }
}

@Composable
fun ManageServersButtons(
    currentServer: StashServer?,
    serversWithOutCurrent: List<StashServer>,
    onSwitchClick: () -> Unit,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
    switchSelected: Boolean,
    removeSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val canRemove = serversWithOutCurrent.isNotEmpty()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.tryRequestFocus() }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier =
            modifier
                .focusGroup()
                .focusRestorer(focusRequester),
    ) {
//        stickyHeader {
//            Text(
//                text = "Manage servers",
//                color = MaterialTheme.colorScheme.onPrimaryContainer,
//                textAlign = TextAlign.Center,
//                modifier = Modifier.fillMaxWidth(),
//            )
//        }
        item {
            SimpleListItem(
                title = "Switch server",
                subtitle = null,
                showArrow = true,
                selected = switchSelected,
                enabled = currentServer != null && canRemove,
                onClick = onSwitchClick,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                    )
                },
                modifier =
                    Modifier
                        .focusRequester(focusRequester),
            )
        }
        item {
            SimpleListItem(
                title = "Add server",
                subtitle = null,
                showArrow = true,
                onClick = onAddClick,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Green.copy(alpha = .5f),
                    )
                },
            )
        }
        item {
            SimpleListItem(
                title = "Remove server",
                subtitle = null,
                showArrow = true,
                selected = removeSelected,
                enabled = canRemove,
                onClick = onRemoveClick,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.Red,
                    )
                },
            )
        }
    }
}

@Composable
fun ServerList(
    servers: Map<StashServer, ServerTestResult>,
    action: ManageServerAction,
    onClick: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.tryRequestFocus() }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier =
            modifier
                .focusGroup()
                .focusRestorer(focusRequester),
    ) {
        stickyHeader {
            Text(
                text =
                    when (action) {
                        ManageServerAction.Remove -> "Remove server"
                        ManageServerAction.Switch -> "Switch server"
                    },
                color =
                    when (action) {
                        ManageServerAction.Remove -> MaterialTheme.colorScheme.error
                        ManageServerAction.Switch -> MaterialTheme.colorScheme.onSecondaryContainer
                    },
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillParentMaxWidth(),
            )
        }
        itemsIndexed(servers.keys.toList()) { index, server ->
            val status = servers[server]!!
            SimpleListItem(
                title = server.url,
                subtitle =
                    if (action == ManageServerAction.Switch && status is ServerTestResult.Error) {
                        status.result.message
                    } else {
                        null
                    },
                showArrow = action == ManageServerAction.Switch && status == ServerTestResult.Success,
                onClick = { onClick.invoke(server) },
                enabled = action == ManageServerAction.Remove || status == ServerTestResult.Success,
                leadingContent = {
                    if (action == ManageServerAction.Switch) {
                        when (status) {
                            is ServerTestResult.Error -> {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = status.result.message,
                                    tint = Color.Red,
                                )
                            }
                            ServerTestResult.Pending -> {
                                CircularProgress(
                                    modifier = Modifier.size(ListItemDefaults.IconSize),
                                    fillMaxSize = false,
                                )
                            }
                            ServerTestResult.Success -> {}
                        }
                    }
                },
                modifier = Modifier.ifElse(index == 0, Modifier.focusRequester(focusRequester)),
            )
        }
    }
}

sealed interface ManageServerAction {
    data object Switch : ManageServerAction

    data object Remove : ManageServerAction
}
