package com.github.damontecres.stashapp.ui.components.filter

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.between
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.nullCheck
import com.github.damontecres.stashapp.ui.pages.SearchForDialog
import com.github.damontecres.stashapp.ui.tryRequestFocus
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
            mutableStateListOf(
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
fun GenericCriterionInputPicker(
    name: String,
    criterionModifier: CriterionModifier,
    removeEnabled: Boolean,
    isValid: Boolean,
    onChangeCriterionModifier: () -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
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

        content.invoke(this)

        item {
            SimpleListItem(
                title = stringResource(R.string.stashapp_actions_save),
                subtitle = null,
                showArrow = false,
                onClick = onSave,
                enabled = isValid,
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
    GenericCriterionInputPicker(
        name = name,
        criterionModifier = value.modifier,
        removeEnabled = removeEnabled,
        isValid = value.isValid(),
        onChangeCriterionModifier = onChangeCriterionModifier,
        onSave = onSave,
        onRemove = onRemove,
        modifier = modifier,
    ) {
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
    GenericCriterionInputPicker(
        name = name,
        criterionModifier = criterionModifier,
        removeEnabled = removeEnabled,
        isValid = values.isNotEmpty(),
        onChangeCriterionModifier = onChangeCriterionModifier,
        onSave = onSave,
        onRemove = onRemove,
        modifier = modifier,
    ) {
        item {
            SimpleListItem(
                title = stringResource(R.string.stashapp_criterion_value),
                subtitle = values.joinToString(", "),
                showArrow = true,
                onClick = onChangeValue,
            )
        }
    }
}

@Composable
fun MultiCriterionPicker(
    name: String,
    dataType: DataType,
    criterionModifier: CriterionModifier,
    include: List<String>,
    exclude: List<String>,
    removeEnabled: Boolean,
    includeSubValues: Boolean?,
    onChangeCriterionModifier: () -> Unit,
    onPickInclude: () -> Unit,
    onPickExclude: () -> Unit,
    onIncludeSubValueClick: (() -> Unit)?,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GenericCriterionInputPicker(
        name = name,
        criterionModifier = criterionModifier,
        removeEnabled = removeEnabled,
        isValid = include.isNotEmpty() || exclude.isNotEmpty(),
        onChangeCriterionModifier = onChangeCriterionModifier,
        onSave = onSave,
        onRemove = onRemove,
        modifier = modifier,
    ) {
        if (!criterionModifier.nullCheck) {
            item {
                SimpleListItem(
                    title = stringResource(R.string.stashapp_criterion_modifier_includes),
                    subtitle = include.joinToString(", "),
                    showArrow = true,
                    onClick = onPickInclude,
                )
            }
            item {
                SimpleListItem(
                    title = stringResource(R.string.stashapp_criterion_modifier_excludes),
                    subtitle = exclude.joinToString(", "),
                    showArrow = true,
                    onClick = onPickExclude,
                )
            }
            if (includeSubValues != null && onIncludeSubValueClick != null) {
                item {
                    val subValueTitle =
                        stringResource(
                            when (dataType) {
                                DataType.TAG -> R.string.stashapp_include_sub_tags
                                DataType.STUDIO -> R.string.stashapp_include_sub_studios
                                DataType.GROUP -> R.string.stashapp_include_sub_groups
                                else -> throw IllegalStateException("$dataType not supported")
                            },
                        )
                    SimpleListItem(
                        title = subValueTitle,
                        subtitle = null,
                        showArrow = false,
                        onClick = onIncludeSubValueClick,
                        leadingContent = {
                            Switch(
                                checked = includeSubValues,
                                onCheckedChange = null,
                            )
                        },
                    )
                }
            }
        }
    }
}

data class IdName(
    val id: String,
    val name: String,
)

@Composable
fun MultiCriterionPickerDialog(
    uiConfig: ComposeUiConfig,
    dataType: DataType,
    name: String,
    initialValues: List<IdName>,
    onSave: (List<IdName>) -> Unit,
    onDismiss: () -> Unit,
    idStore: (DataType, StashData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val selectedItems = remember { mutableStateListOf(*initialValues.toTypedArray()) }
    var showSearchFor by remember { mutableStateOf(false) }
    LaunchedEffect(showSearchFor) {
        if (!showSearchFor) {
            focusRequester.tryRequestFocus()
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(),
    ) {
        LazyColumn(
            modifier =
                modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        shape = RoundedCornerShape(16.dp),
                    ).focusGroup()
                    .focusRestorer()
                    .focusRequester(focusRequester),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            stickyHeader {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            items(selectedItems) { item ->
                ListItem(
                    modifier = Modifier,
                    selected = false,
                    enabled = true,
                    onClick = { selectedItems.remove(item) },
                    onLongClick = {},
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.stashapp_actions_remove),
                            tint = Color.Red,
                        )
                    },
                    headlineContent = {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    supportingContent = {},
                )
            }
            item {
                HorizontalDivider(Modifier.height(16.dp))
            }
            item {
                SimpleListItem(
                    title = stringResource(R.string.stashapp_actions_add),
                    subtitle = null,
                    showArrow = true,
                    onClick = { showSearchFor = true },
                    modifier = Modifier,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.stashapp_actions_add),
                        )
                    },
                )
            }
            item {
                SimpleListItem(
                    title = stringResource(R.string.stashapp_actions_submit),
                    subtitle = null,
                    showArrow = true,
                    onClick = {
                        onSave.invoke(selectedItems)
                        onDismiss.invoke()
                    },
                    modifier = Modifier,
                    leadingContent = {},
                )
            }
        }

        SearchForDialog(
            show = showSearchFor,
            dataType = dataType,
            onItemClick = {
                Log.v(TAG, "onItemClick ${it.id}")
                idStore.invoke(dataType, it)
                selectedItems.add(IdName(it.id, extractTitle(it) ?: it.id))
                showSearchFor = false
            },
            onDismissRequest = { showSearchFor = false },
            uiConfig = uiConfig,
            dialogTitle = stringResource(dataType.pluralStringId),
            dismissOnClick = false,
            showSuggestions = true,
            showRecent = true,
            allowCreate = false,
            startingSearchQuery = "",
        )
    }
}
