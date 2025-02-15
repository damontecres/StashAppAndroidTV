package com.github.damontecres.stashapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.ui.FontAwesome
import kotlinx.coroutines.delay

data class DialogItem(
    val headlineContent: @Composable () -> Unit,
    val onClick: () -> Unit,
    val overlineContent: @Composable (() -> Unit)? = null,
    val supportingContent: @Composable (() -> Unit)? = null,
    val leadingContent: @Composable (BoxScope.() -> Unit)? = null,
    val trailingContent: @Composable (() -> Unit)? = null,
) {
    constructor(
        text: String,
        @StringRes iconStringRes: Int,
        onClick: () -> Unit,
    ) : this(
        headlineContent = {
            Text(
                text = text,
//                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        leadingContent = {
            Text(
                text = stringResource(id = iconStringRes),
//                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontFamily = FontAwesome,
            )
        },
        onClick = onClick,
    )

    constructor(
        text: String,
        icon: ImageVector,
        onClick: () -> Unit,
    ) : this(
        headlineContent = {
            Text(
                text = text,
//                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = text,
//                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        onClick = onClick,
    )

    constructor(
        text: String,
        onClick: () -> Unit,
    ) : this(
        headlineContent = {
            Text(
                text = text,
//                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        onClick = onClick,
    )
}

@Composable
fun DialogPopup(
    showDialog: Boolean,
    title: String,
    items: List<DialogItem>,
    onDismissRequest: () -> Unit,
    dismissOnClick: Boolean = true,
    waitToLoad: Boolean = true,
) {
    var waiting by remember { mutableStateOf(waitToLoad) }
    if (showDialog) {
        if (waitToLoad) {
            LaunchedEffect(Unit) {
                // This is a hack because a long click will propagate here and click the first list item
                // So this disables the list items assuming the user will stop pressing when the dialog appears
                waiting = true
                delay(500)
                waiting = false
            }
        } else {
            waiting = false
        }
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(),
        ) {
            val elevatedContainerColor = MaterialTheme.colorScheme.secondaryContainer
            Column(
                modifier =
                    Modifier
//                        .widthIn(min = 520.dp, max = 300.dp)
//                        .dialogFocusable()
                        .graphicsLayer {
                            this.clip = true
                            this.shape = RoundedCornerShape(28.0.dp)
                        }.drawBehind { drawRect(color = elevatedContainerColor) }
                        .padding(PaddingValues(24.dp)),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                items.forEach {
                    ListItem(
                        selected = false,
                        enabled = !waiting,
                        onClick = {
                            if (dismissOnClick) {
                                onDismissRequest.invoke()
                            }
                            it.onClick.invoke()
                        },
                        headlineContent = it.headlineContent,
                        overlineContent = it.overlineContent,
                        supportingContent = it.supportingContent,
                        leadingContent = it.leadingContent,
                        trailingContent = it.trailingContent,
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}
