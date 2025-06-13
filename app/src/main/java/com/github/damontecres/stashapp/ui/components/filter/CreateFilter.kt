package com.github.damontecres.stashapp.ui.components.filter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.data.flip
import com.github.damontecres.stashapp.filter.CreateFilterViewModel
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.filter.findFilterSummary
import com.github.damontecres.stashapp.filter.getFilterOptions
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.reflect.full.createInstance

internal const val TAG = "CreateFilter"

@Composable
fun CreateFilterScreen(
    uiConfig: ComposeUiConfig,
    dataType: DataType,
    initialFilter: FilterArgs?,
    navigationManager: NavigationManager,
    modifier: Modifier = Modifier,
    viewModel: CreateFilterViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()

    val ready by viewModel.ready.observeAsState(false)
    val name by viewModel.filterName.observeAsState()
    val findFilter by viewModel.findFilter.observeAsState(StashFindFilter(sortAndDirection = dataType.defaultSort))
    val objectFilter by viewModel.objectFilter.observeAsState(dataType.filterType.createInstance())
    val resultCount by viewModel.resultCount.observeAsState(-1)

    LaunchedEffect(initialFilter) {
        viewModel.initialize(dataType, initialFilter)
        viewModel.updateCount()
    }

    Column(modifier = modifier) {
        Text(
            text = "Create ${stringResource(dataType.stringId)} Filter",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        )
        if (ready) {
            CreateFilterColumns(
                uiConfig = uiConfig,
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
                idStore = viewModel::store,
                onSubmit = { save ->
                    if (save) {
                        scope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                            viewModel.saveFilter()
                            navigationManager.goBack()
                            navigationManager.navigate(Destination.Filter(viewModel.createFilterArgs()))
                        }
                    } else {
                        navigationManager.goBack()
                        navigationManager.navigate(Destination.Filter(viewModel.createFilterArgs()))
                    }
                },
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
    uiConfig: ComposeUiConfig,
    dataType: DataType,
    name: String?,
    resultCount: Int,
    findFilter: StashFindFilter,
    objectFilter: StashDataFilter,
    updateFilterName: (String?) -> Unit,
    updateFindFilter: (StashFindFilter) -> Unit,
    updateObjectFilter: (StashDataFilter) -> Unit,
    idLookup: (DataType, List<String>) -> Map<String, CreateFilterViewModel.NameDescription?>,
    idStore: (DataType, StashData) -> Unit,
    onSubmit: (Boolean) -> Unit,
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
            getFilterOptions(dataType).count {
                (it as FilterOption<StashDataFilter, Any>)
                    .getter
                    .invoke(objectFilter)
                    .getOrNull() != null
            }
        }

    var inputTextAction by remember { mutableStateOf<InputTextAction?>(null) }
    var inputCriterionModifier by remember { mutableStateOf<InputCriterionModifier?>(null) }
    var selectFromListAction by remember { mutableStateOf<SelectFromListAction?>(null) }
    var multiCriterionInfo by remember { mutableStateOf<MultiCriterionInfo?>(null) }
    var inputDateAction by remember { mutableStateOf<InputDateAction?>(null) }
    var inputDurationAction by remember { mutableStateOf<InputDurationAction?>(null) }

    var selectedFilterOption by remember { mutableStateOf<FilterOption<StashDataFilter, Any>?>(null) }

    val focusRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val listWidth = 280.dp

    val shouldBlur =
        inputTextAction != null ||
            inputCriterionModifier != null ||
            selectFromListAction != null ||
            multiCriterionInfo != null ||
            inputDateAction != null ||
            inputDurationAction != null

    LazyRow(
        modifier =
            modifier
                .ifElse(
                    shouldBlur,
                    Modifier
                        .blur(10.dp)
                        .graphicsLayer {
                            alpha = .25f
                        },
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
                onSubmit = onSubmit,
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

                    ObjectFilterPicker(
                        uiConfig = uiConfig,
                        filterOption = filterOption,
                        initialValue = initialValue,
                        objectFilterChoiceFocusRequester = objectFilterChoiceFocusRequester,
                        saveObjectFilter = saveObjectFilter,
                        onInputCriterionModifier = { inputCriterionModifier = it },
                        onInputTextAction = { inputTextAction = it },
                        onSelectFromListAction = { selectFromListAction = it },
                        onMultiCriterionInfo = { multiCriterionInfo = it },
                        onInputDateAction = { inputDateAction = it },
                        onInputDurationAction = { inputDurationAction = it },
                        mapIdToName = {
                            // TODO?
                            idLookup.invoke(filterOption.dataType!!, listOf(it))[it]?.name ?: it
                        },
                        modifier =
                            Modifier
                                .width(listWidth)
                                .animateItem()
                                .focusRequester(objectFilterChoiceFocusRequester)
                                .focusProperties {
                                    onExit = {
                                        // TODO if there are changes, might be good to block this so the user doesn't lose changes accidentally
                                        selectedFilterOption = null
                                    }
                                }.background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(16.dp),
                                ),
                    )
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
    selectFromListAction?.let { action ->
        SelectFromListDialog(
            action = action,
            onSubmit = {
                action.onSubmit.invoke(it)
                selectFromListAction = null
            },
            onDismiss = { selectFromListAction = null },
        )
    }
    multiCriterionInfo?.let { info ->
        MultiCriterionPickerDialog(
            uiConfig = uiConfig,
            dataType = info.dataType,
            name = info.name,
            initialValues = info.initialValues,
            onSave = info.onSave,
            onDismiss = { multiCriterionInfo = null },
            idStore = idStore,
        )
    }
    inputDateAction?.let {
        DatePickerDialog(
            name = it.name,
            value = it.value,
            onSave = it.onSave,
            onDismiss = { inputDateAction = null },
        )
    }
    inputDurationAction?.let {
        DurationPickerDialog(
            name = it.name,
            value = it.value,
            onSave = it.onSave,
            onDismiss = { inputDurationAction = null },
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

data class SelectFromListAction(
    val filterName: String,
    val options: List<String>,
    val currentOptions: List<String>,
    val multiSelect: Boolean,
    val onSubmit: (indices: List<Int>) -> Unit,
)

data class MultiCriterionInfo(
    val name: String,
    val dataType: DataType,
    val initialValues: List<IdName>,
    val onAdd: (IdName) -> Unit,
    val onSave: (List<IdName>) -> Unit,
)

data class InputDateAction(
    val name: String,
    val value: Date,
    val onSave: (Date) -> Unit,
)

data class InputDurationAction(
    val name: String,
    val value: Int?,
    val onSave: (Int) -> Unit,
)

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
    onSubmit: (Boolean) -> Unit,
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
                onClick = { onSubmit.invoke(false) },
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
                onClick = { onSubmit.invoke(true) },
            )
        }
    }
}
