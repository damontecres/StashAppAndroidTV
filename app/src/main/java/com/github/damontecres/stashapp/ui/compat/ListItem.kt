package com.github.damontecres.stashapp.ui.compat

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.ListItemBorder
import androidx.tv.material3.ListItemColors
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.ListItemGlow
import androidx.tv.material3.ListItemScale
import androidx.tv.material3.ListItemShape
import androidx.tv.material3.LocalContentColor
import com.github.damontecres.stashapp.ui.DeviceType
import com.github.damontecres.stashapp.ui.LocalDeviceType

@Composable
fun ListItem(
    selected: Boolean,
    onClick: () -> Unit,
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    tonalElevation: Dp = ListItemDefaults.TonalElevation,
    shape: ListItemShape = ListItemDefaults.shape(),
    colors: ListItemColors = ListItemDefaults.colors(),
    scale: ListItemScale = ListItemDefaults.scale(),
    border: ListItemBorder = ListItemDefaults.border(),
    glow: ListItemGlow = ListItemDefaults.glow(),
    interactionSource: MutableInteractionSource? = null,
    dense: Boolean = false,
) {
    if (LocalDeviceType.current == DeviceType.TV) {
        if (dense) {
            androidx.tv.material3.DenseListItem(
                selected = selected,
                onClick = onClick,
                headlineContent = headlineContent,
                modifier = modifier,
                enabled = enabled,
                onLongClick = onLongClick,
                overlineContent = overlineContent,
                supportingContent = supportingContent,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                tonalElevation = tonalElevation,
                shape = shape,
                colors = colors,
                scale = scale,
                border = border,
                glow = glow,
                interactionSource = interactionSource,
            )
        } else {
            androidx.tv.material3.ListItem(
                selected = selected,
                onClick = onClick,
                headlineContent = headlineContent,
                modifier = modifier,
                enabled = enabled,
                onLongClick = onLongClick,
                overlineContent = overlineContent,
                supportingContent = supportingContent,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                tonalElevation = tonalElevation,
                shape = shape,
                colors = colors,
                scale = scale,
                border = border,
                glow = glow,
                interactionSource = interactionSource,
            )
        }
    } else {
        // TODO this is kind of hack to force tv.Text to use the right color
        CompositionLocalProvider(
            LocalContentColor provides colors.contentColor,
        ) {
            androidx.compose.material3.ListItem(
                headlineContent = headlineContent,
                modifier =
                    modifier.combinedClickable(
                        enabled = enabled,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        indication = LocalIndication.current,
                        interactionSource = interactionSource,
                    ),
                overlineContent = overlineContent,
                supportingContent = supportingContent,
                leadingContent = {
                    leadingContent?.let {
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            it.invoke(this)
                        }
                    }
                },
                trailingContent = trailingContent,
                colors =
                    androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = colors.containerColor,
                        headlineColor = colors.contentColor,
                        leadingIconColor = colors.contentColor,
                        overlineColor = colors.contentColor,
                        supportingColor = colors.contentColor,
                        trailingIconColor = colors.contentColor,
                        disabledHeadlineColor = colors.disabledContentColor,
                        disabledLeadingIconColor = colors.disabledContentColor,
                        disabledTrailingIconColor = colors.disabledContentColor,
                    ),
                //            tonalElevation = TODO(),
//            shadowElevation = TODO()
            )
        }
    }
}
