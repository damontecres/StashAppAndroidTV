package com.github.damontecres.stashapp.ui.components.filter

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.ui.between
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.nullCheck
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
fun SelectFromListDialog(
    action: SelectFromListAction,
    onSubmit: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedIndices =
        remember {
            mutableListOf(
                *action.currentOptions
                    .map { action.options.indexOf(it) }
                    .toTypedArray(),
            )
        }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(),
    ) {
        val elevatedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        LazyColumn(
            modifier =
                Modifier
                    .graphicsLayer {
                        this.clip = true
                        this.shape = RoundedCornerShape(28.0.dp)
                    }.drawBehind { drawRect(color = elevatedContainerColor) }
                    .padding(PaddingValues(24.dp)),
        ) {
            stickyHeader {
                Text(
                    text = action.filterName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            itemsIndexed(action.options) { index, item ->
                SimpleListItem(
                    title = item,
                    subtitle = null,
                    showArrow = false,
                    onClick = {
                        if (action.multiSelect) {
                            if (index in selectedIndices) {
                                selectedIndices.remove(index)
                            } else {
                                selectedIndices.add(index)
                            }
                        } else {
                            onSubmit.invoke(listOf(index))
                        }
                    },
                    modifier = Modifier,
                    leadingContent = {
                        if (index in selectedIndices) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.stashapp_actions_enable),
                            )
                        }
                    },
                )
            }
            if (action.multiSelect) {
                item {
                    HorizontalDivider(Modifier.height(16.dp))
                }
                item {
                    SimpleListItem(
                        title = stringResource(R.string.stashapp_actions_submit),
                        subtitle = null,
                        showArrow = true,
                        onClick = { onSubmit.invoke(selectedIndices) },
                        modifier = Modifier,
                        leadingContent = {},
                    )
                }
            }
        }
    }
}

interface SimpleCriterionInput<T : Comparable<T>> {
    val value: T
    val value2: T?
    val modifier: CriterionModifier

    fun isValid(): Boolean {
        if (modifier.between) {
            val val2 = value2
            return val2 != null && value < val2
        } else {
            return true
        }
    }
}

class SimpleIntCriterionInput(
    val input: IntCriterionInput,
) : SimpleCriterionInput<Int> {
    override val value: Int = input.value
    override val value2: Int? = input.value2.getOrNull()
    override val modifier: CriterionModifier = input.modifier
}

class SimpleFloatCriterionInput(
    val input: FloatCriterionInput,
) : SimpleCriterionInput<Double> {
    override val value: Double = input.value
    override val value2: Double? = input.value2.getOrNull()
    override val modifier: CriterionModifier = input.modifier
}

class SimpleStringCriterionInput(
    val input: StringCriterionInput,
) : SimpleCriterionInput<String> {
    override val value: String = input.value
    override val value2: String? = null
    override val modifier: CriterionModifier = input.modifier
}

@Composable
fun <T : Comparable<T>> CriterionInputPicker(
    name: String,
    value: SimpleCriterionInput<T>,
    removeEnabled: Boolean,
    onChangeCriterionModifier: () -> Unit,
    onChangeValue: () -> Unit,
    onChangeValue2: () -> Unit,
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
                    onClick = { onChangeValue() },
                    modifier = Modifier.animateItem(),
                )
            }
            if (value.modifier.between) {
                item {
                    SimpleListItem(
                        title = stringResource(R.string.stashapp_criterion_modifier_less_than),
                        subtitle = value.value2?.toString(),
                        showArrow = true,
                        onClick = { onChangeValue2() },
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
                enabled = value.isValid(),
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
fun SelectFromListPicker(
    name: String,
    values: List<String>,
    criterionModifier: CriterionModifier,
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
                subtitle = criterionModifier.getString(context),
                showArrow = true,
                onClick = onChangeCriterionModifier,
            )
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.stashapp_criterion_value),
                subtitle = values.joinToString(", "),
                showArrow = true,
                onClick = onChangeValue,
            )
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.stashapp_actions_save),
                subtitle = null,
                showArrow = false,
                onClick = onSave,
                enabled = values.isNotEmpty(),
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
