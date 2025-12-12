package com.github.damontecres.stashapp.ui.components.server

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.PreviewTheme
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.ui.components.EditTextBox
import com.github.damontecres.stashapp.util.isNotNullOrBlank

@Composable
fun ConfigurePin(
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes descriptionString: Int = R.string.set_app_pin_code,
    @StringRes submitString: Int = R.string.stashapp_actions_submit,
    @StringRes cancelString: Int = R.string.stashapp_actions_skip,
) {
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }

    val valid = pin1.isBlank() || (pin1.toIntOrNull() != null && pin1 == pin2)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier,
    ) {
        item {
            Text(
                text = "Set a PIN code?",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillParentMaxWidth(),
            )
        }
        item {
            Text(
                text = stringResource(descriptionString),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PIN",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(104.dp),
                )
                EditTextBox(
                    value = pin1,
                    onValueChange = { pin1 = it },
                    keyboardOptions =
                        KeyboardOptions(
                            autoCorrectEnabled = false,
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions = KeyboardActions(),
                    leadingIcon = null,
                    isInputValid = { true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Confirm PIN",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(104.dp),
                )
                EditTextBox(
                    value = pin2,
                    onValueChange = { pin2 = it },
                    keyboardOptions =
                        KeyboardOptions(
                            autoCorrectEnabled = false,
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                if (valid) {
                                    onSubmit(pin1)
                                }
                            },
                        ),
                    leadingIcon = null,
                    isInputValid = { valid },
                    enabled = pin1.isNotNullOrBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onCancel,
                    enabled = true,
                ) {
                    Text(
                        text = stringResource(cancelString),
                    )
                }
                Button(
                    onClick = { onSubmit.invoke(pin1) },
                    enabled = pin1.isNotBlank() && valid,
                ) {
                    Text(
                        text = stringResource(submitString),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConfigurePinPreview() {
    PreviewTheme {
        ConfigurePin(
            {},
            {},
            Modifier,
            descriptionString = R.string.read_only_pin_description,
        )
    }
}
