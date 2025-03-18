package com.github.damontecres.stashapp.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.SavedFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer

@Composable
fun SavedFiltersButton(
    dataType: DataType,
    /**
     * Called when user clicks a saved filter
     */
    onFilterChange: (FilterArgs) -> Unit,
    /**
     * Called when user us creating a filter from current
     */
    onFCreateFromFilter: () -> Unit,
    /**
     * Called when user us creating a new filter
     */
    onCreateFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    var savedFilters by remember { mutableStateOf(listOf<SavedFilter>()) }
    val filterParser = FilterParser(StashServer.requireCurrentServer().version)

    LaunchedEffect(dataType) {
        savedFilters = QueryEngine(StashServer.requireCurrentServer()).getSavedFilters(dataType)
    }

    val dialogItems = mutableListOf<DialogItem>()
    dialogItems.add(
        DialogItem("Create filter") {
            onCreateFilter.invoke()
        },
    )
    dialogItems.add(
        DialogItem("Create filter from current") {
            onFCreateFromFilter.invoke()
        },
    )
    dialogItems.add(
        DialogItem(headlineContent = {
            Spacer(Modifier.height(4.dp))
        }, onClick = {
        }),
    )
    dialogItems.addAll(
        savedFilters.sortedBy { it.name }.map {
            DialogItem(it.name) {
                onFilterChange.invoke(it.toFilterArgs(filterParser))
            }
        },
    )

    Button(
        onClick = { showDialog = true },
    ) {
        Text(text = stringResource(R.string.stashapp_search_filter_saved_filters))
    }

    DialogPopup(
        showDialog = showDialog,
        title = stringResource(R.string.stashapp_search_filter_saved_filters),
        items = dialogItems,
        onDismissRequest = { showDialog = false },
        waitToLoad = false,
    )

//    Box(modifier = modifier) {
//        Button(
//            onClick = { expanded = true },
//            onLongClick = {
//                // TODO?
//            },
//        ) {
//            Text(text = stringResource(R.string.stashapp_search_filter_saved_filters))
//        }
//        Material3MainTheme {
//            DropdownMenu(
//                expanded = expanded,
//                onDismissRequest = { expanded = false },
//            ) {
//                DropdownMenuItem(
//                    text = { Text("Create filter") },
//                    onClick = {
//                        expanded = false
//                        onUpdateFilter.invoke()
//                    },
//                )
//                DropdownMenuItem(
//                    text = { Text("Create filter from current") },
//                    onClick = {
//                        expanded = false
//                        onUpdateFilter.invoke()
//                    },
//                )
//                HorizontalDivider()
//                savedFilters.sortedBy { it.name }.forEach { savedFilter ->
//                    DropdownMenuItem(
//                        text = { Text(savedFilter.name) },
//                        onClick = {
//                            expanded = false
//                            onFilterChange(savedFilter.toFilterArgs(filterParser))
//                        },
//                    )
//                }
//            }
//        }
//    }
}
