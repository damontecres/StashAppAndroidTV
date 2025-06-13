package com.github.damontecres.stashapp.ui.components.filter

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.tv.material3.Icon
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CircumcisionCriterionInput
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.GenderCriterionInput
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationEnum
import com.github.damontecres.stashapp.api.type.ResolutionCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionEnum
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.CreateFilterViewModel
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.filter.displayName
import com.github.damontecres.stashapp.filter.filterSummary
import com.github.damontecres.stashapp.filter.getFilterOptions
import com.github.damontecres.stashapp.filter.resolutionName
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.views.circNameId
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

@Composable
fun ObjectFilterList(
    dataType: DataType,
    current: StashDataFilter,
    onObjectFilterClick: (FilterOption<StashDataFilter, Any>) -> Unit,
    idLookup: (DataType, List<String>) -> Map<String, CreateFilterViewModel.NameDescription?>,
    selectedFilterOption: FilterOption<StashDataFilter, Any>?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val filterOptions =
        remember { getFilterOptions(dataType).sortedBy { context.getString(it.nameStringId) } }
    LazyColumn(
        modifier =
            modifier
                .focusGroup()
                .focusRestorer(focusRequester),
    ) {
        itemsIndexed(
            filterOptions,
            key = { _, it -> it.nameStringId },
        ) { index, item ->
            item as FilterOption<StashDataFilter, Any>
            val value = item.getter.invoke(current).getOrNull()
            val subtitle =
                value?.let { _ ->
                    filterSummary(item.name, dataType, value, idLookup)
                }
            SimpleListItem(
                title = context.getString(item.nameStringId),
                subtitle = subtitle,
                showArrow = false,
                onClick = { onObjectFilterClick.invoke(item) },
                selected = item == selectedFilterOption,
                leadingContent = {
                    if (value != null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.stashapp_actions_enable),
                        )
                    }
                },
                modifier = Modifier.ifElse(index == 0, Modifier.focusRequester(focusRequester)),
            )
        }
    }
}

