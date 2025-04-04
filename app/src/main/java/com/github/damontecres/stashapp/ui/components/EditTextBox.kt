package com.github.damontecres.stashapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldDefaults.Container
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.MainTheme
import com.github.damontecres.stashapp.ui.Material3MainTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    height: Dp = 40.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Material3MainTheme {
        val colors =
            TextFieldDefaults.colors(
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f),
                errorContainerColor = MaterialTheme.colorScheme.errorContainer,
                focusedIndicatorColor = MaterialTheme.colorScheme.border,
                unfocusedLabelColor = Color.Unspecified,
            )
        CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
            BasicTextField(
                value = value,
                modifier =
                    modifier
                        .defaultMinSize(
                            minWidth = TextFieldDefaults.MinWidth,
                            minHeight = height,
                        ).height(height),
                onValueChange = onValueChange,
                enabled = enabled,
                readOnly = readOnly,
                textStyle = MaterialTheme.typography.bodyLarge.merge(MaterialTheme.colorScheme.onPrimaryContainer),
                cursorBrush = SolidColor(colors.cursorColor),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interactionSource,
                singleLine = true,
                maxLines = 1,
                minLines = 1,
                decorationBox =
                    @Composable { innerTextField ->
                        // places leading icon, text field with label and placeholder, trailing icon
                        TextFieldDefaults.DecorationBox(
                            value = value,
                            visualTransformation = VisualTransformation.None,
                            innerTextField = innerTextField,
                            placeholder = null,
                            label = null,
                            leadingIcon = leadingIcon,
                            trailingIcon = null,
                            prefix = null,
                            suffix = null,
                            supportingText = null,
                            shape = CircleShape,
                            singleLine = true,
                            enabled = enabled,
                            isError = false,
                            interactionSource = interactionSource,
                            colors = colors,
                            contentPadding =
                                PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 10.dp,
                                ),
                            container = {
                                Container(
                                    enabled = enabled,
                                    isError = false,
                                    interactionSource = interactionSource,
                                    modifier = Modifier,
                                    colors = colors,
                                    shape = CircleShape,
                                    focusedIndicatorLineThickness = 4.dp,
                                    unfocusedIndicatorLineThickness = 0.dp,
                                )
                            },
                        )
                    },
            )
        }
    }
}

@Composable
fun SearchEditTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    height: Dp = 40.dp,
) {
    EditTextBox(
        value,
        onValueChange,
        modifier,
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = ImeAction.Search,
            ),
        keyboardActions =
            KeyboardActions(
                onSearch = {
                    onSearchClick.invoke()
                    this.defaultKeyboardAction(ImeAction.Done)
                },
            ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.stashapp_actions_search),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        enabled,
        readOnly,
        height,
    )
}

@Preview
@Composable
private fun EditTextBoxPreview() {
    MainTheme {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(Color.LightGray).padding(4.dp),
        ) {
            Button(onClick = {}) { Text(text = "Create Filter") }
            EditTextBox(
                value = "Sample query",
                onValueChange = {},
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.stashapp_actions_search),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
            )
        }
    }
}
