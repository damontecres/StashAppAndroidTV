package com.github.damontecres.stashapp.ui.components.server

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.filter.SimpleListItem
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

    var showSwitchServer by remember { mutableStateOf(false) }
    var showRemoveServer by remember { mutableStateOf(false) }

    LazyRow(modifier = modifier) {
        item {
            ManageServersButtons(
                currentServer = currentServer,
                serversWithOutCurrent = serversWithOutCurrent,
                onSwitchClick = { showSwitchServer = true },
                onAddClick = {},
                onRemoveClick = {},
                modifier = Modifier,
            )
        }
        if (showSwitchServer) {
            item {
                BackHandler {
                    showSwitchServer = false
                }
                ServerList(
                    servers = serverStatus,
                    action = ManageServerAction.Switch,
                    onClick = onSwitchServer,
                    modifier = Modifier,
                )
            }
        } else if (showRemoveServer) {
            item {
                BackHandler {
                    showRemoveServer = false
                }
                ServerList(
                    servers = serverStatus,
                    action = ManageServerAction.Remove,
                    onClick = {
                        if (allServers.size == 1) {
                            showRemoveServer = false
                        } else {
                            focusManager.moveFocus(FocusDirection.Previous)
                        }
                        viewModel.removeServer(it)
                    },
                    modifier = Modifier,
                )
            }
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
    modifier: Modifier = Modifier,
) {
    val canRemove = serversWithOutCurrent.isNotEmpty()
    LazyColumn(
        modifier = modifier,
    ) {
        stickyHeader {
            Text(
                text = "Manage servers",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillParentMaxWidth(),
            )
        }
        item {
            SimpleListItem(
                title = "Switch server",
                subtitle = null,
                showArrow = true,
                enabled = currentServer != null && canRemove,
                onClick = onSwitchClick,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                    )
                },
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
    LazyColumn(modifier = modifier) {
        items(servers.keys.toList()) { server ->
            val status = servers[server]!!
            SimpleListItem(
                title = server.url,
                subtitle = null,
                showArrow = action == ManageServerAction.Switch && status == ServerTestResult.Success,
                onClick = { onClick.invoke(server) },
                enabled = action == ManageServerAction.Remove || status == ServerTestResult.Success,
                leadingContent = {
                    // TODO
                },
                trailingContent = {
                    if (action == ManageServerAction.Switch) {
                        when (status) {
                            is ServerTestResult.Error -> {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = status.message,
                                    tint = Color.Red,
                                )
                            }
                            ServerTestResult.Pending -> {
                                CircularProgress()
                            }
                            ServerTestResult.Success -> {}
                        }
                    }
                },
            )
        }
    }
}

sealed interface ManageServerAction {
    data object Switch : ManageServerAction

    data object Remove : ManageServerAction
}

@Preview
@Composable
private fun ManageServersContentPreview() {
    AppTheme {
        ManageServersButtons(
            currentServer = null,
            serversWithOutCurrent = listOf(StashServer("", null)),
            onSwitchClick = { },
            onAddClick = {},
            onRemoveClick = {},
            modifier = Modifier,
        )
    }
}
