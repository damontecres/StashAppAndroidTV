package com.github.damontecres.stashapp.ui.components.filter

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.SortOption

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
