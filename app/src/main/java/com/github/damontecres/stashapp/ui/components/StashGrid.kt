package com.github.damontecres.stashapp.ui.components

import android.content.res.Configuration
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.ui.compat.isNotTvDevice
import com.github.damontecres.stashapp.ui.components.playback.isBackwardButton
import com.github.damontecres.stashapp.ui.components.playback.isForwardButton
import com.github.damontecres.stashapp.ui.isPlayKeyUp
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.getPlayDestinationForItem
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.defaultCardWidth
import com.github.damontecres.stashapp.util.getPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

private const val TAG = "StashGrid"
private const val DEBUG = false

enum class FilterUiMode {
    SAVED_FILTERS,
    CREATE_FILTER,
}

enum class CreateFilter {
    FROM_CURRENT,
    NEW_FILTER,
}

@OptIn(ExperimentalComposeUiApi::class)
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
    onSubToggleCheck: ((Boolean) -> Unit)? = null,
    subToggleChecked: Boolean = false,
    subToggleEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filterArgs = pager.filter
    val dataType = filterArgs.dataType
    var showTopRowRaw by rememberSaveable { mutableStateOf(true) }
    val showTopRow by remember { derivedStateOf { showTopRowRaw } }
    var checked by rememberSaveable(filterArgs) { mutableStateOf(subToggleChecked) }
    var searchQuery by rememberSaveable(filterArgs) {
        mutableStateOf(
            filterArgs.findFilter?.q ?: "",
        )
    }
    var shouldRequestFocus by remember { mutableStateOf(requestFocus) }
    val gridFocusRequester = remember { FocusRequester() }
//    LaunchedEffect(Unit) {
//        gridFocusRequester.tryRequestFocus()
//    }

    val navManager = LocalGlobalContext.current.navigationManager
    val rowFocusRequester = remember { FocusRequester() }

