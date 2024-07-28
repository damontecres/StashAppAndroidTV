package com.github.damontecres.stashapp.ui

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
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
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.AppFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashDefaultFilter
import com.github.damontecres.stashapp.data.StashFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.suppliers.GalleryDataSupplier
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.MarkerDataSupplier
import com.github.damontecres.stashapp.suppliers.MovieDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.suppliers.StudioDataSupplier
import com.github.damontecres.stashapp.suppliers.TagDataSupplier
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.theme.Material3AppTheme
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.parseSortDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val TAG = "FilterPage"

@Parcelize
data class ResolvedFindFilter(
    val q: String?,
    val sort: String?,
    val direction: SortDirectionEnum?,
) : Parcelable {
    val asFindFilterType: FindFilterType
        get() =
            FindFilterType(
                q = Optional.presentIfNotNull(q),
                sort = Optional.presentIfNotNull(sort),
                direction = Optional.presentIfNotNull(direction),
            )
}

data class ResolvedFilter(
    val dataType: DataType,
    val name: String? = null,
    val findFilter: ResolvedFindFilter? = null,
    val objectFilter: Any? = null,
)

fun SavedFilterData.resolve(dataType: DataType): ResolvedFilter =
    ResolvedFilter(
        dataType = DataType.fromFilterMode(mode) ?: dataType,
        name = name,
        // TODO need to override sort or direction?
        findFilter =
            ResolvedFindFilter(
                find_filter?.q,
                find_filter?.sort,
                find_filter?.direction,
            ),
        objectFilter = object_filter,
    )

sealed class ResolvedFilterState {
    data object Loading : ResolvedFilterState()

    data class Success(val filter: ResolvedFilter, val pagingSource: StashPagingSource<*, Any, *>) : ResolvedFilterState()

    data object Error : ResolvedFilterState()
}

