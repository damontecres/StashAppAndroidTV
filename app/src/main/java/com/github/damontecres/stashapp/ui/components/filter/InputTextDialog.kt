package com.github.damontecres.stashapp.ui.components.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.components.EditTextBox

@Composable
fun InputTextDialog(
    onDismissRequest: () -> Unit,
    action: InputTextAction,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(action.value ?: "") }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = true,
            ),
    ) {
//        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
//        dialogWindowProvider?.window?.let { window ->
//            window.setDimAmount(0f)
//        }
        LazyColumn(
            modifier =
                modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        shape = RoundedCornerShape(16.dp),
                    ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = action.title,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            item {
                EditTextBox(
                    value = text,
                    onValueChange = { text = it },
                    isInputValid = action.isValid,
                    keyboardActions =
                        KeyboardActions(
                            onGo = {
                                action.onSubmit.invoke(text)
                                onDismissRequest.invoke()
                            },
                        ),
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = ImeAction.Go,
                            keyboardType = action.keyboardType,
                        ),
                )
            }
            item {
                Button(
                    enabled = action.isValid.invoke(text),
                    onClick = {
                        action.onSubmit.invoke(text)
                        onDismissRequest.invoke()
                    },
                ) {
                    Text(text = stringResource(R.string.stashapp_actions_submit))
                }
            }
        }
    }
}
