package com.github.damontecres.stashapp.ui.components.filter

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.views.getString

@Composable
fun CriterionModifierPickerDialog(
    filterName: String,
    allowedModifiers: List<CriterionModifier>,
    onClick: (CriterionModifier) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val dialogItems =
        allowedModifiers.map {
            DialogItem(it.getString(context)) { onClick.invoke(it) }
        }
    DialogPopup(
        showDialog = true,
        title = filterName + " " + stringResource(R.string.modifier),
        onDismissRequest = onDismiss,
        dialogItems = dialogItems,
        waitToLoad = false,
    )
}

@Composable
fun StringPicker2(
    name: String,
    value: StringCriterionInput,
    removeEnabled: Boolean,
    onChangeCriterionModifier: () -> Unit,
    onChangeValue: () -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LazyColumn(modifier = modifier) {
        stickyHeader {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillParentMaxWidth(),
            )
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.modifier),
                subtitle = value.modifier.getString(context),
                showArrow = true,
                onClick = onChangeCriterionModifier,
            )
        }
        if (value.modifier != CriterionModifier.IS_NULL && value.modifier != CriterionModifier.NOT_NULL) {
            item {
                SimpleListItem(
                    title = stringResource(R.string.stashapp_criterion_value),
                    subtitle = value.value,
                    showArrow = true,
                    onClick = onChangeValue,
                    modifier = Modifier.animateItem(),
                )
            }
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.stashapp_actions_save),
                subtitle = null,
                showArrow = false,
                onClick = onSave,
            )
        }
        if (removeEnabled) {
            // If initial value is not null, then show option to remove it
            item {
                // TODO add remove icon
                SimpleListItem(
                    title = stringResource(R.string.stashapp_actions_remove),
                    subtitle = null,
                    showArrow = false,
                    onClick = onRemove,
                )
            }
        }
    }
}

@Composable
fun StringPicker(
    filterOption: FilterOption<StashDataFilter, StringCriterionInput>,
    initial: StringCriterionInput?,
    onSave: (StringCriterionInput?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showModifierDialog by remember { mutableStateOf(false) }
    var inputTextAction by remember { mutableStateOf<InputTextAction?>(null) }

    val allowedModifiers =
        filterOption.allowedModifiers ?: listOf(
            CriterionModifier.EQUALS,
            CriterionModifier.NOT_EQUALS,
            CriterionModifier.INCLUDES,
            CriterionModifier.EXCLUDES,
            CriterionModifier.IS_NULL,
            CriterionModifier.NOT_NULL,
            CriterionModifier.MATCHES_REGEX,
            CriterionModifier.NOT_MATCHES_REGEX,
        )
    var value by
        remember {
            mutableStateOf(
                initial ?: StringCriterionInput(
                    value = "",
                    modifier = allowedModifiers[0],
                ),
            )
        }
    LazyColumn(modifier = modifier) {
        stickyHeader {
            Text(
                text = stringResource(filterOption.nameStringId),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillParentMaxWidth(),
            )
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.modifier),
                subtitle = value.modifier.getString(context),
                showArrow = true,
                onClick = { showModifierDialog = true },
            )
        }
        if (value.modifier != CriterionModifier.IS_NULL && value.modifier != CriterionModifier.NOT_NULL) {
            item {
                SimpleListItem(
                    title = stringResource(R.string.stashapp_criterion_value),
                    subtitle = value.value,
                    showArrow = true,
                    onClick = {
                        inputTextAction =
                            InputTextAction(
                                title = context.getString(filterOption.nameStringId),
                                value = value.value,
                                keyboardType = KeyboardType.Text,
                                onSubmit = { value = value.copy(value = it) },
                            )
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.stashapp_actions_save),
                subtitle = null,
                showArrow = false,
                onClick = { onSave.invoke(value) },
            )
        }
        if (initial != null) {
            // If initial value is not null, then show option to remove it
            item {
                // TODO add remove icon
                SimpleListItem(
                    title = stringResource(R.string.stashapp_actions_remove),
                    subtitle = null,
                    showArrow = false,
                    onClick = { onSave.invoke(null) },
                )
            }
        }
    }
    if (showModifierDialog) {
        CriterionModifierPickerDialog(
            filterName = stringResource(filterOption.nameStringId),
            allowedModifiers = allowedModifiers,
            onDismiss = { showModifierDialog = false },
            onClick = { value = value.copy(modifier = it) },
        )
    }
    inputTextAction?.let {
        InputTextDialog(
            onDismissRequest = { inputTextAction = null },
            action = it,
            modifier = Modifier,
        )
    }
}
