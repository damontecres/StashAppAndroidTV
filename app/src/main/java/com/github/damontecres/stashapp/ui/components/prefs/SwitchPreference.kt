package com.github.damontecres.stashapp.ui.components.prefs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.damontecres.stashapp.ui.compat.ListItem

@Composable
fun SwitchPreference(
    title: String,
    value: Boolean,
    onClick: () -> Unit,
    summaryOn: String?,
    summaryOff: String?,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) = SwitchPreference(
    title = title,
    value = value,
    onClick = onClick,
    modifier = modifier,
    summary = if (value) summaryOn else summaryOff,
    onLongClick = onLongClick,
)

@Composable
fun SwitchPreference(
    title: String,
    value: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    ListItem(
        selected = false,
        dense = true,
        onClick = onClick,
        onLongClick = onLongClick,
        headlineContent = {
            PreferenceTitle(title)
        },
        supportingContent = {
            PreferenceSummary(summary)
        },
        trailingContent = {
            androidx.tv.material3.Switch(
                checked = value,
                onCheckedChange = { },
                colors =
                    androidx.tv.material3.SwitchDefaults
                        .colors(),
            )
        },
        modifier = modifier,
    )
}
