package com.github.damontecres.stashapp.ui.components.filter

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
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
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.filter.findFilterSummary
import com.github.damontecres.stashapp.util.isNotNullOrBlank

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