@HiltViewModel
class FilterGridViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val queryEngine = QueryEngine(context)
        private val filterParser = FilterParser(ServerPreferences(context).serverVersion)

        private val _resolvedFilterState = MutableLiveData<ResolvedFilterState>()
        val resolvedFilterState: LiveData<ResolvedFilterState> get() = _resolvedFilterState

        private val _savedFilters = mutableStateListOf<ResolvedFilter>()
        val savedFilters: SnapshotStateList<ResolvedFilter> get() = _savedFilters

        suspend fun fetchSavedFilters(dataType: DataType) {
            val filters = queryEngine.getSavedFilters(dataType).map { it.resolve(dataType) }
            _savedFilters.addAll(filters)
        }

        suspend fun updateFilter(filter: StashFilter) {
            _resolvedFilterState.value = ResolvedFilterState.Loading
            val resolvedFilter =
                when (filter) {
                    is AppFilter -> {
                        TODO()
                    }

                    is StashDefaultFilter -> {
                        val savedFilter = queryEngine.getDefaultFilter(filter.dataType)
                        if (savedFilter != null) {
                            savedFilter.resolve(filter.dataType)
                        } else {
                            Log.v(TAG, "No default filter for ${filter.dataType}")
                            val sortAndDirection = filter.dataType.defaultSort
                            ResolvedFilter(
                                dataType = filter.dataType,
                                name = context.getString(filter.dataType.pluralStringId),
                                findFilter =
                                    ResolvedFindFilter(
                                        null,
                                        sortAndDirection.sort,
                                        sortAndDirection.direction,
                                    ),
                                objectFilter = null,
                            )
                        }
                    }

                    is StashCustomFilter -> {
                        ResolvedFilter(
                            dataType = filter.dataType,
                            name = filter.description,
                            findFilter = ResolvedFindFilter(filter.query, filter.sortBy, parseSortDirection(filter.direction)),
                            objectFilter = null,
                        )
                    }
                    is StashSavedFilter -> {
                        val savedFilter =
                            queryEngine.getSavedFilter(filter.savedFilterId)
                        if (savedFilter != null) {
                            savedFilter.resolve(filter.dataType)
                        } else {
                            Log.v(TAG, "No saved filter for id=${filter.savedFilterId}")
                            _resolvedFilterState.value = ResolvedFilterState.Error
                            null
                        }
                    }
                    else -> throw IllegalStateException("Unsupported StashFilter type: $filter")
                }

            if (resolvedFilter != null) {
                updateFilter(resolvedFilter)
            }
        }

        fun updateFilter(resolvedFilter: ResolvedFilter) {
            _resolvedFilterState.value = ResolvedFilterState.Loading
            val dataSupplier =
                when (resolvedFilter.dataType) {
                    DataType.SCENE -> {
                        val objectFilter =
                            filterParser.convertSceneObjectFilter(resolvedFilter.objectFilter)
                        SceneDataSupplier(
                            resolvedFilter.findFilter?.asFindFilterType,
                            objectFilter,
                        )
                    }
                    DataType.TAG -> {
                        val objectFilter =
                            filterParser.convertTagObjectFilter(resolvedFilter.objectFilter)
                        TagDataSupplier(
                            resolvedFilter.findFilter?.asFindFilterType,
                            objectFilter,
                        )
                    }

                    DataType.STUDIO -> {
                        val objectFilter =
                            filterParser.convertStudioObjectFilter(resolvedFilter.objectFilter)
                        StudioDataSupplier(
                            resolvedFilter.findFilter?.asFindFilterType,
                            objectFilter,
                        )
                    }

                    DataType.PERFORMER -> {
                        val objectFilter =
                            filterParser.convertPerformerObjectFilter(resolvedFilter.objectFilter)
                        PerformerDataSupplier(
                            resolvedFilter.findFilter?.asFindFilterType,
                            objectFilter,
                        )
                    }

                    DataType.IMAGE -> {
                        val objectFilter =
                            filterParser.convertImageObjectFilter(resolvedFilter.objectFilter)
                        ImageDataSupplier(
                            resolvedFilter.findFilter?.asFindFilterType,
                            objectFilter,
                        )
                    }

                    DataType.MARKER -> {
                        val objectFilter =
                            filterParser.convertMarkerObjectFilter(resolvedFilter.objectFilter)
                        MarkerDataSupplier(
                            resolvedFilter.findFilter?.asFindFilterType,
                            objectFilter,
                        )
                    }

                    DataType.MOVIE -> {
                        val objectFilter =
                            filterParser.convertMovieObjectFilter(resolvedFilter.objectFilter)
                        MovieDataSupplier(
                            resolvedFilter.findFilter?.asFindFilterType,
                            objectFilter,
                        )
                    }

                    DataType.GALLERY -> {
                        val objectFilter =
                            filterParser.convertGalleryObjectFilter(resolvedFilter.objectFilter)
                        GalleryDataSupplier(
                            resolvedFilter.findFilter?.asFindFilterType,
                            objectFilter,
                        )
                    }
                } as StashPagingSource.DataSupplier<*, Any, *>

            _resolvedFilterState.value =
                ResolvedFilterState.Success(
                    resolvedFilter,
                    StashPagingSource(
                        StashApplication.getApplication(),
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
    startingFilter: StashFilter,
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
            ResolvedFilterGrid(resolvedFilterState as ResolvedFilterState.Success, itemOnClick)
        }
    }
}

@OptIn(ExperimentalTvFoundationApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun ResolvedFilterGrid(
    resolvedFilter: ResolvedFilterState.Success,
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

    TvLazyVerticalGrid(
        modifier = Modifier.padding(16.dp),
        columns = TvGridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
        }
        items(lazyPagingItems.itemCount) { index ->
            val item = lazyPagingItems[index]
            if (item != null) {
                StashCard(item = item, itemOnClick)
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
    val context = LocalContext.current
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
