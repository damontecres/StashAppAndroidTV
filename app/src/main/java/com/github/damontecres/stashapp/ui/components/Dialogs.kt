package com.github.damontecres.stashapp.ui.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import kotlinx.coroutines.delay

data class DialogItem(
    val text: String,
    val onClick: () -> Unit,
)

@Composable
fun DialogPopup(
    showDialog: Boolean,
    title: String,
    items: List<DialogItem>,
    onDismissRequest: () -> Unit,
    dismissOnClick: Boolean = true,
) {
    var waiting by remember { mutableStateOf(true) }
    if (showDialog) {
        LaunchedEffect(Unit) {
            // This is a hack because a long click will propagate here and click the first list item
            // So this disables the list items assuming the user will stop pressing when the dialog appears
            waiting = true
            delay(500)
            waiting = false
        }
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(),
        ) {
            val elevatedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
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
                        headlineContent = {
                            Text(text = it.text)
                        },
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}
