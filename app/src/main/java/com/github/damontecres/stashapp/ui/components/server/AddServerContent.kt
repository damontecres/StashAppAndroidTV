package com.github.damontecres.stashapp.ui.components.server

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.EditTextBox
import com.github.damontecres.stashapp.ui.components.SwitchWithLabel
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.getPreference
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.launch

private const val TAG = "AddServer"

@Composable
fun AddServer(
    currentServerUrls: List<String>,
    onSubmit: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageServersViewModel = viewModel(),
) {
    val context = LocalContext.current

    var serverUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf<String?>(null) }
    var showApiKey by remember { mutableStateOf(false) }
    var trustCerts by remember {
        mutableStateOf(
            getPreference(
                context,
                R.string.pref_key_trust_certs,
                false,
            ),
        )
    }

    val connectionState by viewModel.connectionState.observeAsState(ConnectionState.Inactive)

    var showTrustDialog by remember { mutableStateOf(false) }

    LaunchedEffect(serverUrl, apiKey, trustCerts) {
        viewModel.testServer(serverUrl, apiKey, trustCerts)
    }
    LaunchedEffect(connectionState) {
        showTrustDialog =
            connectionState.let {
                it is ConnectionState.Result && it.testResult is TestResult.SelfSignedCertRequired
            }
    }

    val labelWidth = 64.dp

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        stickyHeader {
            Text(
                text = stringResource(R.string.add_server),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillParentMaxWidth(),
            )
        }

        // Server url
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.stashapp_url),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(labelWidth),
                )
                EditTextBox(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    keyboardOptions =
                        KeyboardOptions(
                            autoCorrectEnabled = false,
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Uri,
                        ),
                    keyboardActions = KeyboardActions(),
                    leadingIcon = null,
                    isInputValid = {
                        connectionState.let {
                            when (it) {
                                ConnectionState.DuplicateServer -> false
                                ConnectionState.Inactive -> true
                                is ConnectionState.Result -> it.canConnect
                                ConnectionState.Testing -> true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // API Key
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.animateItem(),
            ) {
                Text(
                    text = stringResource(R.string.stashapp_config_general_auth_api_key),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(labelWidth),
                )
                EditTextBox(
                    value = apiKey ?: "",
                    onValueChange = { apiKey = it },
                    keyboardOptions =
                        KeyboardOptions(
                            autoCorrectEnabled = false,
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = if (showApiKey) KeyboardType.Ascii else KeyboardType.Password,
                        ),
                    keyboardActions = KeyboardActions(),
                    leadingIcon = null,
                    enabled =
                        connectionState.let { it is ConnectionState.Result && it.testResult is TestResult.AuthRequired } ||
                            apiKey.isNotNullOrBlank(),
                    isInputValid = {
                        apiKey.isNullOrEmpty() ||
                            connectionState.let {
                                when (it) {
                                    ConnectionState.DuplicateServer -> true
                                    ConnectionState.Inactive -> true
                                    is ConnectionState.Result -> it.testResult is TestResult.Success
                                    ConnectionState.Testing -> true
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            StatusText(connectionState)
        }

        item {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier =
                    Modifier
                        .fillParentMaxWidth()
                        .animateItem(),
            ) {
                SwitchWithLabel(
                    label = "Show API Key",
                    checked = showApiKey,
                    onStateChange = { showApiKey = it },
                    modifier = Modifier,
                )
            }
        }

        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillParentMaxWidth(),
            ) {
                SubmitServerButton(
                    connectionState = connectionState,
                    serverUrl = serverUrl,
                    apiKey = apiKey,
                    trustCerts = trustCerts,
                    onSubmit = onSubmit,
                    modifier = Modifier,
                )
            }
        }
    }
    AnimatedVisibility(showTrustDialog) {
        AllowSelfSignedCertsDialog(
            onDismissRequest = { showTrustDialog = false },
            onEnableTrust = {
                PreferenceManager.getDefaultSharedPreferences(context).edit(true) {
                    putBoolean(context.getString(R.string.pref_key_trust_certs), true)
                }
                trustCerts = true
            },
        )
    }
}

@Composable
fun SubmitServerButton(
    connectionState: ConnectionState,
    serverUrl: String?,
    apiKey: String?,
    trustCerts: Boolean,
    onSubmit: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var testing by remember { mutableStateOf(false) }

    Button(
        onClick = {
            scope.launch(StashCoroutineExceptionHandler()) {
                try {
                    testing = true
                    if (serverUrl.isNotNullOrBlank()) {
                        val server = StashServer(serverUrl, apiKey?.ifBlank { null })
                        val apolloClient =
                            StashClient.createTestApolloClient(
                                context,
                                server,
                                trustCerts,
                            )
                        val testResult =
                            testStashConnection(
                                context,
                                false,
                                apolloClient,
                            )
                        if (testResult is TestResult.Success) {
                            onSubmit.invoke(server)
                        }
                    }
                } finally {
                    testing = false
                }
            }
        },
        enabled = !testing && connectionState.let { it is ConnectionState.Result && it.testResult is TestResult.Success },
        modifier = modifier,
    ) {
        if (testing || connectionState is ConnectionState.Testing) {
            CircularProgress(Modifier.size(32.dp), false)
        } else {
            Text(
                text = stringResource(R.string.stashapp_actions_submit),
//                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
fun StatusText(
    state: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val message =
        state.let {
            when (it) {
                is ConnectionState.Result -> it.testResult.message.ifBlank { null }
                is ConnectionState.DuplicateServer -> "Duplicate server"
                else -> null
            }
        }
    if (message.isNotNullOrBlank()) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
    } else if (state is ConnectionState.Result && state.testResult is TestResult.Success) {
        Text(
            text = stringResource(R.string.success),
            color = Color.Green.copy(alpha = .66f),
            modifier = modifier,
        )
    }
}

@Composable
fun AllowSelfSignedCertsDialog(
    onDismissRequest: () -> Unit,
    onEnableTrust: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
            modifier =
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                    ),
        ) {
            item {
                Text(
                    text =
                        "The server may be using a self-signed certificate. Do you want to trust self-signed certificates?\n\n" +
                            "Note: if enabled, the app must be restarted/force stopped after completing setup!",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier,
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier =
                        Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                ) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.focusRequester(focusRequester),
                    ) {
                        Text(
                            text = "No, go back",
                        )
                    }
                    Button(
                        onClick = {
                            onDismissRequest.invoke()
                            onEnableTrust.invoke()
                        },
                        modifier = Modifier,
                    ) {
                        Text(
                            text = "Yes",
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AddServerPreview() {
    AppTheme {
        AllowSelfSignedCertsDialog(
            {},
            {},
        )
    }
}
