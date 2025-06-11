package com.github.damontecres.stashapp.ui.components.filter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.data.flip
import com.github.damontecres.stashapp.filter.CreateFilterViewModel
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.filter.filterSummary
import com.github.damontecres.stashapp.filter.findFilterSummary
import com.github.damontecres.stashapp.filter.getFilterOptions
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.EditTextBox
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlin.reflect.full.createInstance

@Composable
fun CreateFilterScreen(
    dataType: DataType,
    initialFilter: FilterArgs?,
    modifier: Modifier = Modifier,
    viewModel: CreateFilterViewModel = viewModel(),
) {
    val ready by viewModel.ready.observeAsState(false)
    val name by viewModel.filterName.observeAsState()
    val findFilter by viewModel.findFilter.observeAsState(StashFindFilter(sortAndDirection = dataType.defaultSort))
    val objectFilter by viewModel.objectFilter.observeAsState(dataType.filterType.createInstance())
    val resultCount by viewModel.resultCount.observeAsState(-1)

    LaunchedEffect(initialFilter) {
        viewModel.initialize(dataType, initialFilter)
    }

    Column(modifier = modifier) {
        Text(
            text = "Create Filter",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (ready) {
            CreateFilterColumns(
                dataType = dataType,
                name = name,
                resultCount = resultCount,
                findFilter = findFilter,
                objectFilter = objectFilter,
                updateFilterName = {
                    viewModel.filterName.value = it
                },
                updateFindFilter = {
                    viewModel.findFilter.value = it
                    viewModel.updateCount()
                },
                updateObjectFilter = {
                    viewModel.objectFilter.value = it
                    viewModel.updateCount()
                },
                idLookup = viewModel::lookupIds,
                modifier =
                    Modifier
                        .fillMaxSize(),
            )
        } else {
            CircularProgress()
        }
    }
}

@Composable
fun CreateFilterColumns(
    dataType: DataType,
    name: String?,
    resultCount: Int,
    findFilter: StashFindFilter,
    objectFilter: StashDataFilter,
    updateFilterName: (String?) -> Unit,
    updateFindFilter: (StashFindFilter) -> Unit,
    updateObjectFilter: (StashDataFilter) -> Unit,
    idLookup: (DataType, List<String>) -> Map<String, CreateFilterViewModel.NameDescription?>,
    modifier: Modifier = Modifier,
) {
    val findFilterInteractionSource = remember { MutableInteractionSource() }
    val objectFilterInteractionSource = remember { MutableInteractionSource() }

    val findFilterItemFocused = findFilterInteractionSource.collectIsFocusedAsState().value
    val objectFilterItemFocused = objectFilterInteractionSource.collectIsFocusedAsState().value

    var findFilterFocused by remember { mutableStateOf(false) }
    var objectFilterFocused by remember { mutableStateOf(false) }
    var sortByFocused by remember { mutableStateOf(false) }
    val findFilterFocusRequester = remember { FocusRequester() }
    val sortByFocusRequester = remember { FocusRequester() }
    val objectFilterFocusRequester = remember { FocusRequester() }
    val objectFilterChoiceFocusRequester = remember { FocusRequester() }
    val objectFilterCount =
        remember(objectFilter) {
            getFilterOptions(dataType)
                .filter {
                    (it as FilterOption<StashDataFilter, Any>)
                        .getter
                        .invoke(
                            objectFilter,
                        ).getOrNull() != null
                }.count()
        }

    var inputTextAction by remember { mutableStateOf<InputTextAction?>(null) }
    var inputCriterionModifier by remember { mutableStateOf<InputCriterionModifier?>(null) }
    var selectedFilterOption by remember { mutableStateOf<FilterOption<StashDataFilter, Any>?>(null) }

    val focusRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val listWidth = 280.dp

    LazyRow(
        modifier =
            modifier
                .ifElse(
                    inputTextAction != null || inputCriterionModifier != null,
                    Modifier.blur(10.dp),
                ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            LaunchedEffect(Unit) {
                focusRequester.tryRequestFocus()
            }
            BasicFilterSettings(
                dataType = dataType,
                name = name,
                resultCount = resultCount,
                findFilter = findFilter,
                objectFilter = objectFilter,
                objectFilterCount = objectFilterCount,
                onFilterNameClick = {
                    inputTextAction =
                        InputTextAction(
                            title = context.getString(R.string.stashapp_filter_name),
                            value = name,
                            keyboardType = KeyboardType.Text,
                            onSubmit = updateFilterName,
                        )
                },
                findFilterOnClick = { findFilterFocused = true },
                objectFilterOnClick = { objectFilterFocused = true },
                findFilterInteractionSource = findFilterInteractionSource,
                objectFilterInteractionSource = objectFilterInteractionSource,
                modifier =
                    Modifier
                        .width(listWidth)
                        .animateItem()
                        .focusRequester(focusRequester)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp),
                        ).onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyUp && it.key == Key.DirectionRight) {
                                if (findFilterItemFocused) {
                                    findFilterFocused = true
                                } else if (objectFilterItemFocused) {
                                    objectFilterFocused = true
                                }
                                true
                            } else {
                                false
                            }
                        },
            )
        }
        if (findFilterFocused) {
            item {
                LaunchedEffect(Unit) {
                    findFilterFocusRequester.tryRequestFocus()
                }
                BackHandler {
                    focusRequester.tryRequestFocus()
                    findFilterFocused = false
                }
                FindFilterSettings(
                    dataType = dataType,
                    sortAndDirection = findFilter.sortAndDirection ?: dataType.defaultSort,
                    query = findFilter.q,
                    onSortByClick = { sortByFocused = true },
                    onDirectionClick = {
                        val current = findFilter.sortAndDirection ?: dataType.defaultSort
                        updateFindFilter.invoke(
                            findFilter.withDirection(
                                current.direction.flip(),
                                dataType,
                            ),
                        )
                    },
                    onQueryClick = {
                        inputTextAction =
                            InputTextAction(
                                title = context.getString(R.string.stashapp_component_tagger_noun_query),
                                value = findFilter.q,
                                keyboardType = KeyboardType.Text,
                                onSubmit = {
                                    updateFindFilter.invoke(findFilter.copy(q = it))
                                },
                            )
                    },
                    modifier =
                        Modifier
                            .width(listWidth)
                            .animateItem()
                            .focusRequester(findFilterFocusRequester)
                            .focusProperties {
                                onExit = {
                                    if (!sortByFocused) {
                                        findFilterFocused = false
                                    }
                                }
                            }.background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp),
                            ),
                )
            }
            if (sortByFocused) {
                item {
                    LaunchedEffect(Unit) {
                        sortByFocusRequester.tryRequestFocus()
                    }
                    BackHandler {
                        findFilterFocusRequester.tryRequestFocus()
                        sortByFocused = false
                    }
                    SortByList(
                        dataType = dataType,
                        currentSort = (findFilter.sortAndDirection ?: dataType.defaultSort).sort,
                        onSortByClick = {
                            updateFindFilter.invoke(findFilter.withSort(it.key))
                            sortByFocused = false
                            findFilterFocusRequester.tryRequestFocus()
                        },
                        modifier =
                            Modifier
                                .width(listWidth)
                                .animateItem()
                                .focusRequester(sortByFocusRequester)
                                .focusProperties {
                                    onExit = {
                                        sortByFocused = false
                                    }
                                }.background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(16.dp),
                                ),
                    )
                }
            }
        }
        if (objectFilterFocused) {
            item {
                LaunchedEffect(Unit) {
                    objectFilterFocusRequester.tryRequestFocus()
                }
                BackHandler {
                    focusRequester.tryRequestFocus()
                    objectFilterFocused = false
                }
                ObjectFilterList(
                    dataType = dataType,
                    current = objectFilter,
                    onObjectFilterClick = { selectedFilterOption = it },
                    idLookup = idLookup,
                    selectedFilterOption = selectedFilterOption,
                    modifier =
                        Modifier
                            .width(listWidth)
                            .animateItem()
                            .focusRequester(objectFilterFocusRequester)
                            .focusProperties {
                                onExit = {
                                    if (selectedFilterOption == null) {
                                        objectFilterFocused = false
                                    }
                                }
                            }.background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp),
                            ),
                )
            }
            selectedFilterOption?.let { filterOption ->
                item {
                    val initialValue =
                        remember { filterOption.getter.invoke(objectFilter).getOrNull() }
                    var value by remember { mutableStateOf(initialValue) }
                    BackHandler {
                        objectFilterFocusRequester.tryRequestFocus()
                        selectedFilterOption = null
                    }

                    val saveObjectFilter = { newValue: Any? ->
                        val newObjectFilter =
                            filterOption.setter.invoke(
                                objectFilter,
                                Optional.presentIfNotNull(newValue),
                            )
                        updateObjectFilter(newObjectFilter)
                        objectFilterFocusRequester.tryRequestFocus()
                        selectedFilterOption = null
                    }

                    if (filterOption.nameStringId == R.string.stashapp_rating) {
                        // TODO
                    } else if (filterOption.nameStringId == R.string.stashapp_duration) {
                        // TODO
                    } else {
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
                                    StringPicker(
                                        modifier =
                                            Modifier
                                                .width(listWidth)
                                                .animateItem()
                                                .focusRequester(objectFilterChoiceFocusRequester)
                                                .focusProperties {
                                                    onExit = {
                                                        selectedFilterOption = null
                                                    }
                                                }.background(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(16.dp),
                                                ),
                                        name = stringResource(filterOption.nameStringId),
                                        value = input,
                                        removeEnabled = initialValue != null,
                                        onChangeCriterionModifier = {
                                            inputCriterionModifier =
                                                InputCriterionModifier(
                                                    filterName = context.getString(filterOption.nameStringId),
                                                    allowedModifiers =
                                                        filterOption.allowedModifiers
                                                            ?: listOf(
                                                                CriterionModifier.EQUALS,
                                                                CriterionModifier.NOT_EQUALS,
                                                                CriterionModifier.INCLUDES,
                                                                CriterionModifier.EXCLUDES,
                                                                CriterionModifier.IS_NULL,
                                                                CriterionModifier.NOT_NULL,
                                                                CriterionModifier.MATCHES_REGEX,
                                                                CriterionModifier.NOT_MATCHES_REGEX,
                                                            ),
                                                    onClick = {
                                                        value = input.copy(modifier = it)
                                                    },
                                                )
                                        },
                                        onChangeValue = {
                                            inputTextAction =
                                                InputTextAction(
                                                    title = context.getString(filterOption.nameStringId),
                                                    value = input.value,
                                                    keyboardType = KeyboardType.Text,
                                                    onSubmit = {
                                                        value = input.copy(value = it)
                                                    },
                                                )
                                        },
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
                                    IntPicker(
                                        modifier =
                                            Modifier
                                                .width(listWidth)
                                                .animateItem()
                                                .focusRequester(objectFilterChoiceFocusRequester)
                                                .focusProperties {
                                                    onExit = {
                                                        selectedFilterOption = null
                                                    }
                                                }.background(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(16.dp),
                                                ),
                                        name = stringResource(filterOption.nameStringId),
                                        value = input,
                                        removeEnabled = initialValue != null,
                                        onChangeCriterionModifier = {
                                            inputCriterionModifier =
                                                InputCriterionModifier(
                                                    filterName = context.getString(filterOption.nameStringId),
                                                    allowedModifiers =
                                                        filterOption.allowedModifiers
                                                            ?: listOf(
                                                                CriterionModifier.EQUALS,
                                                                CriterionModifier.NOT_EQUALS,
                                                                CriterionModifier.BETWEEN,
                                                                CriterionModifier.NOT_BETWEEN,
                                                                CriterionModifier.GREATER_THAN,
                                                                CriterionModifier.LESS_THAN,
                                                                CriterionModifier.IS_NULL,
                                                                CriterionModifier.NOT_NULL,
                                                            ),
                                                    onClick = {
                                                        value = input.copy(modifier = it)
                                                    },
                                                )
                                        },
                                        onChangeValue = { isValue2 ->
                                            inputTextAction =
                                                InputTextAction(
                                                    title = context.getString(filterOption.nameStringId),
                                                    value =
                                                        (
                                                            if (isValue2) {
                                                                input.value2.getOrNull()
                                                                    ?: 0
                                                            } else {
                                                                input.value
                                                            }
                                                        ).toString(),
                                                    keyboardType = KeyboardType.Number,
                                                    onSubmit = {
                                                        val newInt = it.toIntOrNull()
                                                        if (newInt != null) {
                                                            value =
                                                                if (isValue2) {
                                                                    input.copy(
                                                                        value2 =
                                                                            Optional.present(
                                                                                newInt,
                                                                            ),
                                                                    )
                                                                } else {
                                                                    input.copy(value = newInt)
                                                                }
                                                        }
                                                    },
                                                )
                                        },
                                        onSave = { saveObjectFilter(value) },
                                        onRemove = { saveObjectFilter(null) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    inputTextAction?.let {
        InputTextDialog(
            onDismissRequest = { inputTextAction = null },
            action = it,
        )
    }
    inputCriterionModifier?.let {
        CriterionModifierPickerDialog(
            filterName = it.filterName,
            allowedModifiers = it.allowedModifiers,
            onClick = it.onClick,
            onDismiss = { inputCriterionModifier = null },
        )
    }
}

data class InputTextAction(
    val title: String,
    val value: String?,
    val keyboardType: KeyboardType,
    val onSubmit: (String) -> Unit,
)

data class InputCriterionModifier(
    val filterName: String,
    val allowedModifiers: List<CriterionModifier>,
    val onClick: (CriterionModifier) -> Unit,
)

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

@Composable
fun BasicFilterSettings(
    dataType: DataType,
    name: String?,
    findFilter: StashFindFilter,
    objectFilter: StashDataFilter,
    objectFilterCount: Int,
    resultCount: Int,
    onFilterNameClick: () -> Unit,
    findFilterOnClick: () -> Unit,
    objectFilterOnClick: () -> Unit,
    findFilterInteractionSource: MutableInteractionSource,
    objectFilterInteractionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    LazyColumn(
        modifier =
            modifier
                .focusGroup()
                .focusRestorer(focusRequester),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            SimpleListItem(
                title = stringResource(R.string.stashapp_filter_name),
                subtitle = name,
                showArrow = false,
                onClick = onFilterNameClick,
                modifier = Modifier.focusRequester(focusRequester),
            )
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.sort_by),
                subtitle = findFilterSummary(context, dataType, findFilter),
                showArrow = true,
                onClick = findFilterOnClick,
                interactionSource = findFilterInteractionSource,
            )
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.stashapp_filters),
                subtitle =
                    when (objectFilterCount) {
                        // TODO i18n for plurals
                        1 -> "1 " + stringResource(R.string.stashapp_filter)
                        in 2..Int.MAX_VALUE -> objectFilterCount.toString() + " " + stringResource(R.string.stashapp_filters)
                        else -> null
                    },
                showArrow = true,
                onClick = objectFilterOnClick,
                interactionSource = objectFilterInteractionSource,
            )
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.submit_without_saving),
                subtitle =
                    if (resultCount >= 0) {
                        resultCount.toString() + " " + stringResource(R.string.results)
                    } else {
                        null
                    },
                showArrow = false,
                onClick = {},
            )
        }
        item {
            SimpleListItem(
                enabled = name.isNotNullOrBlank(),
                title = stringResource(R.string.save_and_submit),
                subtitle =
                    if (name.isNotNullOrBlank()) {
                        null
                    } else {
                        stringResource(R.string.save_and_submit_no_name_desc)
                    },
                showArrow = false,
                onClick = {},
            )
        }
    }
}

