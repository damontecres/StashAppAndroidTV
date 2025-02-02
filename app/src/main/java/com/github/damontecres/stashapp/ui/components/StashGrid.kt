package com.github.damontecres.stashapp.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.SwitchWithLabel
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

enum class FilterUiMode {
    SAVED_FILTERS,
    CREATE_FILTER,
}

@Composable
fun StashGridControls(
    initialFilter: FilterArgs,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    uiConfig: ComposeUiConfig,
    filterUiMode: FilterUiMode,
    modifier: Modifier = Modifier,
    initialPosition: Int = 0,
    itemOnLongClick: ((Any) -> Unit)? = null,
    positionCallback: ((columns: Int, position: Int) -> Unit)? = null,
) {
    val fontFamily = FontFamily(Font(resId = R.font.fa_solid_900))
    val context = LocalContext.current

    val dataType = initialFilter.dataType
    var filterArgs by remember(initialFilter) { mutableStateOf(initialFilter) }
    var showTopRowRaw by remember { mutableStateOf(true) }
    val showTopRow by remember { derivedStateOf { showTopRowRaw } }
    var checked by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (showTopRow) {
            Row(
                modifier =
                    Modifier
                        .padding(8.dp)
                        .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                    if (filterUiMode == FilterUiMode.SAVED_FILTERS) {
                        SavedFiltersButton(
                            modifier = Modifier,
                            dataType = dataType,
                            onFilterChange = { filterArgs = it },
                            onCreateFilter = {
                                // TODO
                            },
                            onUpdateFilter = {
                                // TODO
                            },
                        )
                    }
                    SortByButton(
                        modifier = Modifier,
                        dataType = dataType,
                        current = filterArgs.sortAndDirection,
                        onSortChange = { filterArgs = filterArgs.with(it) },
                    )
                    if (dataType.supportsPlaylists || dataType == DataType.IMAGE) {
                        Button(
                            onClick = {},
                        ) {
                            Text(text = stringResource(R.string.play_all))
                        }
                    }
                    if (filterUiMode == FilterUiMode.CREATE_FILTER) {
                        Button(
                            onClick = {},
                        ) {
                            Text(text = "Create Filter")
                        }
                    }
                    if (dataType.supportsSubContent) {
                        SwitchWithLabel(
                            modifier = Modifier,
                            label = stringResource(R.string.stashapp_include_sub_tag_content),
                            state = checked,
                            onStateChange = { isChecked ->
                                checked = isChecked
                            },
                        )
                    }
                }
                // TODO search
            }
        }
        StashGrid(
            filterArgs,
            uiConfig,
            itemOnClick,
            longClicker,
            Modifier.fillMaxSize(),
            initialPosition,
            positionCallback = { columns, position ->
                showTopRowRaw = position < columns
                positionCallback?.invoke(columns, position)
            },
        )
    }
}

