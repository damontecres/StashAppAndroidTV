package com.github.damontecres.stashapp.ui.components.filter

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.isNotNullOrBlank

@Composable
fun SimpleListItem(
    title: String,
    subtitle: String?,
    showArrow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    enabled: Boolean = true,
    selected: Boolean = false,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    ListItem(
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        onLongClick = {},
        leadingContent = leadingContent,
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        supportingContent = {
            if (subtitle.isNotNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    modifier = Modifier.enableMarquee(interactionSource.collectIsFocusedAsState().value),
                )
            }
        },
        trailingContent = {
            if (showArrow) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                )
            }
        },
        interactionSource = interactionSource,
    )
}
