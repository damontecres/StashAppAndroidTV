package com.github.damontecres.stashapp.ui.components.prefs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.damontecres.stashapp.ui.compat.ListItem

@Composable
fun ClickPreference(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    ListItem(
        dense = true,
        selected = false,
        onClick = onClick,
        onLongClick = onLongClick,
        headlineContent = {
            PreferenceTitle(title)
        },
        supportingContent = {
            PreferenceSummary(summary)
        },
        modifier = modifier,
    )
}
