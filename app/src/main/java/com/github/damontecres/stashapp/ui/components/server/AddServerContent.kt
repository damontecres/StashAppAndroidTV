package com.github.damontecres.stashapp.ui.components.server

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.PreviewTheme
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.EditTextBox
import com.github.damontecres.stashapp.ui.components.SwitchWithLabel
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashPreferencesSerializer
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.preferences
import com.github.damontecres.stashapp.util.updateAdvancedPreferences
import kotlinx.coroutines.launch

private const val TAG = "AddServer"

@Composable
fun AddServer(
    onSubmit: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageServersViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences by context.preferences.data.collectAsState(StashPreferencesSerializer.defaultValue)

    val testButtonFocusRequester = remember { FocusRequester() }

    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf<String?>(null) }
    var showApiKey by remember { mutableStateOf(false) }
    var usePassword by remember { mutableStateOf(false) }

    var trustCerts by remember {
        mutableStateOf(preferences.advancedPreferences.trustSelfSignedCertificates)
    }

    val connectionState by viewModel.connectionState.observeAsState(ConnectionState.Inactive)

    var showTrustDialog by remember { mutableStateOf(false) }

    LaunchedEffect(serverUrl, apiKey, trustCerts, username) {
        viewModel.clearConnectionStatus()
    }
    LaunchedEffect(connectionState) {
        connectionState.let {
            if (it is ConnectionState.Result && it.testResult is TestResult.Success) {
                Log.i(TAG, "Connection to $serverUrl successful!")
                onSubmit.invoke(StashServer(serverUrl, apiKey))
            } else if (it is ConnectionState.NewApiKey) {
                Log.i(TAG, "Connection to $serverUrl successful with new API key!")
                onSubmit.invoke(StashServer(serverUrl, it.apiKey))
            } else if (it is ConnectionState.Result && it.testResult is TestResult.SelfSignedCertRequired) {
                showTrustDialog = true
            }
        }
    }

    val labelWidth = if (usePassword) 88.dp else 72.dp

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.focusGroup(),
    ) {
        stickyHeader {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.fillParentMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.add_server),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillParentMaxWidth(),
                )

                Icon(
                    painter = painterResource(R.drawable.stash_logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(64.dp),
                )
            }
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
                            imeAction = ImeAction.Next,
                            showKeyboardOnFocus = true,
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
                                is ConnectionState.NewApiKey -> true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Username
        if (usePassword) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.stashapp_config_general_auth_username),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.width(labelWidth),
                    )
                    EditTextBox(
                        value = username,
                        onValueChange = { username = it },
                        keyboardOptions =
                            KeyboardOptions(
                                autoCorrectEnabled = false,
                                capitalization = KeyboardCapitalization.None,
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Next,
                                showKeyboardOnFocus = false,
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
                                    is ConnectionState.NewApiKey -> true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Password/API Key
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.animateItem(),
            ) {
                Text(
                    text =
                        stringResource(
                            if (usePassword) {
                                R.string.stashapp_config_general_auth_password
                            } else {
                                R.string.stashapp_config_general_auth_api_key
                            },
                        ),
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
                            imeAction = ImeAction.Next,
                            showKeyboardOnFocus = false,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = {
                                testButtonFocusRequester.tryRequestFocus()
                            },
                        ),
                    leadingIcon = null,
                    enabled = true,
                    isInputValid = {
                        apiKey.isNullOrEmpty() ||
                            connectionState.let {
                                when (it) {
                                    ConnectionState.DuplicateServer -> true
                                    ConnectionState.Inactive -> true
                                    is ConnectionState.Result -> it.testResult is TestResult.Success
                                    ConnectionState.Testing -> true
                                    is ConnectionState.NewApiKey -> true
                                }
                            }
                    },
                    placeholder =
                        if (usePassword) {
                            null
                        } else {
                            {
                                Text(
                                    text = "Optional, if needed",
                                    color =
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                            alpha = .25f,
                                        ),
                                )
                            }
                        },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (usePassword) {
            item {
                Text(
                    text = "Note: an API key will be fetched or generated for the server",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SwitchWithLabel(
                        label = "Use username",
                        checked = usePassword,
                        onStateChange = {
                            apiKey = null
                            usePassword = it
                        },
                        modifier = Modifier,
                    )
                    SwitchWithLabel(
                        label = if (usePassword) "Show password" else "Show API Key",
                        checked = showApiKey,
                        onStateChange = { showApiKey = it },
                        modifier = Modifier,
                    )
                }
            }
        }

        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillParentMaxWidth(),
            ) {
                Button(
                    onClick = {
                        viewModel.testServer(serverUrl, apiKey, trustCerts, username, usePassword)
                    },
                    enabled = serverUrl.isNotNullOrBlank() && connectionState == ConnectionState.Inactive,
                    modifier = Modifier.focusRequester(testButtonFocusRequester),
                ) {
                    if (connectionState is ConnectionState.Testing) {
                        CircularProgress(Modifier.size(32.dp), false)
                    } else {
                        Text(
                            text = stringResource(R.string.test_connection),
//                color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
    AnimatedVisibility(showTrustDialog) {
        AllowSelfSignedCertsDialog(
            onDismissRequest = { showTrustDialog = false },
            onEnableTrust = {
                scope.launch(StashCoroutineExceptionHandler()) {
                    context.preferences.updateData {
                        it.updateAdvancedPreferences {
                            trustSelfSignedCertificates = true
                        }
                    }
                }
                trustCerts = true
            },
        )
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
    PreviewTheme {
        AllowSelfSignedCertsDialog(
            {},
            {},
        )
    }
}
