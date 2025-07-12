package com.github.damontecres.stashapp.ui.components.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddServer(
    currentServerUrls: List<String>,
    onSubmit: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf<String?>(null) }
    var showApiKey by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<TestResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var checkingConnection by remember { mutableStateOf(false) }

    val trustCerts = remember { getPreference(context, R.string.pref_key_trust_certs, false) }

    LaunchedEffect(serverUrl, apiKey) {
        result = null
        errorMessage = null
        if (serverUrl.isNotNullOrBlank()) {
            try {
                checkingConnection = true
                errorMessage = null
                delay(500L)
                if (serverUrl in currentServerUrls) {
                    errorMessage = "Duplicate server"
                } else {
                    result =
                        if (serverUrl.isNotNullOrBlank()) {
                            val apolloClient =
                                StashClient.createTestApolloClient(
                                    context,
                                    StashServer(serverUrl, apiKey?.ifBlank { null }),
                                    trustCerts,
                                )
                            testStashConnection(context, false, apolloClient)
                        } else {
                            null
                        }
                    errorMessage = result?.message
                }
            } catch (ex: Exception) {
                result = TestResult.Error(ex.localizedMessage, ex)
                errorMessage = ex.localizedMessage
            } finally {
                checkingConnection = false
            }
        } else {
            checkingConnection = false
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
                text = "Add server",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillParentMaxWidth(),
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "URL:",
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
                        result == null ||
                            result is TestResult.Success ||
                            result is TestResult.AuthRequired
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.animateItem(),
            ) {
                Text(
                    text = "API Key:",
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
                        result is TestResult.AuthRequired || apiKey.isNotNullOrBlank(),
                    isInputValid = {
                        apiKey.isNullOrEmpty() || result is TestResult.Success
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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

        errorMessage?.let {
            if (it.isNotNullOrBlank()) {
                item {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
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
                        scope.launch(StashCoroutineExceptionHandler()) {
                            try {
                                checkingConnection = true
                                val server = StashServer(serverUrl, apiKey?.ifBlank { null })
                                if (serverUrl.isNotNullOrBlank()) {
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
                                checkingConnection = false
                            }
                        }
                    },
                    enabled = !checkingConnection && result is TestResult.Success && serverUrl !in currentServerUrls,
                    modifier = Modifier,
                ) {
                    if (checkingConnection) {
                        CircularProgress(Modifier.size(32.dp), false)
                    } else {
                        Text(
                            text = stringResource(R.string.stashapp_actions_submit),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
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
        AddServer(
            currentServerUrls = listOf(),
            onSubmit = {},
            modifier = Modifier,
        )
    }
}
