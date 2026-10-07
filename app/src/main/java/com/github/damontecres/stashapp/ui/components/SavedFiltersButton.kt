package com.github.damontecres.stashapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.SavedFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SavedFiltersButtonViewModel(
    val serverRepository: ServerRepository,
    val queryEngine: QueryEngine,
) : ViewModel()

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
    viewModel: SavedFiltersButtonViewModel = koinViewModel(),
) {
    val server by viewModel.serverRepository.currentServer.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var savedFilters by remember { mutableStateOf(listOf<SavedFilter>()) }

    val filterParser = remember(server) { FilterParser(server.serverPreferences.version) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(dataType) {
        scope.launch(
            LoggingCoroutineExceptionHandler(
                server,
                scope,
                toastMessage = "Failed to get saved filters",
            ),
        ) {
            savedFilters = viewModel.queryEngine.getSavedFilters(dataType)
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