@Composable
fun ObjectFilterPicker(
    uiConfig: ComposeUiConfig,
    filterOption: FilterOption<StashDataFilter, Any>,
    initialValue: Any?,
    objectFilterChoiceFocusRequester: FocusRequester,
    saveObjectFilter: (Any?) -> Unit,
    onInputCriterionModifier: (InputCriterionModifier) -> Unit,
    onInputTextAction: (InputTextAction) -> Unit,
    onSelectFromListAction: (SelectFromListAction) -> Unit,
    onMultiCriterionInfo: (MultiCriterionInfo) -> Unit,
    onInputDateAction: (InputDateAction) -> Unit,
    onInputDurationAction: (InputDurationAction) -> Unit,
    mapIdToName: (id: String) -> String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var value by remember { mutableStateOf(initialValue) }

    fun onChangeCriterionModifier(change: (CriterionModifier) -> Any): () -> Unit =
        {
            onInputCriterionModifier.invoke(
                InputCriterionModifier(
                    filterName = context.getString(filterOption.nameStringId),
                    allowedModifiers = filterOption.allowedModifiers,
                    onClick = { value = change.invoke(it) },
                ),
            )
        }

    fun onChangeValue(
        currentValue: Any?,
        keyboardType: KeyboardType,
        change: (String) -> Any?,
    ): () -> Unit =
        {
            onInputTextAction.invoke(
                InputTextAction(
                    title = context.getString(filterOption.nameStringId),
                    value = currentValue?.toString(),
                    keyboardType = keyboardType,
                    onSubmit = { value = change.invoke(it) },
                ),
            )
        }

    if (filterOption.nameStringId == R.string.stashapp_rating) {
        LaunchedEffect(Unit) {
            if (initialValue == null) {
                value =
                    IntCriterionInput(
                        value = 0,
                        value2 = Optional.absent(),
                        modifier = CriterionModifier.EQUALS,
                    )
            }
        }
        value?.let { input ->
            LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
            input as IntCriterionInput
            val keyboardType =
                if (uiConfig.ratingAsStars) KeyboardType.Decimal else KeyboardType.Number
            val multiplier = if (uiConfig.ratingAsStars) 20.0 else 10.0
            CriterionInputPicker(
                modifier = modifier,
                name = stringResource(filterOption.nameStringId),
                value = SimpleRatingCriterionInput(input, uiConfig.ratingAsStars),
                removeEnabled = initialValue != null,
                onChangeCriterionModifier =
                    onChangeCriterionModifier { input.copy(modifier = it) },
                onChangeValue =
                    onChangeValue(
                        input.value / multiplier,
                        keyboardType,
                    ) {
                        it.toDoubleOrNull()?.let { input.copy(value = (it * multiplier).toInt()) }
                    },
                onChangeValue2 =
                    onChangeValue(
                        input.value2
                            .getOrNull()
                            ?.let { it / multiplier },
                        keyboardType,
                    ) {
                        input.copy(
                            value2 =
                                Optional.presentIfNotNull(
                                    (it.toDoubleOrNull()?.times(multiplier))?.toInt(),
                                ),
                        )
                    },
                onSave = { saveObjectFilter(value) },
                onRemove = { saveObjectFilter(null) },
            )
        }
    } else if (filterOption.nameStringId == R.string.stashapp_duration) {
        LaunchedEffect(Unit) {
            if (initialValue == null) {
                value =
                    IntCriterionInput(
                        value = 0,
                        value2 = Optional.absent(),
                        modifier = filterOption.allowedModifiers[0],
                    )
            }
        }
        value?.let { input ->
            LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
            input as IntCriterionInput
            CriterionInputPicker(
                modifier = modifier,
                name = stringResource(filterOption.nameStringId),
                value = SimpleDurationCriterionInput(input),
                removeEnabled = initialValue != null,
                onChangeCriterionModifier =
                    onChangeCriterionModifier { input.copy(modifier = it) },
                onChangeValue = {
                    onInputDurationAction.invoke(
                        InputDurationAction(
                            name = context.getString(filterOption.nameStringId),
                            value = input.value,
                            onSave = {
                                value = input.copy(value = it)
                            },
                        ),
                    )
                },
                onChangeValue2 = {
                    onInputDurationAction.invoke(
                        InputDurationAction(
                            name = context.getString(filterOption.nameStringId),
                            value = input.value2.getOrNull(),
                            onSave = {
                                value = input.copy(value = it)
                            },
                        ),
                    )
                },
                onSave = { saveObjectFilter(value) },
                onRemove = { saveObjectFilter(null) },
            )
        }
    } else {
        // TODO better titles?
        val includesTitle =
            filterOption.dataType?.let {
                context.getString(filterOption.dataType.pluralStringId) + " " +
                    context.getString(R.string.stashapp_criterion_modifier_includes)
            }
        val excludesTitle =
            filterOption.dataType?.let {
                context.getString(filterOption.dataType.pluralStringId) + " " +
                    context.getString(R.string.stashapp_criterion_modifier_excludes)
            }
        when (filterOption.type) {
            StringCriterionInput::class -> {
                LaunchedEffect(Unit) {
                    if (initialValue == null) {
                        value =
                            StringCriterionInput(
                                value = "",
                                modifier = CriterionModifier.EQUALS,
                            )
                    }
                }
                value?.let { input ->
                    LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
                    input as StringCriterionInput
                    CriterionInputPicker(
                        modifier = modifier,
                        name = stringResource(filterOption.nameStringId),
                        value = SimpleStringCriterionInput(input),
                        removeEnabled = initialValue != null,
                        onChangeCriterionModifier =
                            onChangeCriterionModifier {
                                input.copy(modifier = it)
                            },
                        onChangeValue =
                            onChangeValue(
                                input.value,
                                KeyboardType.Text,
                            ) {
                                input.copy(value = it)
                            },
                        onChangeValue2 = {},
                        onSave = { saveObjectFilter(value) },
                        onRemove = { saveObjectFilter(null) },
                    )
                }
            }

            IntCriterionInput::class -> {
                LaunchedEffect(Unit) {
                    if (initialValue == null) {
                        value =
                            IntCriterionInput(
                                value = 0,
                                value2 = Optional.absent(),
                                modifier = CriterionModifier.EQUALS,
                            )
                    }
                }
                value?.let { input ->
                    LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
                    input as IntCriterionInput
                    CriterionInputPicker(
                        modifier = modifier,
                        name = stringResource(filterOption.nameStringId),
                        value = SimpleIntCriterionInput(input),
                        removeEnabled = initialValue != null,
                        onChangeCriterionModifier =
                            onChangeCriterionModifier { input.copy(modifier = it) },
                        onChangeValue =
                            onChangeValue(
                                input.value,
                                KeyboardType.Number,
                            ) {
                                it.toIntOrNull()?.let { input.copy(value = it) }
                            },
                        onChangeValue2 =
                            onChangeValue(
                                input.value2.getOrNull(),
                                KeyboardType.Number,
                            ) {
                                input.copy(value2 = Optional.presentIfNotNull(it.toIntOrNull()))
                            },
                        onSave = { saveObjectFilter(value) },
                        onRemove = { saveObjectFilter(null) },
                    )
                }
            }

            FloatCriterionInput::class -> {
                LaunchedEffect(Unit) {
                    if (initialValue == null) {
                        value =
                            FloatCriterionInput(
                                value = 0.0,
                                value2 = Optional.absent(),
                                modifier = CriterionModifier.EQUALS,
                            )
                    }
                }
                value?.let { input ->
                    LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
                    input as FloatCriterionInput
                    CriterionInputPicker(
                        modifier = modifier,
                        name = stringResource(filterOption.nameStringId),
                        value = SimpleFloatCriterionInput(input),
                        removeEnabled = initialValue != null,
                        onChangeCriterionModifier =
                            onChangeCriterionModifier { input.copy(modifier = it) },
                        onChangeValue =
                            onChangeValue(
                                input.value,
                                KeyboardType.Decimal,
                            ) {
                                it.toDoubleOrNull()?.let { input.copy(value = it) }
                            },
                        onChangeValue2 =
                            onChangeValue(
                                input.value2.getOrNull(),
                                KeyboardType.Decimal,
                            ) {
                                input.copy(value2 = Optional.presentIfNotNull(it.toDoubleOrNull()))
                            },
                        onSave = { saveObjectFilter(value) },
                        onRemove = { saveObjectFilter(null) },
                    )
                }
            }

            Boolean::class -> {
                LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
                BooleanPicker(
                    name = stringResource(filterOption.nameStringId),
                    value = value as Boolean?,
                    onSave = { saveObjectFilter(it) },
                    onRemove = { saveObjectFilter(null) },
                    modifier = modifier,
                )
            }

            ResolutionCriterionInput::class -> {
                LaunchedEffect(Unit) {
                    if (initialValue == null) {
                        value =
                            ResolutionCriterionInput(
                                value = ResolutionEnum.FULL_HD,
                                modifier = filterOption.allowedModifiers[0],
                            )
                    }
                }
                value?.let { input ->
                    LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
                    input as ResolutionCriterionInput
                    SelectFromListPicker(
                        modifier = modifier,
                        name = stringResource(filterOption.nameStringId),
                        values = listOf(resolutionName(input.value)),
                        criterionModifier = input.modifier,
                        removeEnabled = initialValue != null,
                        onChangeCriterionModifier =
                            onChangeCriterionModifier { input.copy(modifier = it) },
                        onChangeValue = {
                            onSelectFromListAction.invoke(
                                SelectFromListAction(
                                    filterName = context.getString(filterOption.nameStringId),
                                    options =
                                        ResolutionEnum.entries
                                            .filter { it != ResolutionEnum.UNKNOWN__ }
                                            .map { resolutionName(it) },
                                    currentOptions = listOf(resolutionName(input.value)),
                                    multiSelect = false,
                                    onSubmit = {
                                        it.firstOrNull()?.let { idx ->
                                            value =
                                                input.copy(value = ResolutionEnum.entries[idx])
                                        }
                                    },
                                ),
                            )
                        },
                        onSave = { saveObjectFilter(value) },
                        onRemove = { saveObjectFilter(null) },
                    )
                }
            }

            CircumcisionCriterionInput::class -> {
                LaunchedEffect(Unit) {
                    if (initialValue == null) {
                        value =
                            CircumcisionCriterionInput(
                                value = Optional.absent(),
                                modifier = filterOption.allowedModifiers[0],
                            )
                    }
                }
                value?.let { input ->
                    LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
                    input as CircumcisionCriterionInput
                    SelectFromListPicker(
                        modifier = modifier,
                        name = stringResource(filterOption.nameStringId),
                        values =
                            (input.value.getOrNull() ?: listOf()).map {
                                context.getString(circNameId(it))
                            },
                        criterionModifier = input.modifier,
                        removeEnabled = initialValue != null,
                        onChangeCriterionModifier =
                            onChangeCriterionModifier { input.copy(modifier = it) },
                        onChangeValue = {
                            onSelectFromListAction.invoke(
                                SelectFromListAction(
                                    filterName = context.getString(filterOption.nameStringId),
                                    options =
                                        CircumisedEnum.entries
                                            .filter { it != CircumisedEnum.UNKNOWN__ }
                                            .map { context.getString(circNameId(it)) },
                                    currentOptions =
                                        (input.value.getOrNull() ?: listOf()).map {
                                            context.getString(circNameId(it))
                                        },
                                    multiSelect = true,
                                    onSubmit = {
                                        value =
                                            input.copy(value = Optional.present(it.map { CircumisedEnum.entries[it] }))
                                    },
                                ),
                            )
                        },
                        onSave = { saveObjectFilter(value) },
                        onRemove = { saveObjectFilter(null) },
                    )
                }
            }

            DateCriterionInput::class -> {
                val defaultDate =
                    remember {
                        if (filterOption.nameStringId == R.string.stashapp_birthdate) {
                            val cal = Calendar.getInstance()
                            cal.time = Date()
                            cal.add(Calendar.YEAR, -18)
                            cal.time
                        } else {
                            Date()
                        }
                    }
                LaunchedEffect(Unit) {
                    if (initialValue == null) {
                        value =
                            DateCriterionInput(
                                value = dateFormat.format(defaultDate),
                                value2 = Optional.absent(),
                                modifier = CriterionModifier.EQUALS,
                            )
                    }
                }
                value?.let { input ->
                    LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
                    input as DateCriterionInput
                    CriterionInputPicker(
                        modifier = modifier,
                        name = stringResource(filterOption.nameStringId),
                        value = SimpleDateCriterionInput(input),
                        removeEnabled = initialValue != null,
                        onChangeCriterionModifier =
                            onChangeCriterionModifier { input.copy(modifier = it) },
                        onChangeValue = {
                            onInputDateAction.invoke(
                                InputDateAction(
                                    name = context.getString(filterOption.nameStringId),
                                    value = dateFormat.parse(input.value)!!,
                                    onSave = {
                                        value = input.copy(value = dateFormat.format(it))
                                    },
                                ),
                            )
                        },
                        onChangeValue2 = {
                            onInputDateAction.invoke(
                                InputDateAction(
                                    name = context.getString(filterOption.nameStringId),
                                    value =
                                        input.value2.getOrNull()?.let { dateFormat.parse(it) }
                                            ?: defaultDate,
                                    onSave = {
                                        value =
                                            input.copy(
                                                value2 =
                                                    Optional.presentIfNotNull(
                                                        dateFormat.format(it),
                                                    ),
                                            )
                                    },
                                ),
                            )
                        },
                        onSave = { saveObjectFilter(value) },
                        onRemove = { saveObjectFilter(null) },
                    )
                }
            }

            GenderCriterionInput::class -> {
                LaunchedEffect(Unit) {
                    if (initialValue == null) {
                        value =
                            GenderCriterionInput(
                                value = Optional.absent(),
                                value_list = Optional.absent(),
                                modifier = filterOption.allowedModifiers[0],
                            )
                    }
                }
                value?.let { input ->
                    LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
                    input as GenderCriterionInput
                    SelectFromListPicker(
                        modifier = modifier,
                        name = stringResource(filterOption.nameStringId),
                        values =
                            (input.value_list.getOrNull() ?: listOf()).map {
                                displayName(context, it)
                            },
                        criterionModifier = input.modifier,
                        removeEnabled = initialValue != null,
                        onChangeCriterionModifier =
                            onChangeCriterionModifier { input.copy(modifier = it) },
                        onChangeValue = {
                            onSelectFromListAction.invoke(
                                SelectFromListAction(
                                    filterName = context.getString(filterOption.nameStringId),
                                    options =
                                        GenderEnum.entries
                                            .filter { it != GenderEnum.UNKNOWN__ }
                                            .map { displayName(context, it) },
                                    currentOptions =
                                        (input.value_list.getOrNull() ?: listOf()).map {
                                            displayName(context, it)
                                        },
                                    multiSelect = true,
                                    onSubmit = {
                                        value =
                                            input.copy(value_list = Optional.present(it.map { GenderEnum.entries[it] }))
                                    },
                                ),
                            )
                        },
                        onSave = { saveObjectFilter(value) },
                        onRemove = { saveObjectFilter(null) },
                    )
                }
            }

            MultiCriterionInput::class -> {
                LaunchedEffect(Unit) {
                    if (initialValue == null) {
                        value =
                            MultiCriterionInput(
                                value = Optional.absent(),
                                excludes = Optional.absent(),
                                modifier = filterOption.allowedModifiers[0],
                            )
                    }
                }
                value?.let { input ->
                    LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
                    input as MultiCriterionInput
                    MultiCriterionPicker(
                        name = stringResource(filterOption.nameStringId),
                        dataType = filterOption.dataType!!,
                        criterionModifier = input.modifier,
                        include = input.value.getOrNull()?.map(mapIdToName) ?: listOf(),
                        exclude = input.excludes.getOrNull()?.map(mapIdToName) ?: listOf(),
                        removeEnabled = initialValue != null,
                        includeSubValues = null,
                        onChangeCriterionModifier = onChangeCriterionModifier { input.copy(modifier = it) },
                        onPickInclude = {
                            onMultiCriterionInfo.invoke(
                                MultiCriterionInfo(
                                    name = includesTitle!!,
                                    dataType = filterOption.dataType,
                                    initialValues =
                                        input.value.getOrNull()?.map {
                                            IdName(it, mapIdToName.invoke(it))
                                        } ?: listOf(),
                                    onAdd = {
                                        val list =
                                            (input.value.getOrNull()?.map { it } ?: listOf())
                                                .toMutableList()
                                        list.add(it.id)
                                        value =
                                            input.copy(value = Optional.present(list))
                                    },
                                    onSave = {
                                        value =
                                            input.copy(value = Optional.present(it.map { it.id }))
                                    },
                                ),
                            )
                        },
                        onPickExclude = {
                            onMultiCriterionInfo.invoke(
                                MultiCriterionInfo(
                                    name = excludesTitle!!,
                                    dataType = filterOption.dataType,
                                    initialValues =
                                        input.excludes.getOrNull()?.map {
                                            IdName(it, mapIdToName.invoke(it))
                                        } ?: listOf(),
                                    onAdd = {},
                                    onSave = {
                                        value =
                                            input.copy(excludes = Optional.present(it.map { it.id }))
                                    },
                                ),
                            )
                        },
                        onIncludeSubValueClick = null,
                        onSave = { saveObjectFilter(value) },
                        onRemove = { saveObjectFilter(null) },
                        modifier = modifier,
                    )
                }
            }

            OrientationCriterionInput::class -> {
                LaunchedEffect(Unit) {
                    if (initialValue == null) {
                        value =
                            OrientationCriterionInput(
                                value = listOf(),
                            )
                    }
                }
                value?.let { input ->
                    LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
                    input as OrientationCriterionInput
                    SelectFromListPicker(
                        modifier = modifier,
                        name = stringResource(filterOption.nameStringId),
                        values = input.value.map { displayName(it) },
                        criterionModifier = null,
                        removeEnabled = initialValue != null,
                        onChangeCriterionModifier = {},
                        onChangeValue = {
                            onSelectFromListAction.invoke(
                                SelectFromListAction(
                                    filterName = context.getString(filterOption.nameStringId),
                                    options =
                                        OrientationEnum.entries
                                            .filter { it != OrientationEnum.UNKNOWN__ }
                                            .map { displayName(it) },
                                    currentOptions = input.value.map { displayName(it) },
                                    multiSelect = true,
                                    onSubmit = {
                                        value =
                                            input.copy(value = it.map { OrientationEnum.entries[it] })
                                    },
                                ),
                            )
                        },
                        onSave = { saveObjectFilter(value) },
                        onRemove = { saveObjectFilter(null) },
                    )
                }
            }

            HierarchicalMultiCriterionInput::class -> {
                LaunchedEffect(Unit) {
                    if (initialValue == null) {
                        value =
                            HierarchicalMultiCriterionInput(
                                value = Optional.absent(),
                                excludes = Optional.absent(),
                                depth = Optional.absent(),
                                modifier = filterOption.allowedModifiers[0],
                            )
                    }
                }
                value?.let { input ->
                    LaunchedEffect(Unit) { objectFilterChoiceFocusRequester.tryRequestFocus() }
                    input as HierarchicalMultiCriterionInput
                    MultiCriterionPicker(
                        name = stringResource(filterOption.nameStringId),
                        dataType = filterOption.dataType!!,
                        criterionModifier = input.modifier,
                        include = input.value.getOrNull()?.map(mapIdToName) ?: listOf(),
                        exclude = input.excludes.getOrNull()?.map(mapIdToName) ?: listOf(),
                        removeEnabled = initialValue != null,
                        includeSubValues = input.depth.getOrNull() == -1,
                        onChangeCriterionModifier = onChangeCriterionModifier { input.copy(modifier = it) },
                        onPickInclude = {
                            onMultiCriterionInfo.invoke(
                                MultiCriterionInfo(
                                    name = includesTitle!!,
                                    dataType = filterOption.dataType,
                                    initialValues =
                                        input.value.getOrNull()?.map {
                                            IdName(it, mapIdToName.invoke(it))
                                        } ?: listOf(),
                                    onAdd = {
                                        val list =
                                            (input.value.getOrNull()?.map { it } ?: listOf())
                                                .toMutableList()
                                        list.add(it.id)
                                        value =
                                            input.copy(value = Optional.present(list))
                                    },
                                    onSave = {
                                        value =
                                            input.copy(value = Optional.present(it.map { it.id }))
                                    },
                                ),
                            )
                        },
                        onPickExclude = {
                            onMultiCriterionInfo.invoke(
                                MultiCriterionInfo(
                                    name = excludesTitle!!,
                                    dataType = filterOption.dataType,
                                    initialValues =
                                        input.excludes.getOrNull()?.map {
                                            IdName(it, mapIdToName.invoke(it))
                                        } ?: listOf(),
                                    onAdd = {},
                                    onSave = {
                                        value =
                                            input.copy(excludes = Optional.present(it.map { it.id }))
                                    },
                                ),
                            )
                        },
                        onIncludeSubValueClick = {
                            if (input.depth.getOrNull() == -1) {
                                value = input.copy(depth = Optional.absent())
                            } else {
                                value = input.copy(depth = Optional.present(-1))
                            }
                        },
                        onSave = { saveObjectFilter(value) },
                        onRemove = { saveObjectFilter(null) },
                        modifier = modifier,
                    )
                }
            }
        }
    }
}
