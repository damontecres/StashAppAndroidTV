package com.github.damontecres.stashapp.ui.pages

import android.widget.Toast
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationManager
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
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private lateinit var server: StashServer
    private var currentQuery = ""

    val scenes = MutableLiveData<List<Any>>(listOf())
    val groups = MutableLiveData<List<Any>>(listOf())
    val markers = MutableLiveData<List<Any>>(listOf())
    val performers = MutableLiveData<List<Any>>(listOf())
    val studios = MutableLiveData<List<Any>>(listOf())
    val tags = MutableLiveData<List<Any>>(listOf())
    val images = MutableLiveData<List<Any>>(listOf())
    val galleries = MutableLiveData<List<Any>>(listOf())

    val mapping =
        mapOf(
            DataType.SCENE to scenes,
            DataType.GROUP to groups,
            DataType.MARKER to markers,
            DataType.PERFORMER to performers,
            DataType.STUDIO to studios,
            DataType.TAG to tags,
            DataType.IMAGE to images,
            DataType.GALLERY to galleries,
        )

    fun init(
        server: StashServer,
        initialQuery: String,
    ) {
        this.server = server
        search(initialQuery)
    }

    fun search(query: String) {
        if (query.isNotBlank() && query != this.currentQuery) {
            this.currentQuery = query
            val queryEngine = QueryEngine(server)
            val perPage =
                PreferenceManager
                    .getDefaultSharedPreferences(StashApplication.getApplication())
                    .getInt("maxSearchResults", 25)
            DataType.entries.forEach {
                val data = mapping[it]!!
                data.value = listOf()

                val stashFindFilter =
                    StashFindFilter(
                        q = query,
                        sortAndDirection =
                            SortAndDirection(
                                SortOption.sortByName(it),
                                SortDirectionEnum.ASC,
                            ),
                    )
                val findFilter =
                    stashFindFilter.toFindFilterType(
                        perPage = perPage,
                        page = 1,
                    )

                viewModelScope.launch(
                    StashCoroutineExceptionHandler { ex ->
                        Toast.makeText(
                            StashApplication.getApplication(),
                            "Search for ${StashApplication.getApplication().getString(it.pluralStringId)} failed: ${ex.message}",
                            Toast.LENGTH_LONG,
                        )
                    },
                ) {
                    val results = queryEngine.find(it, findFilter)
                    if (results.isNotEmpty()) {
                        data.value = results
                    }
                }
            }
        } else if (query != this.currentQuery) {
            mapping.values.forEach { it.value = listOf() }
        }
    }
}

@Composable
fun SearchPage(
    server: StashServer,
    navigationManager: NavigationManager,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier.fillMaxSize(),
    initialQuery: String = "",
    viewModel: SearchViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var searchQuery by rememberSaveable { mutableStateOf(initialQuery) }

    val scenes by viewModel.scenes.observeAsState(listOf())
    val groups by viewModel.groups.observeAsState(listOf())
    val markers by viewModel.markers.observeAsState(listOf())
    val performers by viewModel.performers.observeAsState(listOf())
    val studios by viewModel.studios.observeAsState(listOf())
    val tags by viewModel.tags.observeAsState(listOf())
    val images by viewModel.images.observeAsState(listOf())
    val galleries by viewModel.galleries.observeAsState(listOf())

    val itemLists =
        mapOf(
            DataType.SCENE to scenes,
            DataType.GROUP to groups,
            DataType.MARKER to markers,
            DataType.PERFORMER to performers,
            DataType.STUDIO to studios,
            DataType.TAG to tags,
            DataType.IMAGE to images,
            DataType.GALLERY to galleries,
        )

    OneTimeLaunchedEffect {
        viewModel.init(server, initialQuery)
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
            val searchDelay =
                PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getInt(context.getString(R.string.pref_key_search_delay), 500)
                    .toLong()
            SearchEditTextBox(
                modifier = Modifier.ifElse(focusedRow < 0, Modifier.focusRequester(focusRequester)),
                value = searchQuery,
                onValueChange = { newQuery ->
                    searchQuery = newQuery
                    job?.cancel()
                    job =
                        scope.launch {
                            delay(searchDelay)
                            viewModel.search(searchQuery)
                        }
                },
                onSearchClick = {
                    job?.cancel()
                    viewModel.search(searchQuery)
                },
            )
        }

        DataType.entries.forEachIndexed { index, dataType ->
            val data = itemLists[dataType]!!
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
                    .focusRestorer(firstFocus),
            state = listState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(items) { index, item ->
                val cardModifier =
                    if (index == 0) {
                        Modifier.focusRequester(firstFocus)
                    } else {
                        Modifier
                    }
                StashCard(
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
