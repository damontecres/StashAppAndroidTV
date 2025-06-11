package com.github.damontecres.stashapp.ui.components.filter

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Icon
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.CreateFilterViewModel
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.filter.filterSummary
import com.github.damontecres.stashapp.filter.getFilterOptions
import com.github.damontecres.stashapp.ui.util.ifElse

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
