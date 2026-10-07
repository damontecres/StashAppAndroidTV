package com.github.damontecres.stashapp.ui.pages

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.di.server.MutationEngine
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.di.services.ServerLogger
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.RowColumn
import com.github.damontecres.stashapp.ui.components.SearchEditTextBox
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.OneTimeLaunchedEffect
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SearchViewModel(
    private val serverRepository: ServerRepository,
    private val serverLogger: ServerLogger,
    private val queryEngine: QueryEngine,
    private val mutationEngine: MutationEngine,
    val navigationManager: NavigationManager,
) : ViewModel() {
    private var currentQuery = ""

    private val _state = MutableStateFlow(SearchPageState())
    val state: StateFlow<SearchPageState> = _state

    fun init(
        initialQuery: String,
        perPage: Int,
    ) {
        search(initialQuery, perPage)
    }

    fun search(
        query: String,
        perPage: Int,
    ) {
        if (query.isNotBlank() && query != this.currentQuery) {
            this.currentQuery = query
            DataType.entries.forEach { dataType ->
                _state.update { state ->
                    state.results[dataType] = emptyList()
                    state
                }

                val stashFindFilter =
                    StashFindFilter(
                        q = query,
                        sortAndDirection =
                            SortAndDirection(
                                SortOption.sortByName(dataType),
                                SortDirectionEnum.ASC,
                            ),
                    )
                val findFilter =
                    stashFindFilter.toFindFilterType(
                        perPage = perPage,
                        page = 1,
                    )

                viewModelScope.launch(
                    LoggingCoroutineExceptionHandler(
                        serverRepository.currentServer.value,
                        viewModelScope,
                        toastMessage = "Search for ${
                            StashApplication.getApplication().getString(dataType.pluralStringId)
                        } failed",
                    ),
                ) {
                    val results = queryEngine.find(dataType, findFilter)
                    if (results.isNotEmpty()) {
                        _state.update { state ->
                            state.results[dataType] = results
                            state
                        }
                    }
                }
            }
        } else if (query != this.currentQuery) {
            _state.update { state ->
                DataType.entries.forEach { state.results[it] = emptyList() }
                state
            }
        }
    }
}

data class SearchPageState(
    val results: SnapshotStateMap<DataType, List<StashData>> =
        mutableStateMapOf<DataType, List<StashData>>()
            .apply { DataType.entries.forEach { put(it, emptyList()) } },
)

@Composable
fun SearchPage(
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
    viewModel: SearchViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var searchQuery by rememberSaveable { mutableStateOf(initialQuery) }
    val perPage = uiConfig.preferences.searchPreferences.maxResults
    val state by viewModel.state.collectAsState()

    OneTimeLaunchedEffect {
        viewModel.init(initialQuery, perPage)
//        focusRequester.tryRequestFocus()
    }

    LaunchedEffect(Unit) {
        focusRequester.tryRequestFocus()
    }

    val listState = rememberLazyListState()
    var focusedIndex by rememberSaveable { mutableStateOf(RowColumn(0, 0)) }
    var focusedRow by rememberSaveable { mutableIntStateOf(-1) }

    LazyColumn(
        state = listState,
        modifier =
            modifier
                .focusGroup()
                .focusRestorer(focusRequester),
        contentPadding = PaddingValues(16.dp),
    ) {
        stickyHeader {
            var job: Job? = null
            val searchDelay = uiConfig.preferences.searchPreferences.searchDelayMs
            SearchEditTextBox(
                modifier = Modifier.ifElse(focusedRow < 0, Modifier.focusRequester(focusRequester)),
                value = searchQuery,
                onValueChange = { newQuery ->
                    searchQuery = newQuery
                    job?.cancel()
                    job =
                        scope.launch(StashCoroutineExceptionHandler()) {
                            delay(searchDelay)
                            viewModel.search(searchQuery, perPage)
                        }
                },
                onSearchClick = {
                    job?.cancel()
                    viewModel.search(searchQuery, perPage)
                },
            )
        }

        DataType.entries.forEachIndexed { index, dataType ->
            val data = state.results[dataType].orEmpty()
            if (data.isNotEmpty()) {
                item {
                    HomePageRow(
                        uiConfig = uiConfig,
                        row =
                            FrontPageParser.FrontPageRow.Success(
                                name = stringResource(dataType.pluralStringId),
                                filter =
                                    FilterArgs(
                                        dataType = dataType,
                                        findFilter =
                                            StashFindFilter(
                                                q = searchQuery,
                                                sortAndDirection =
                                                    SortAndDirection(
                                                        SortOption.sortByName(dataType),
                                                        SortDirectionEnum.ASC,
                                                    ),
                                            ),
                                    ),
                                data = data,
                            ),
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        onFocus = { idx, item ->
                            focusedIndex = RowColumn(index, idx)
//                            focusedItem = item
                            focusedRow = index
                        },
                        rowFocusRequester = if (index == focusedIndex.row) focusRequester else null,
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}

@Composable
fun SearchItemsRow(
    title: String,
    items: List<Any>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    filterArgs: FilterArgs,
    modifier: Modifier = Modifier,
) {
    val firstFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    Column(
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .focusGroup()
                    .focusRestorer(firstFocus),
            state = listState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(items) { index, item ->
                StashCard(
                    modifier = Modifier.ifElse(index == 0, Modifier.focusRequester(firstFocus)),
                    uiConfig = uiConfig,
                    item = item,
                    itemOnClick = {
                        itemOnClick.onClick(
                            item,
                            FilterAndPosition(filterArgs, index),
                        )
                    },
                    longClicker = longClicker,
                    getFilterAndPosition = null,
                )
            }
        }
    }
}