//    LaunchedEffect(Unit) {
//        if (requestFocus && showTopRowRaw) {
//            rowFocusRequester.tryRequestFocus()
//        } else if (requestFocus) {
//            gridFocusRequester.tryRequestFocus()
//        }
//    }
    var showMarkerDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (showTopRow) {
            ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                LazyRow(
                    modifier =
                        Modifier
                            .padding(8.dp)
                            .focusGroup()
                            .onFocusChanged {
                                if (it.isFocused) rowFocusRequester.tryRequestFocus()
                            }.focusable(true),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (filterUiMode == FilterUiMode.SAVED_FILTERS) {
                        item {
                            SavedFiltersButton(
                                modifier =
                                    Modifier
                                        .focusRequester(rowFocusRequester)
                                        .focusProperties { down = gridFocusRequester },
                                dataType = dataType,
                                onFilterChange = { updateFilter(it) },
                                onCreateFilter = { createFilter.invoke(CreateFilter.NEW_FILTER) },
                                onFCreateFromFilter = { createFilter.invoke(CreateFilter.FROM_CURRENT) },
                            )
                        }
                    }
                    item {
                        SortByButton(
                            modifier =
                                Modifier
                                    .ifElse(
                                        filterUiMode != FilterUiMode.SAVED_FILTERS,
                                        { Modifier.focusRequester(rowFocusRequester) },
                                    ).focusProperties { down = gridFocusRequester },
                            dataType = dataType,
                            current = filterArgs.sortAndDirection,
                            onSortChange = {
                                updateFilter(filterArgs.with(it).withResolvedRandom())
                            },
                        )
                    }
                    if (dataType.supportsPlaylists || dataType == DataType.IMAGE) {
                        item {
                            Button(
                                onClick = {
                                    when (dataType) {
                                        DataType.IMAGE -> {
                                            navManager.navigate(
                                                Destination.Slideshow(
                                                    filterArgs,
                                                    0,
                                                    true,
                                                ),
                                            )
                                        }

                                        DataType.MARKER -> {
                                            showMarkerDialog = true
                                        }

                                        else -> {
                                            navManager.navigate(
                                                Destination.Playlist(
                                                    filterArgs,
                                                    0,
                                                    0,
                                                ),
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.focusProperties { down = gridFocusRequester },
                            ) {
                                Text(text = stringResource(R.string.play_all))
                            }
                        }
                    }
                    if (filterUiMode == FilterUiMode.CREATE_FILTER) {
                        item {
                            Button(
                                onClick = {
                                    createFilter.invoke(CreateFilter.FROM_CURRENT)
                                },
                                modifier = Modifier.focusProperties { down = gridFocusRequester },
                            ) {
                                Text(text = "Create Filter")
                            }
                        }
                    }
                    if (subToggleLabel != null) {
                        item {
                            SwitchWithLabel(
                                modifier = Modifier.focusProperties { down = gridFocusRequester },
                                label = subToggleLabel,
                                checked = checked,
                                enabled = subToggleEnabled,
                                onStateChange = { isChecked ->
                                    checked = isChecked
                                    onSubToggleCheck?.invoke(isChecked)
                                },
                            )
                        }
                    }

                    var job: Job? = null
                    val searchDelay = uiConfig.preferences.searchPreferences.searchDelayMs
                    item {
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
                                gridFocusRequester.tryRequestFocus()
                            },
                        )
                    }
                }
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusProperties {
                        exit = {
                            if (it == FocusDirection.Up) {
                                rowFocusRequester
                            } else {
                                FocusRequester.Default
                            }
                        }
                    },
        )
    }
    if (showMarkerDialog) {
        MarkerDurationDialog(
            onDismissRequest = { showMarkerDialog = false },
            onClick = {
                showMarkerDialog = false
                val destination = Destination.Playlist(filterArgs, 0, it)
                navManager.navigate(destination)
            },
        )
    }
}

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
    val orientation = LocalConfiguration.current.orientation
    val navigationManager = LocalGlobalContext.current.navigationManager
    val startPosition = initialPosition.coerceIn(0, (pager.size - 1).coerceAtLeast(0))
    val columns =
        (
            uiConfig.cardSettings.columns * (ScenePresenter.CARD_WIDTH.toDouble() / pager.filter.dataType.defaultCardWidth) +
                // TODO better sizing
                if (isNotTvDevice && orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0
        ).toInt()

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val filterArgs = pager.filter
    val firstFocus = remember { FocusRequester() }
    val zeroFocus = remember { FocusRequester() }
    var previouslyFocusedIndex by rememberSaveable { mutableIntStateOf(0) }
    var focusedIndex by rememberSaveable { mutableIntStateOf(initialPosition) }
    var focusedIndexOnExit by rememberSaveable { mutableIntStateOf(-1) }

    // Tracks whether the very first requestFocus has run, if the caller isn't requesting focus,
    // then the first time will never run
    var hasRequestFocusRun by rememberSaveable { mutableStateOf(!requestFocus) }
    var savedFocusedIndex by rememberSaveable { mutableIntStateOf(-1) }

    if (DEBUG) {
        Log.d(
            TAG,
            "StashGrid: hasRun=$hasRequestFocusRun, requestFocus=$requestFocus, initialPosition=$initialPosition, focusedIndex=$focusedIndex",
        )
    }

    LaunchedEffect(Unit) {
        if (!hasRequestFocusRun) {
            // On very first composition, if parent wants to focus on the grid, scroll to the item
            if (requestFocus && initialPosition >= 0) {
                if (DEBUG) {
                    Log.d(
                        TAG,
                        "focus on startPosition=$startPosition, from initialPosition=$initialPosition",
                    )
                }
                focusedIndex = startPosition
                gridState.scrollToItem(startPosition, 0)
                firstFocus.tryRequestFocus()
            }
        } else {
            val index = savedFocusedIndex
            if (DEBUG) Log.d(TAG, "savedFocusedIndex=$index")
            if (index in 0..<pager.size) {
                // If this is a recomposition, but not the first
                // focus on the restored index
                // gridState.scrollToItem(index, -columns)
                firstFocus.tryRequestFocus()
            }
            savedFocusedIndex = -1
        }
//        hasRun = true
    }

    val context = LocalContext.current
    val showJumpButtons =
        remember { getPreference(context, R.string.pref_key_ui_grid_jump_controls, true) }

    var alphabetFocus by remember { mutableStateOf(false) }
    val focusOn = { index: Int ->
        if (DEBUG) Log.v(TAG, "focusOn: focusedIndex=$focusedIndex, index=$index")
        if (index != focusedIndex) {
            previouslyFocusedIndex = focusedIndex
        }
        focusedIndex = index
    }

    if (isNotTvDevice) {
        // Only focusing invokes positionCallback, so on touch, listen to changes
        // TODO Maybe can use this for both tv & touch?
        LaunchedEffect(gridState) {
            snapshotFlow { gridState.firstVisibleItemIndex }
                .collect {
                    positionCallback?.invoke(columns, it)
                    focusOn.invoke(it)
                }
        }
    }

    // Wait for a recomposition to focus
    LaunchedEffect(alphabetFocus) {
        if (alphabetFocus) {
            firstFocus.tryRequestFocus()
        }
        alphabetFocus = false
    }

    val useBackToJump = uiConfig.preferences.interfacePreferences.scrollTopOnBack
    val showFooter = uiConfig.preferences.interfacePreferences.showPositionFooter
    val useJumpRemoteButtons = uiConfig.preferences.interfacePreferences.pageWithRemoteButtons
    val jump2 =
        remember {
            if (pager.size >= 25_000) {
                columns * 2000
            } else if (pager.size >= 7_000) {
                columns * 200
            } else if (pager.size >= 2_000) {
                columns * 50
            } else {
                columns * 20
            }
        }
    val jump1 =
        remember {
            if (pager.size >= 25_000) {
                columns * 500
            } else if (pager.size >= 7_000) {
                columns * 50
            } else if (pager.size >= 2_000) {
                columns * 15
            } else {
                columns * 6
            }
        }

    val jump = { jump: Int ->
        scope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val newPosition =
                (gridState.firstVisibleItemIndex + jump).coerceIn(0..<pager.size)
            if (DEBUG) Log.d(TAG, "newPosition=$newPosition")
            savedFocusedIndex = newPosition
            focusOn(newPosition)
            gridState.scrollToItem(newPosition, 0)
        }
    }
    val jumpToTop = {
        scope.launch(StashCoroutineExceptionHandler()) {
            if (focusedIndex < (columns * 6)) {
                // If close, animate the scroll
                gridState.animateScrollToItem(0, 0)
            } else {
                gridState.scrollToItem(0, 0)
            }
            focusOn(0)
            zeroFocus.tryRequestFocus()
        }
    }
    val server = LocalGlobalContext.current.server

    var longPressing by remember { mutableStateOf(false) }
    Row(
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
                .fillMaxSize()
                .onKeyEvent {
                    if (DEBUG) Log.d(TAG, "onKeyEvent: ${it.nativeKeyEvent}")
                    if (useBackToJump && it.key == Key.Back && it.nativeKeyEvent.isLongPress) {
                        longPressing = true
                        val newPosition = previouslyFocusedIndex
                        if (DEBUG) Log.d(TAG, "Back long pressed: newPosition=$newPosition")
                        if (newPosition > 0) {
                            focusOn(newPosition)
                            scope.launch(StashCoroutineExceptionHandler()) {
                                gridState.scrollToItem(newPosition, -columns)
                                firstFocus.tryRequestFocus()
                            }
                        }
                        return@onKeyEvent true
                    } else if (it.type == KeyEventType.KeyUp) {
                        if (longPressing && it.key == Key.Back) {
                            longPressing = false
                            return@onKeyEvent true
                        }
                        longPressing = false
                    }
                    if (it.type != KeyEventType.KeyUp) {
                        return@onKeyEvent false
                    } else if (useBackToJump && it.key == Key.Back && focusedIndex > 0) {
                        jumpToTop()
                        return@onKeyEvent true
                    } else if (isPlayKeyUp(it)) {
                        val destination =
                            getPlayDestinationForItem(
                                server,
                                pager[focusedIndex],
                                FilterAndPosition(pager.filter, focusedIndex),
                            )
                        return@onKeyEvent if (destination != null) {
                            navigationManager.navigate(destination)
                            true
                        } else {
                            false
                        }
                    } else if (useJumpRemoteButtons && isForwardButton(it)) {
                        jump(jump1)
                        return@onKeyEvent true
                    } else if (useJumpRemoteButtons && isBackwardButton(it)) {
                        jump(-jump1)
                        return@onKeyEvent true
                    } else {
                        return@onKeyEvent false
                    }
                },
    ) {
        if (showJumpButtons && pager.size > 0) {
            JumpButtons(
                jump1 = jump1,
                jump2 = jump2,
                jumpClick = { jump(it) },
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Box(
            modifier = Modifier.weight(1f),
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
                        .focusRequester(gridFocusRequester)
                        .focusProperties {
                            onExit = {
                                // Leaving the grid, so "forget" the position
                                focusedIndexOnExit = focusedIndex
                                focusedIndex = -1
                                savedFocusedIndex = -1
                            }
                            onEnter = {
                                focusedIndexOnExit = -1
                                if (focusedIndex < 0 && gridState.firstVisibleItemIndex <= startPosition) {
                                    focusedIndex = startPosition
                                    firstFocus.tryRequestFocus()
                                }
                            }
                        },
            ) {
                items(pager.size) { index ->
                    val mod =
                        if (index == savedFocusedIndex) {
                            if (DEBUG) Log.d(TAG, "Adding firstFocus to itemClickedIndex $index")
                            Modifier.focusRequester(firstFocus)
                        } else if ((index == focusedIndex) or (focusedIndex < 0 && index == 0)) {
                            if (DEBUG) Log.d(TAG, "Adding firstFocus to focusedIndex $index")
                            Modifier.focusRequester(firstFocus)
                        } else {
                            Modifier
                        }
                    val item = pager[index]
                    if (!hasRequestFocusRun && requestFocus && initialPosition >= 0) {
                        // On very first composition, if parent wants to focus on the grid, do so
                        LaunchedEffect(Unit) {
                            if (DEBUG) {
                                Log.d(
                                    TAG,
                                    "non-null focus on startPosition=$startPosition, from initialPosition=$initialPosition",
                                )
                            }
                            // focus on startPosition
                            gridState.scrollToItem(startPosition, 0)
                            firstFocus.tryRequestFocus()
                            hasRequestFocusRun = true
                        }
                    }
                    StashCard(
                        modifier =
                            mod
                                .ifElse(index == 0, Modifier.focusRequester(zeroFocus))
                                .onFocusChanged { focusState ->
                                    if (DEBUG) {
                                        Log.v(
                                            TAG,
                                            "$index isFocused=${focusState.isFocused}",
                                        )
                                    }
                                    if (focusState.isFocused) {
                                        // Focused, so set that up
                                        focusOn(index)
                                        positionCallback?.invoke(columns, index)
                                    } else if (focusedIndex == index) {
                                        savedFocusedIndex = index
                                        // Was focused on this, so mark unfocused
                                        focusedIndex = -1
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
            if (pager.size == 0) {
//                focusedIndex = -1
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.stashapp_studio_tagger_no_results_found),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            if (showFooter) {
                // Footer
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .background(AppColors.TransparentBlack50),
                ) {
                    val index =
                        if (focusedIndex >= 0) {
                            focusedIndex + 1
                        } else {
                            max(savedFocusedIndex, focusedIndexOnExit) + 1
                        }
                    Text(
                        modifier = Modifier.padding(4.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                        text = "$index / ${pager.size}",
                    )
                }
            }
            if (isNotTvDevice) {
                androidx.compose.animation.AnimatedVisibility(
                    focusedIndex > columns,
                    modifier = Modifier.align(Alignment.BottomEnd),
                ) {
                    // Can use material3 here since it's only for non-TV
                    androidx.compose.material3.Button(
                        onClick = { jumpToTop() },
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }
        }
        // Letters
        if (pager.isNotEmpty() &&
            SortOption.isJumpSupported(
                filterArgs.dataType,
                filterArgs.sortAndDirection.sort,
            )
        ) {
            AlphabetButtons(
                modifier = Modifier.align(Alignment.CenterVertically),
                letterClicked = { letter ->
                    scope.launch(StashCoroutineExceptionHandler()) {
                        val jumpPosition = letterPosition.invoke(letter)
                        Log.d(TAG, "Alphabet jump to $jumpPosition")
                        gridState.scrollToItem(jumpPosition)
                        focusOn(jumpPosition)
                        alphabetFocus = true
//                        firstFocus.tryRequestFocus()
                    }
                },
            )
        }
    }
}

@Composable
fun JumpButtons(
    jump1: Int,
    jump2: Int,
    jumpClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        JumpButton(R.string.fa_angles_up, -jump2, jumpClick)
        JumpButton(R.string.fa_angle_up, -jump1, jumpClick)
        JumpButton(R.string.fa_angle_down, jump1, jumpClick)
        JumpButton(R.string.fa_angles_down, jump2, jumpClick)
    }
}

@Composable
fun JumpButton(
    @StringRes stringRes: Int,
    jumpAmount: Int,
    jumpClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier.width(40.dp),
        contentPadding = PaddingValues(4.dp),
        onClick = {
            jumpClick.invoke(jumpAmount)
        },
    ) {
        Text(text = stringResource(stringRes), fontFamily = FontAwesome)
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
                modifier =
                    Modifier.size(24.dp),
                contentPadding = PaddingValues(2.dp),
                onClick = {
                    letterClicked.invoke(AlphabetSearchUtils.LETTERS[index])
                },
            ) {
                Text(text = AlphabetSearchUtils.LETTERS[index].toString())
            }
        }
    }
}
