package com.github.damontecres.stashapp.ui.components.prefs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.PreviewTheme
import com.github.damontecres.stashapp.ui.compat.Button

@Composable
fun ConfirmOldUi(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier,
    ) {
        item {
            Text(
                text = "Switch back to legacy UI?",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillParentMaxWidth(),
            )
        }
        item {
            Text(
                text =
                    "Please report any issues you encountered with the new UI!\n\n" +
                        "Do you want switch to the legacy UI?\n\n" +
                        "Note: the app will restart.",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onCancel,
                ) {
                    Text(
                        text = stringResource(R.string.stashapp_actions_cancel),
                    )
                }
                Button(
                    onClick = onConfirm,
                ) {
                    Text(
                        text = stringResource(R.string.stashapp_actions_confirm),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConfirmOldUiPreview() {
    PreviewTheme {
        ConfirmOldUi(
            onCancel = {},
            onConfirm = {},
        )
    }
}
