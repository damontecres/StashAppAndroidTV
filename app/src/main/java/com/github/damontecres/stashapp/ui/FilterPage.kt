package com.github.damontecres.stashapp.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.theme.Material3AppTheme
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "FilterPage"

sealed class ResolvedFilterState {
    data object Loading : ResolvedFilterState()

    data class Success(
        val filter: FilterArgs,
        val pagingSource: StashPagingSource<*, StashData, Any, *>,
    ) : ResolvedFilterState()

    data object Error : ResolvedFilterState()
}

@HiltViewModel
class FilterGridViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        stashServer: StashServer,
    ) : ViewModel() {
        private val queryEngine = QueryEngine(stashServer)
        private val filterParser = FilterParser(stashServer.version)
        private val dataSupplierFactory = DataSupplierFactory(stashServer.version)

        private val _resolvedFilterState = MutableLiveData<ResolvedFilterState>()
        val resolvedFilterState: LiveData<ResolvedFilterState> get() = _resolvedFilterState

        private val _savedFilters = mutableStateListOf<FilterArgs>()
        val savedFilters: SnapshotStateList<FilterArgs> get() = _savedFilters

        suspend fun fetchSavedFilters(dataType: DataType) {
            val filters =
                queryEngine.getSavedFilters(dataType).map { it.toFilterArgs(filterParser) }
            _savedFilters.addAll(filters)
        }

        fun updateFilter(filter: FilterArgs) {
            _resolvedFilterState.value = ResolvedFilterState.Loading

            val dataSupplier = dataSupplierFactory.create<Query.Data, StashData, Query.Data>(filter)

            _resolvedFilterState.value =
                ResolvedFilterState.Success(
                    filter,
                    StashPagingSource(
                        queryEngine,
                        25, // TODO
                        dataSupplier = dataSupplier,
                        useRandom = false,
                    ),
                )
        }
    }

@Suppress("ktlint:standard:function-naming")
@Composable
fun FilterGrid(
    startingFilter: FilterArgs,
    itemOnClick: (item: Any) -> Unit,
) {
    Log.v(TAG, "startingFilter=$startingFilter")
    val viewModel = hiltViewModel<FilterGridViewModel>()

    val resolvedFilterState by viewModel.resolvedFilterState.observeAsState(ResolvedFilterState.Loading)

    LaunchedEffect(Unit) {
        viewModel.updateFilter(startingFilter)
    }
    LaunchedEffect(Unit) {
        viewModel.fetchSavedFilters(startingFilter.dataType)
    }

    when (resolvedFilterState) {
        is ResolvedFilterState.Loading -> {
            Text(
                text = "Querying for filter...",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally),
            )
        }
        is ResolvedFilterState.Error -> {
            Text(
                text = "Error!!",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally),
            )
        }
        is ResolvedFilterState.Success -> {
            ResolvedFilterGrid(
                resolvedFilterState as ResolvedFilterState.Success,
                itemOnClick = itemOnClick,
            )
        }
    }
}

@OptIn(ExperimentalTvFoundationApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun ResolvedFilterGrid(
    resolvedFilter: ResolvedFilterState.Success,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    showHeader: Boolean = true,
    itemOnClick: (item: Any) -> Unit,
) {
//    val viewModel = hiltViewModel<FilterGridViewModel>()
    val pager =
        Pager(
            PagingConfig(
                pageSize = 25,
                prefetchDistance = 25 * 2,
                initialLoadSize = 25 * 2,
            ),
        ) {
            resolvedFilter.pagingSource
        }
    Log.v("ResolvedFilterGrid", "resolvedFilter.filter.name=${resolvedFilter.filter.name}")

    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
    val focusRequester = remember { FocusRequester() }

    TvLazyVerticalGrid(
        modifier =
            modifier
                .padding(16.dp)
                .fillMaxSize()
                .focusGroup()
                .focusRequester(focusRequester),
        columns = TvGridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showHeader) {
            item("header", span = { TvGridItemSpan(this.maxLineSpan) }) {
                Column { // TODO Box?
                    ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                        val filterName =
                            if (resolvedFilter.filter.name.isNotNullOrBlank()) {
                                resolvedFilter.filter.name
                            } else {
                                stringResource(id = resolvedFilter.filter.dataType.pluralStringId)
                            }
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = filterName,
                        )
                    }
                    SavedFilterDropDown()
                }
            }
        }
        if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
            item {
                Text(
                    text = "Waiting for items to load from the backend",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally),
                )
            }
        } else if (lazyPagingItems.itemCount == 0) {
            item {
                Text(
                    text = "No items found",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally),
                )
            }
        }

        items(lazyPagingItems.itemCount) { index ->
            val item = lazyPagingItems[index]
            if (item != null) {
                StashCard(item = item, itemOnClick)
            }
            // TODO is this inefficient?
            if (index == 0) {
                focusRequester.requestFocus()
            }
        }

        if (lazyPagingItems.loadState.append == LoadState.Loading) {
            item {
//                CircularProgressIndicator(
//                    modifier =
//                        Modifier.fillMaxWidth()
//                            .wrapContentWidth(Alignment.CenterHorizontally),
//                )
                Text(
                    text = "Waiting for items to load from the backend",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SavedFilterDropDown() {
    val viewModel = hiltViewModel<FilterGridViewModel>()
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.TopEnd),
    ) {
        Button(onClick = { expanded = !expanded }) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                Text("Filters")
            }
        }
        Material3AppTheme {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                viewModel.savedFilters.forEach { savedFilter ->
                    if (savedFilter.name != null) {
                        DropdownMenuItem(
                            text = { Text(savedFilter.name) },
                            onClick = {
                                expanded = false
                                viewModel.updateFilter(savedFilter)
                            },
                        )
                    }
                }
            }
        }
    }
}
