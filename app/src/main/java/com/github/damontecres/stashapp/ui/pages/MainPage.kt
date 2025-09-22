package com.github.damontecres.stashapp.ui.pages

import android.content.Context
import android.util.Log
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transitionFactory
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.StatisticsQuery
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.cards.ViewAllCard
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.RowColumn
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.ui.components.main.MainPageHeader
import com.github.damontecres.stashapp.ui.isPlayKeyUp
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.CrossFadeFactory
import com.github.damontecres.stashapp.ui.util.OneTimeLaunchedEffect
import com.github.damontecres.stashapp.ui.util.getPlayDestinationForItem
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.getCaseInsensitive
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.formatBytes
import com.github.damontecres.stashapp.views.formatNumber
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val TAG = "MainPage"

class MainPageViewModel : ViewModel() {
    private lateinit var server: StashServer

    val frontPageRows = mutableStateListOf<FrontPageParser.FrontPageRow.Success>()

    private val _serverStats = MutableLiveData<StatisticsQuery.Stats?>()
    val serverStats: LiveData<StatisticsQuery.Stats?> = _serverStats

    fun init(
        context: Context,
        server: StashServer,
        pageSize: Int,
    ) {
        this.server = server
        val queryEngine = QueryEngine(server)
        val filterParser = FilterParser(server.version)
        val frontPageContent =
            server.serverPreferences.uiConfiguration?.getCaseInsensitive("frontPageContent") as List<Map<String, *>>?
        if (frontPageContent != null) {
            Log.d(TAG, "${frontPageContent.size} front page rows")
            val frontPageParser =
                FrontPageParser(
                    context,
                    queryEngine,
                    filterParser,
                    pageSize,
                )
            viewModelScope.launch(LoggingCoroutineExceptionHandler(server, viewModelScope)) {
                val jobs = frontPageParser.parse(frontPageContent)

                jobs.forEach { job ->
                    job.await().let { row ->
                        if (row is FrontPageParser.FrontPageRow.Success) {
                            frontPageRows.add(row)
                        }
                    }
                }
            }
        }
    }

    fun updateStatistics() {
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            val queryEngine = QueryEngine(server)
            _serverStats.value = queryEngine.executeQuery(StatisticsQuery()).data?.stats
        }
    }
}

