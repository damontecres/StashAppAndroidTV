package com.github.damontecres.stashapp.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
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
    itemOnClick: (Any) -> Unit,
    filterUiMode: FilterUiMode,
    modifier: Modifier = Modifier,
    itemOnLongClick: ((Any) -> Unit)? = null,
    positionCallback: ((columns: Int, position: Int) -> Unit)? = null,
) {
    val dataType = initialFilter.dataType
    var filterArgs by remember { mutableStateOf(initialFilter) }
    var showTopRowRaw by remember { mutableStateOf(true) }
    val showTopRow by remember { derivedStateOf { showTopRowRaw } }
    var checked by remember { mutableStateOf(false) }

    var sortByDropDown by remember { mutableStateOf(false) }

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
                        Button(
                            onClick = {},
                        ) {
                            Text(text = stringResource(R.string.stashapp_search_filter_saved_filters))
                        }
                    }
                    Box {
                        Button(
                            onClick = { sortByDropDown = true },
                            onLongClick = {},
                        ) {
                            Text(text = stringResource(R.string.sort_by))
                        }
                        DropdownMenu(
                            expanded = sortByDropDown,
                            onDismissRequest = { sortByDropDown = false },
                        ) {
                            dataType.sortOptions.forEach { sortOption ->
                                DropdownMenuItem(
                                    text = { Text(sortOption.getString(LocalContext.current)) },
                                    onClick = {
                                        sortByDropDown = false
                                        filterArgs =
                                            filterArgs.with(
                                                SortAndDirection(
                                                    sortOption,
                                                    SortDirectionEnum.ASC,
                                                ),
                                            )
                                    },
                                )
                            }
                        }
                    }
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
            itemOnClick,
            Modifier.fillMaxSize(),
            positionCallback = { columns, position ->
                showTopRowRaw = position < columns
                positionCallback?.invoke(columns, position)
            },
        )
    }
}

@Composable
fun StashGrid(
    filterArgs: FilterArgs,
    itemOnClick: (Any) -> Unit,
    modifier: Modifier = Modifier,
    positionCallback: ((columns: Int, position: Int) -> Unit)? = null,
) {
    val columns = 5
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val server = StashServer.requireCurrentServer()
    val dataSupplierFactory = DataSupplierFactory(server.version)

    key(filterArgs) {
        val dataSupplier =
            dataSupplierFactory.create<Query.Data, StashData, Query.Data>(filterArgs)
        val pagingSource =
            StashPagingSource(QueryEngine(server), dataSupplier) { _, _, item -> item }
        val pager = remember { ComposePager(pagingSource, scope) }

        LaunchedEffect(filterArgs) {
            pager.init()
        }

        val firstFocus = remember { FocusRequester() }
        var focusedIndex by remember { mutableIntStateOf(0) }

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
                    modifier =
                        Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                            .focusGroup(),
//                    .focusRestorer { firstFocus },
//                    .focusRestorer(),
                ) {
                    if (pager.size() < 0) {
                        item {
                            Text(
                                text = "Waiting for items to load from the backend",
                                color = Color.White,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth(Alignment.CenterHorizontally),
                            )
                        }
                    } else {
                        items(
                            pager.size(),
//                key = { pager[it]?.id },
                        ) { index ->
                            val mod =
                                if (index == (gridState.firstVisibleItemIndex + gridState.firstVisibleItemScrollOffset)) {
//                            Modifier.focusRequester(firstFocus)
                                    Modifier
                                } else {
                                    Modifier
                                }
                            val item = pager[index]
                            if (item == null) {
                                Text(
                                    text = "Loading...",
                                    color = Color.White,
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
                                    uiConfig = ComposeUiConfig(true),
                                    item = item,
                                    itemOnClick = itemOnClick,
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
                        color = Color.White,
                        text = "${focusedIndex + 1} / ${pager.size()}",
                    )
                }
            }
            // Letters
            Log.d(
                "Compose",
                "pager.size()=${pager.size()}, filterArgs.sortAndDirection.sort=${filterArgs.sortAndDirection.sort}",
            )
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
