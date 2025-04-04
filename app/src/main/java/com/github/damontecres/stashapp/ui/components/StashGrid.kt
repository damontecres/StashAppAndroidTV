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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.SwitchWithLabel
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "StashGrid"

enum class FilterUiMode {
    SAVED_FILTERS,
    CREATE_FILTER,
}

enum class CreateFilter {
    FROM_CURRENT,
    NEW_FILTER,
}

@Composable
fun StashGridControls(
    server: StashServer,
    pager: ComposePager<StashData>,
    updateFilter: (FilterArgs) -> Unit,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    uiConfig: ComposeUiConfig,
    filterUiMode: FilterUiMode,
    createFilter: (CreateFilter) -> Unit,
    letterPosition: suspend (Char) -> Int,
    requestFocus: Boolean,
    modifier: Modifier = Modifier,
    initialPosition: Int = 0,
    itemOnLongClick: ((Any) -> Unit)? = null,
    positionCallback: ((columns: Int, position: Int) -> Unit)? = null,
    subToggleLabel: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filterArgs = pager.filter
    val dataType = filterArgs.dataType
    var showTopRowRaw by rememberSaveable { mutableStateOf(true) }
    val showTopRow by remember { derivedStateOf { showTopRowRaw } }
    var checked by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable(filterArgs) {
        mutableStateOf(
            filterArgs.findFilter?.q ?: "",
        )
    }
    var shouldRequestFocus by remember { mutableStateOf(requestFocus) }
    val gridFocusRequester = remember { FocusRequester() }

    val navManager = LocalGlobalContext.current.navigationManager

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
                            modifier = Modifier.focusProperties { down = gridFocusRequester },
                            dataType = dataType,
                            onFilterChange = { updateFilter(it) },
                            onCreateFilter = { createFilter.invoke(CreateFilter.NEW_FILTER) },
                            onFCreateFromFilter = { createFilter.invoke(CreateFilter.FROM_CURRENT) },
                        )
                    }
                    SortByButton(
                        modifier = Modifier.focusProperties { down = gridFocusRequester },
                        dataType = dataType,
                        current = filterArgs.sortAndDirection,
                        onSortChange = { updateFilter(filterArgs.with(it)) },
                    )
                    if (dataType.supportsPlaylists || dataType == DataType.IMAGE) {
                        Button(
                            onClick = {
                                val destination =
                                    if (dataType == DataType.IMAGE) {
                                        Destination.Slideshow(filterArgs, 0, true)
                                    } else {
                                        Destination.Playlist(filterArgs, 0)
                                    }
                                navManager.navigate(destination)
                            },
                            modifier = Modifier.focusProperties { down = gridFocusRequester },
                        ) {
                            Text(text = stringResource(R.string.play_all))
                        }
                    }
                    if (filterUiMode == FilterUiMode.CREATE_FILTER) {
                        Button(
                            onClick = {
                                createFilter.invoke(CreateFilter.FROM_CURRENT)
                            },
                            modifier = Modifier.focusProperties { down = gridFocusRequester },
                        ) {
                            Text(text = "Create Filter")
                        }
                    }
                    if (subToggleLabel != null) {
                        SwitchWithLabel(
                            modifier = Modifier.focusProperties { down = gridFocusRequester },
                            label = subToggleLabel,
                            state = checked,
                            onStateChange = { isChecked ->
                                checked = isChecked
                            },
                        )
                    }
                }
                var job: Job? = null
                val searchDelay =
                    PreferenceManager
                        .getDefaultSharedPreferences(context)
                        .getInt(context.getString(R.string.pref_key_search_delay), 500)
                        .toLong()
                SearchEditTextBox(
                    modifier =
                        Modifier.focusProperties { down = gridFocusRequester },
                    value = searchQuery,
                    onValueChange = { newQuery ->
                        shouldRequestFocus = false
                        searchQuery = newQuery
                        job?.cancel()
                        job =
                            scope.launch {
                                delay(searchDelay)
                                if ((filterArgs.findFilter?.q ?: "") != searchQuery) {
                                    updateFilter(filterArgs.withQuery(searchQuery))
                                }
                            }
                    },
                    onSearchClick = {
                        shouldRequestFocus = true
                        job?.cancel()
                        if ((filterArgs.findFilter?.q ?: "") != searchQuery) {
                            updateFilter(filterArgs.withQuery(searchQuery))
                        }
                    },
                )
            }
        }
        StashGrid(
            pager,
            uiConfig,
            itemOnClick,
            longClicker,
            requestFocus = shouldRequestFocus,
            letterPosition = letterPosition,
            initialPosition = initialPosition,
            positionCallback = { columns, position ->
                showTopRowRaw = position < columns
                positionCallback?.invoke(columns, position)
            },
            gridFocusRequester = gridFocusRequester,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StashGrid(
    pager: ComposePager<StashData>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    letterPosition: suspend (Char) -> Int,
    requestFocus: Boolean,
    gridFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    initialPosition: Int = 0,
    positionCallback: ((columns: Int, position: Int) -> Unit)? = null,
) {
    val columns = 5
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val filterArgs = pager.filter
    val firstFocus = remember(pager) { FocusRequester() }
    var focusedIndex by rememberSaveable(pager) { mutableIntStateOf(initialPosition) }
    if (initialPosition > 0) {
        Log.v(TAG, "Scroll to $initialPosition")
        LaunchedEffect(Unit) {
            gridState.scrollToItem(initialPosition, -columns)
        }
    }
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
                        .focusRestorer { firstFocus }
                        .focusRequester(gridFocusRequester),
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
                            if (requestFocus && focusedIndex == index) {
                                LaunchedEffect(Unit) {
                                    firstFocus.requestFocus()
                                }
                            }
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
                        val jumpPosition = letterPosition.invoke(letter)
                        gridState.scrollToItem(jumpPosition)
                    }
                },
            )
        }
    }
}

@Composable
fun JumpButtons(
    itemCount: Int,
    jumpClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Button(
            onClick = {
                jumpClick.invoke(-10)
            },
        ) {
            Text(text = stringResource(R.string.fa_angle_up), fontFamily = FontAwesome)
        }
        Button(
            onClick = {
                jumpClick.invoke(10)
            },
        ) {
            Text(text = stringResource(R.string.fa_angle_down), fontFamily = FontAwesome)
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

@Composable
fun SearchQuery(
    query: String?,
    onQueryChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
}
