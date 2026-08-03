package com.github.damontecres.stashapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R

/**
 * Displays an error message and/or exception
 */
@Composable
fun ErrorMessage(
    message: String?,
    exception: Throwable?,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.stashapp_errors_header),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
        )
        message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        exception?.localizedMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        var cause = exception?.cause
        while (cause != null) {
            cause.localizedMessage?.let {
                Text(
                    text = "Caused by: $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            cause = cause.cause
        }
    }
}