class StashGridViewModel(
    server: StashServer,
    filterArgs: FilterArgs,
) : ViewModel() {
    private val _pager = MutableLiveData<ComposePager<StashData>>()
    val pager: LiveData<ComposePager<StashData>> = _pager

    init {
        val dataSupplierFactory = DataSupplierFactory(server.version)
        val dataSupplier =
            dataSupplierFactory.create<Query.Data, StashData, Query.Data>(filterArgs)
        val pagingSource =
            StashPagingSource(QueryEngine(server), dataSupplier) { _, _, item -> item }
        _pager.value = ComposePager(pagingSource, viewModelScope)
        viewModelScope.launch { _pager.value!!.init() }
    }

    companion object {
        val SERVER_KEY = object : CreationExtras.Key<StashServer> {}
        val FILTER_KEY = object : CreationExtras.Key<FilterArgs> {}
        val FACTORY: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val server = this[SERVER_KEY]!!
                    val filterArgs = this[FILTER_KEY]!!
                    StashGridViewModel(server, filterArgs)
                }
            }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StashGrid(
    filterArgs: FilterArgs,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    initialPosition: Int = 0,
    positionCallback: ((columns: Int, position: Int) -> Unit)? = null,
) {
    val columns = 5
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val server = StashServer.requireCurrentServer()
    key(filterArgs) {
        val viewModel =
            ViewModelProvider.create(
                LocalViewModelStoreOwner.current!!,
                StashGridViewModel.FACTORY,
                MutableCreationExtras().apply {
                    set(StashGridViewModel.SERVER_KEY, server)
                    set(StashGridViewModel.FILTER_KEY, filterArgs)
                },
            )[StashGridViewModel::class]
        val pager by viewModel.pager.observeAsState()

        val firstFocus = remember { FocusRequester() }
        var focusedIndex by rememberSaveable { mutableIntStateOf(initialPosition) }
        if (initialPosition > 0) {
            LaunchedEffect(filterArgs, initialPosition) {
                gridState.scrollToItem(focusedIndex, -columns)
            }
        }
        pager?.let { pager ->
            Row(
                modifier =
                    modifier
                        .fillMaxSize(),
            ) {
                JumpButtons(
                    itemCount = pager.size(),
                    jumpClick = { jump ->
                        scope.launch {
                            val newPosition =
                                (gridState.firstVisibleItemIndex + jump).coerceIn(0..<pager.size())
                            gridState.scrollToItem(newPosition)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                Box(
                    modifier = Modifier,
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        state = gridState,
                        contentPadding = PaddingValues(16.dp),
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .focusGroup()
                                .focusRestorer { firstFocus },
//                    .focusRestorer(),
                    ) {
                        if (pager.size() < 0) {
                            item {
                                Text(
                                    text = "Waiting for items to load from the backend",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                            .wrapContentHeight(Alignment.CenterVertically),
                                )
                            }
                        } else if (pager.size() == 0) {
                            item {
                                Text(
                                    text = stringResource(R.string.stashapp_studio_tagger_no_results_found),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                            .wrapContentHeight(Alignment.CenterVertically),
                                )
                            }
                        } else {
                            items(pager.size()) { index ->
                                val mod =
                                    if (index == focusedIndex) {
                                        Modifier.focusRequester(firstFocus)
                                    } else {
                                        Modifier
                                    }
                                val item = pager[index]
                                if (item == null) {
                                    Text(
                                        text = "Loading...",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier =
                                            mod
                                                .fillMaxWidth()
                                                .wrapContentWidth(Alignment.CenterHorizontally),
                                    )
                                } else {
                                    StashCard(
                                        modifier =
                                            mod.onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    focusedIndex = index
                                                    positionCallback?.invoke(columns, index)
                                                }
                                            },
                                        uiConfig = uiConfig,
                                        item = item,
                                        itemOnClick = {
                                            itemOnClick.onClick(
                                                it,
                                                FilterAndPosition(filterArgs, index),
                                            )
                                        },
                                        longClicker = longClicker,
                                        getFilterAndPosition = {
                                            FilterAndPosition(
                                                filterArgs,
                                                index,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                    // Footer
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .background(
                                    Color(
                                        LocalContext.current.resources.getColor(
                                            R.color.transparent_black_50,
                                            null,
                                        ),
                                    ),
                                ),
                    ) {
                        Text(
                            modifier = Modifier.padding(4.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                            text = "${focusedIndex + 1} / ${pager.size()}",
                        )
                    }
                }
                // Letters
                if (pager.size() > 0 &&
                    SortOption.isJumpSupported(
                        filterArgs.dataType,
                        filterArgs.sortAndDirection.sort,
                    )
                ) {
                    AlphabetButtons(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        letterClicked = { letter ->
                            scope.launch {
                                val dataSupplierFactory = DataSupplierFactory(server.version)
                                val letterPosition =
                                    AlphabetSearchUtils.findPosition(
                                        letter,
                                        filterArgs,
                                        QueryEngine(server),
                                        dataSupplierFactory,
                                    )
//                Log.v(TAG, "Found position for $letter: $letterPosition")
                                val jumpPosition =
                                    if (filterArgs
                                            .sortAndDirection
                                            .direction == SortDirectionEnum.DESC
                                    ) {
                                        // Reverse if sorting descending
                                        pager.size() - letterPosition - 1
                                    } else {
                                        letterPosition
                                    }

                                gridState.scrollToItem(jumpPosition)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun JumpButtons(
    itemCount: Int,
    jumpClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontFamily = FontFamily(Font(resId = R.font.fa_solid_900))
    Column(
        modifier = modifier,
    ) {
        Button(
            onClick = {
                jumpClick.invoke(-10)
            },
        ) {
            Text(text = stringResource(R.string.fa_angle_up), fontFamily = fontFamily)
        }
        Button(
            onClick = {
                jumpClick.invoke(10)
            },
        ) {
            Text(text = stringResource(R.string.fa_angle_down), fontFamily = fontFamily)
        }
    }
}

@Composable
fun AlphabetButtons(
    letterClicked: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(
            AlphabetSearchUtils.LETTERS.length,
            key = { AlphabetSearchUtils.LETTERS[it] },
        ) { index ->
            Log.d("Compose", "AlphabetButtons $index")
            Button(
                onClick = {
                    letterClicked.invoke(AlphabetSearchUtils.LETTERS[index])
                },
            ) {
                Text(text = AlphabetSearchUtils.LETTERS[index].toString())
            }
        }
    }
}