@Composable
fun FindFilterSettings(
    dataType: DataType,
    sortAndDirection: SortAndDirection,
    query: String?,
    onSortByClick: () -> Unit,
    onDirectionClick: () -> Unit,
    onQueryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    LazyColumn(
        modifier =
            modifier
                .focusGroup()
                .focusRestorer(focusRequester),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            SimpleListItem(
                title = stringResource(R.string.sort_by),
                subtitle = sortAndDirection.sort.getString(context),
                showArrow = true,
                onClick = onSortByClick,
                modifier = Modifier.focusRequester(focusRequester),
            )
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.stashapp_config_ui_image_wall_direction),
                subtitle =
                    if (sortAndDirection.direction == SortDirectionEnum.ASC) {
                        stringResource(R.string.stashapp_ascending)
                    } else {
                        stringResource(R.string.stashapp_descending)
                    },
                showArrow = true,
                onClick = onDirectionClick,
            )
        }
        item {
            SimpleListItem(
                title = stringResource(R.string.stashapp_component_tagger_noun_query),
                subtitle = query,
                showArrow = true,
                onClick = onQueryClick,
            )
        }
    }
}

@Composable
fun SortByList(
    dataType: DataType,
    currentSort: SortOption,
    onSortByClick: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier,
    ) {
        items(dataType.sortOptions, key = { it.key }) {
            SimpleListItem(
                title = it.getString(context),
                subtitle = null,
                showArrow = false,
                onClick = { onSortByClick.invoke(it) },
                selected = it == currentSort,
            )
        }
    }
}

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
    LazyColumn(
        modifier =
            modifier
                .focusGroup()
                .focusRestorer(focusRequester),
    ) {
        itemsIndexed(
            getFilterOptions(dataType),
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
fun ObjectFilterChooser(
    objectFilter: StashDataFilter,
    filterOption: FilterOption<StashDataFilter, *>,
    onSave: (StashDataFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (filterOption.nameStringId == R.string.stashapp_rating) {
        // TODO
    } else if (filterOption.nameStringId == R.string.stashapp_duration) {
        // TODO
    } else {
        when (filterOption.type) {
            StringCriterionInput::class -> {
                filterOption as FilterOption<StashDataFilter, StringCriterionInput>
                val value = filterOption.getter.invoke(objectFilter).getOrNull()

                StringPicker(
                    modifier = modifier,
                    name = stringResource(filterOption.nameStringId),
                    value =
                        value ?: StringCriterionInput(
                            value = "",
                            modifier = CriterionModifier.EQUALS,
                        ),
                    removeEnabled = value != null,
                    onChangeCriterionModifier = {},
                    onChangeValue = {},
                    onSave = {},
                    onRemove = {},
                )
            }
        }
    }
}

@Composable
fun ObjectFilterChooser(
    value: Any,
    removeEnabled: Boolean,
    filterOption: FilterOption<StashDataFilter, Any>,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (filterOption.nameStringId == R.string.stashapp_rating) {
        // TODO
    } else if (filterOption.nameStringId == R.string.stashapp_duration) {
        // TODO
    } else {
        when (filterOption.type) {
            StringCriterionInput::class -> {
                StringPicker(
                    modifier = modifier,
                    name = stringResource(filterOption.nameStringId),
                    value = value as StringCriterionInput,
                    removeEnabled = removeEnabled,
                    onChangeCriterionModifier = {},
                    onChangeValue = {},
                    onSave = {},
                    onRemove = {},
                )
            }
        }
    }
}

@Composable
fun SimpleListItem(
    title: String,
    subtitle: String?,
    showArrow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    ListItem(
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        onLongClick = {},
        leadingContent = leadingContent,
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        supportingContent = {
            if (subtitle.isNotNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        trailingContent = {
            if (showArrow) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                )
            }
        },
        interactionSource = interactionSource,
    )
}
