package com.github.damontecres.stashapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.SavedFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

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

    val context = LocalContext.current
    val server = LocalGlobalContext.current.server
    val scope = rememberCoroutineScope()
    LaunchedEffect(dataType) {
        scope.launch(
            LoggingCoroutineExceptionHandler(
                server,
                scope,
                toastMessage = "Failed to get saved filters",
            ),
        ) {
            savedFilters = QueryEngine(StashServer.requireCurrentServer()).getSavedFilters(dataType)
        }
    }

    val dialogItems = mutableListOf<DialogItemEntry>()
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
    dialogItems.add(DialogItem.divider())
    dialogItems.addAll(
        savedFilters.sortedBy { it.name }.map {
            DialogItem(it.name) {
                onFilterChange.invoke(it.toFilterArgs(filterParser))
            }
        },
    )

    Button(
        onClick = { showDialog = true },
        modifier = modifier,
    ) {
        Text(text = stringResource(R.string.stashapp_search_filter_saved_filters))
    }

    DialogPopup(
        showDialog = showDialog,
        title = stringResource(R.string.stashapp_search_filter_saved_filters),
        dialogItems = dialogItems,
        onDismissRequest = { showDialog = false },
        waitToLoad = false,
    )
}
