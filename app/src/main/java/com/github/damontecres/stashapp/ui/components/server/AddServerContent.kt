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
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.TestResultStatus
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.delay

@Composable
fun AddServer(
    currentServerUrls: List<String>,
    onSubmit: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var serverUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf<String?>(null) }
    var showApiKey by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<TestResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var checkingConnection by remember { mutableStateOf(false) }

    LaunchedEffect(serverUrl, apiKey) {
        if (serverUrl.isNotNullOrBlank()) {
            checkingConnection = true
            errorMessage = null
            delay(500L)
            result =
                if (serverUrl.isNotNullOrBlank()) {
                    val apolloClient =
                        StashClient.createTestApolloClient(
                            context,
                            StashServer(serverUrl, apiKey?.ifBlank { null }),
                            false, // TODO
                        )
                    testStashConnection(context, false, apolloClient)
                } else {
                    null
                }
            if (serverUrl in currentServerUrls) {
                errorMessage = "Duplicate server"
            } else {
                errorMessage =
                    when (result?.status) {
                        TestResultStatus.SUCCESS -> null
                        TestResultStatus.AUTH_REQUIRED -> null
                        TestResultStatus.ERROR -> "Error"
                        TestResultStatus.UNSUPPORTED_VERSION -> "Unsupported server version"
                        TestResultStatus.SSL_REQUIRED -> "HTTPS may be required"
                        TestResultStatus.SELF_SIGNED_REQUIRED -> "Trust ssl certificates required"
                        null -> null
                    }
            }

            checkingConnection = false
        }
    }

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
                    modifier = Modifier.width(160.dp),
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
                            result?.status in
                            setOf(
                                TestResultStatus.SUCCESS,
                                TestResultStatus.AUTH_REQUIRED,
                            )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (result?.status == TestResultStatus.AUTH_REQUIRED ||
            result?.status == TestResultStatus.SUCCESS &&
            apiKey.isNotNullOrBlank()
        ) {
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
                        modifier = Modifier.width(160.dp),
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
                        isInputValid = {
                            result?.status == TestResultStatus.SUCCESS
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
        }
        errorMessage?.let {
            item {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillParentMaxWidth(),
            ) {
                Button(
                    onClick = { onSubmit.invoke(StashServer(serverUrl, apiKey?.ifBlank { null })) },
                    enabled = result?.status == TestResultStatus.SUCCESS && serverUrl !in currentServerUrls,
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
