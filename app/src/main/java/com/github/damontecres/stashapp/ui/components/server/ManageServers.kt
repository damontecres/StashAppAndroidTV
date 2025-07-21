package com.github.damontecres.stashapp.ui.components.server

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.filter.SimpleListItem
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.StashServer

@Composable
fun ManageServers(
    currentServer: StashServer?,
    onSwitchServer: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageServersViewModel = viewModel(),
) {
    val allServers by viewModel.allServers.observeAsState(listOf())
    val serverStatus by viewModel.serverStatus.observeAsState(mapOf())
    val serversWithOutCurrent = allServers.toMutableList().apply { remove(currentServer) }

    var showAddServer by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf<StashServer?>(null) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.tryRequestFocus() }

    fun switchServer(server: StashServer) {
        val status = serverStatus[server]
        if (status == ServerTestResult.Success) {
            onSwitchServer.invoke(server)
        } else {
            viewModel.testServer(server)
        }
    }

    Column(
        modifier =
            modifier
                .ifElse(
                    showAddServer,
                    Modifier
                        .blur(10.dp)
                        .graphicsLayer {
                            alpha = .25f
                        },
                ),
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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier =
                Modifier
                    .fillMaxWidth(.6f)
                    .focusGroup()
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .75f),
                        shape = RoundedCornerShape(16.dp),
                    ),
        ) {
            itemsIndexed(serversWithOutCurrent) { index, server ->
                val status = serverStatus[server] ?: ServerTestResult.Pending
                SimpleListItem(
                    title = server.url,
                    subtitle =
                        if (status is ServerTestResult.Error) {
                            status.result.message
                        } else {
                            null
                        },
                    showArrow = status == ServerTestResult.Success,
                    onClick = { switchServer(server) },
                    onLongClick = { showServerDialog = server },
                    enabled = true,
                    leadingContent = {
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
                    },
                    modifier = Modifier.ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                )
            }

            item {
                HorizontalDivider()
                SimpleListItem(
                    title = stringResource(R.string.add_server),
                    subtitle = null,
                    showArrow = true,
                    onClick = { showAddServer = true },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_server),
                            tint = Color.Green.copy(alpha = .5f),
                        )
                    },
                    modifier =
                        Modifier.ifElse(
                            serversWithOutCurrent.isEmpty(),
                            Modifier.focusRequester(focusRequester),
                        ),
                )
            }
        }
    }

    if (showAddServer) {
        Dialog(
            onDismissRequest = { showAddServer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
            dialogWindowProvider?.window?.setGravity(Gravity.TOP)

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
    showServerDialog?.let { server ->
        val items =
            listOf(
                DialogItem(
                    onClick = { switchServer(server) },
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.switch_server),
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                        )
                    },
                ),
                DialogItem(
                    onClick = {
                        viewModel.removeServer(server)
                        focusRequester.tryRequestFocus()
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.stashapp_actions_delete),
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.Red,
                        )
                    },
                ),
            )
        DialogPopup(
            showDialog = true,
            title = server.url,
            dialogItems = items,
            onDismissRequest = { showServerDialog = null },
        )
    }
}
