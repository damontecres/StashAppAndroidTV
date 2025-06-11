package com.github.damontecres.stashapp.ui.components.filter

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.ui.between
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.nullCheck
import com.github.damontecres.stashapp.ui.valid
import com.github.damontecres.stashapp.util.isNotNullOrBlank
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
fun StringPicker(
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
        if (!value.modifier.nullCheck) {
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
                enabled = value.value.isNotNullOrBlank() || value.modifier.nullCheck,
            )
        }
        if (removeEnabled) {
            // If initial value is not null, then show option to remove it
            item {
                SimpleListItem(
                    title = stringResource(R.string.stashapp_actions_remove),
                    subtitle = null,
                    showArrow = false,
                    onClick = onRemove,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.stashapp_actions_remove),
                            tint = Color.Red,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun IntPicker(
    name: String,
    value: IntCriterionInput,
    removeEnabled: Boolean,
    onChangeCriterionModifier: () -> Unit,
    onChangeValue: (Boolean) -> Unit,
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
        if (!value.modifier.nullCheck) {
            item {
                SimpleListItem(
                    title =
                        if (value.modifier.between) {
                            stringResource(R.string.stashapp_criterion_greater_than)
                        } else {
                            stringResource(
                                R.string.stashapp_criterion_value,
                            )
                        },
                    subtitle = value.value.toString(),
                    showArrow = true,
                    onClick = { onChangeValue(false) },
                    modifier = Modifier.animateItem(),
                )
            }
            if (value.modifier.between) {
                item {
                    SimpleListItem(
                        title = stringResource(R.string.stashapp_criterion_modifier_less_than),
                        subtitle = value.value2.getOrNull()?.toString(),
                        showArrow = true,
                        onClick = { onChangeValue(true) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.stashapp_actions_save),
                subtitle = null,
                showArrow = false,
                onClick = onSave,
                enabled = value.valid,
            )
        }
        if (removeEnabled) {
            // If initial value is not null, then show option to remove it
            item {
                SimpleListItem(
                    title = stringResource(R.string.stashapp_actions_remove),
                    subtitle = null,
                    showArrow = false,
                    onClick = onRemove,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.stashapp_actions_remove),
                            tint = Color.Red,
                        )
                    },
                )
            }
        }
    }
}