@Composable
fun MainPage(
    server: StashServer,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    viewModel: MainPageViewModel = viewModel(),
) {
    val context = LocalContext.current
    OneTimeLaunchedEffect {
        viewModel.init(context, server, uiConfig.preferences.searchPreferences.maxResults)
    }

    val frontPageRows = viewModel.frontPageRows // .observeAsState(listOf())
    val serverStats by viewModel.serverStats.observeAsState()

    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        uiConfig.preferences.updatePreferences.let {
            if (it.checkForUpdates) {
                scope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                    UpdateChecker.maybeShowUpdateToast(
                        context,
                        it.updateUrl,
                        false,
                    )
                }
            }
        }
        viewModel.updateStatistics()
    }
    if (frontPageRows.isEmpty()) {
        Box(modifier = modifier.fillMaxSize()) {
            CircularProgress(
                modifier =
                    Modifier
                        .size(160.dp)
                        .align(Alignment.Center),
            )
        }
    } else {
        LaunchedEffect(server, frontPageRows) {
            focusRequester.tryRequestFocus()
        }
        HomePage(
            modifier = modifier.focusRequester(focusRequester),
            server = server,
            serverStats = serverStats,
            uiConfig = uiConfig,
            rows = frontPageRows,
            itemOnClick = itemOnClick,
            longClicker = longClicker,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomePage(
    server: StashServer,
    serverStats: StatisticsQuery.Stats?,
    uiConfig: ComposeUiConfig,
    rows: List<FrontPageParser.FrontPageRow.Success>,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    var focusedItem by remember { mutableStateOf<Any?>(null) }
    val focusRequester = remember { FocusRequester() }
    var focusedIndex by rememberSaveable { mutableStateOf(RowColumn(0, 0)) }
    var focusedRow by rememberSaveable { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    LaunchedEffect(focusedIndex) {
        listState.animateScrollToItem(focusedRow)
    }

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        focusedItem?.let { item ->
            val imageUrl =
                when (item) {
                    is SlimSceneData -> item.paths.screenshot
                    is ImageData -> item.paths.image
                    is PerformerData -> item.image_path
                    is StudioData -> item.image_path
                    is TagData -> item.image_path
                    is MarkerData -> item.screenshot
                    is GroupData -> item.front_image_path
                    is GalleryData -> item.paths.cover
                    else -> null
                }
            if (imageUrl.isNotNullOrBlank()) {
                val gradientColor = MaterialTheme.colorScheme.background
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(imageUrl)
                            .transitionFactory(CrossFadeFactory(250.milliseconds))
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopEnd,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .fillMaxHeight(.85f)
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    Brush.verticalGradient(
                                        colorStops =
                                            arrayOf(
                                                0f to Color.Transparent,
                                                .9f to gradientColor,
                                            ),
                                        startY = 0f,
                                    ),
                                )
                                drawRect(
                                    Brush.horizontalGradient(
                                        colorStops =
                                            arrayOf(
                                                0f to Color.Transparent,
                                                .8f to gradientColor,
                                            ),
                                        startX = size.width * .33f,
                                        endX = 0f,
                                    ),
                                )
//                                drawLine(
//                                    color = Color.Red,
//                                    start = Offset(x = 0f, y = size.height * .5f),
//                                    end = Offset(x = size.width, y = size.height),
//                                )
//                                drawLine(
//                                    color = Color.Red,
//                                    start = Offset.Zero,
//                                    end = Offset(x = size.width, y = size.height),
//                                )
                            },
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
        ) {
            focusedItem?.let { item ->
                MainPageHeader(
                    item = item,
                    uiConfig = uiConfig,
                    modifier = Modifier.fillMaxWidth(.7f),
                )
            }

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 72.dp),
                modifier =
                    Modifier
                        .focusGroup()
                        .focusRestorer { focusRequester },
            ) {
                itemsIndexed(rows) { index, row ->
                    HomePageRow(
                        uiConfig,
                        row,
                        itemOnClick,
                        longClicker,
                        onFocus = { idx, item ->
                            focusedIndex = RowColumn(index, idx)
                            focusedItem = item
                            focusedRow = index
                        },
                        rowFocusRequester = if (index == focusedIndex.row) focusRequester else null,
                        modifier = Modifier,
                    )
                }
                item {
                    ServerStatsRow(
                        server = server,
                        serverStats = serverStats,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun HomePageRow(
    uiConfig: ComposeUiConfig,
    row: FrontPageParser.FrontPageRow.Success,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    onFocus: (Int, Any) -> Unit,
    rowFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    val navigationManager = LocalGlobalContext.current.navigationManager
    Column(modifier = modifier) {
        ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground)) {
            Text(
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp, start = 16.dp),
                text = row.name,
            )
        }
        val firstFocus = remember { FocusRequester() }
        var focusedIndex by rememberSaveable { mutableIntStateOf(0) }
        val rowModifier =
            if (rowFocusRequester != null) Modifier.focusRequester(rowFocusRequester) else Modifier
        val server = LocalGlobalContext.current.server
        LazyRow(
            modifier =
                rowModifier
                    .onFocusChanged {
                        if (it.isFocused) {
                            firstFocus.tryRequestFocus()
                        }
                    }.focusGroup()
                    .focusRestorer(firstFocus)
                    .fillMaxWidth()
                    .onKeyEvent {
                        if (isPlayKeyUp(it)) {
                            val destination =
                                getPlayDestinationForItem(
                                    server,
                                    row.data[focusedIndex],
                                    FilterAndPosition(row.filter, focusedIndex),
                                )
                            return@onKeyEvent if (destination != null) {
                                navigationManager.navigate(destination)
                                true
                            } else {
                                false
                            }
                        }
                        return@onKeyEvent false
                    },
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(row.data) { index, item ->
                if (item != null) {
                    val cardModifier =
                        if (index == focusedIndex) {
                            Modifier.focusRequester(firstFocus)
                        } else {
                            Modifier
                        }.onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                focusedIndex = index
                                onFocus(index, item)
                            }
                        }
                    StashCard(
                        modifier = cardModifier,
                        uiConfig = uiConfig,
                        item = item,
                        itemOnClick = {
                            itemOnClick.onClick(
                                item,
                                FilterAndPosition(row.filter, index),
                            )
                        },
                        longClicker = longClicker,
                        getFilterAndPosition = { FilterAndPosition(row.filter, index) },
                    )
                }
            }
            if (row.data.isNotEmpty()) {
                item {
                    ViewAllCard(
                        modifier =
                            Modifier
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        focusedIndex = row.data.size
                                        onFocus(row.data.size, row.filter)
                                    }
                                }.ifElse(
                                    focusedIndex == row.data.size,
                                    Modifier.focusRequester(firstFocus),
                                ),
                        filter = row.filter,
                        itemOnClick = {
                            itemOnClick.onClick(
                                row.filter,
                                FilterAndPosition(row.filter, row.data.size),
                            )
                        },
                        longClicker = longClicker,
                        uiConfig = uiConfig,
                        getFilterAndPosition = { FilterAndPosition(row.filter, row.data.size) },
                    )
                }
            }
        }
    }
}

@Composable
fun ServerStatsRow(
    server: StashServer,
    serverStats: StatisticsQuery.Stats?,
    modifier: Modifier = Modifier,
) {
    serverStats?.let { stats ->
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TitleValueText(
                stringResource(R.string.stashapp_scenes),
                formatNumber(stats.scene_count, server.serverPreferences.abbreviateCounters),
            )
            TitleValueText(
                stringResource(R.string.stashapp_stats_scenes_size),
                formatBytes(stats.scenes_size.toLong()),
            )
            TitleValueText(
                stringResource(R.string.stashapp_images),
                formatNumber(stats.image_count, server.serverPreferences.abbreviateCounters),
            )
            TitleValueText(
                stringResource(R.string.stashapp_stats_image_size),
                formatBytes(stats.images_size.toLong()),
            )
            TitleValueText(
                stringResource(R.string.stashapp_stats_total_play_count),
                formatNumber(stats.total_play_count, server.serverPreferences.abbreviateCounters),
            )
            TitleValueText(
                stringResource(R.string.stashapp_stats_total_play_duration),
                stats.total_play_duration
                    .toLong()
                    .seconds
                    .toString(),
            )
            TitleValueText(
                stringResource(R.string.stashapp_stats_total_o_count),
                formatNumber(stats.total_o_count, server.serverPreferences.abbreviateCounters),
            )
        }
    }
}
