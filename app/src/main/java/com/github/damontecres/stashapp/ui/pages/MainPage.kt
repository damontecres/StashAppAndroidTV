package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.cards.ViewAllCard
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.main.MainPageSceneDetails
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getCaseInsensitive
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.launch

private const val TAG = "MainPage"

class MainPageViewModel(
    server: StashServer,
) : ViewModel() {
    val frontPageRows = MutableLiveData<List<FrontPageParser.FrontPageRow.Success>>(listOf())

    init {
        val queryEngine = QueryEngine(server)
        val filterParser = FilterParser(server.version)
        val frontPageContent =
            server.serverPreferences.uiConfiguration?.getCaseInsensitive("frontPageContent") as List<Map<String, *>>?
        if (frontPageContent != null) {
            Log.d(TAG, "${frontPageContent.size} front page rows")
            val pageSize = 25 // TODO
            val frontPageParser =
                FrontPageParser(
                    StashApplication.getApplication(),
                    queryEngine,
                    filterParser,
                    pageSize,
                )
            viewModelScope.launch {
                val jobs = frontPageParser.parse(frontPageContent)
                val rows =
                    jobs.mapIndexedNotNull { index, job ->
                        job.await().let { row ->
                            if (row is FrontPageParser.FrontPageRow.Success) {
                                row
                            } else {
                                null
                            }
                        }
                    }
                frontPageRows.value = rows
            }
        }
    }

    companion object {
        val SERVER_KEY = object : CreationExtras.Key<StashServer> {}
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val server = this[SERVER_KEY]!!
                    MainPageViewModel(server)
                }
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
) {
    val viewModel =
        ViewModelProvider.create(
            LocalViewModelStoreOwner.current!!,
            MainPageViewModel.Factory,
            MutableCreationExtras().apply {
                set(MainPageViewModel.SERVER_KEY, server)
            },
        )[MainPageViewModel::class]

    val frontPageRows by viewModel.frontPageRows.observeAsState(listOf())

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(server, frontPageRows) {
        focusRequester.requestFocus()
    }
    HomePage(
        modifier =
            Modifier
                .focusRequester(focusRequester),
        uiConfig = uiConfig,
        rows = frontPageRows,
        itemOnClick = itemOnClick,
        longClicker = longClicker,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomePage(
    uiConfig: ComposeUiConfig,
    rows: List<FrontPageParser.FrontPageRow.Success>,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    var focusedItem by remember { mutableStateOf<Any?>(null) }
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
                            .crossfade(true)
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
                    .padding(16.dp),
        ) {
            focusedItem?.let { item ->
                val datatype =
                    if (item is StashData) {
                        Destination.getDataType(item)
                    } else if (item is FilterArgs) {
                        item.dataType
                    } else {
                        null
                    }
                val visible =
                    when (datatype) {
                        DataType.SCENE -> true
                        else -> false
                    }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(.6f)
                                .padding(bottom = 8.dp),
                    ) {
                        if (item is SlimSceneData) {
                            MainPageSceneDetails(
                                scene = item,
                                uiConfig = uiConfig,
                                modifier =
                                    Modifier
                                        .height(200.dp)
                                        .fillMaxWidth(),
                            )
                        } else if (item is FilterArgs && datatype == DataType.SCENE) {
                            Box(Modifier.height(200.dp)) {
                                Text(
                                    modifier = Modifier.enableMarquee(true),
                                    text =
                                        item.name
                                            ?: stringResource(item.dataType.pluralStringId),
                                    color = Color.LightGray,
                                    style =
                                        MaterialTheme.typography.displayMedium.copy(
                                            shadow =
                                                Shadow(
                                                    color = Color.DarkGray,
                                                    offset = Offset(5f, 2f),
                                                    blurRadius = 2f,
                                                ),
                                        ),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
            val focusRequester = remember { FocusRequester() }
            var focusedIndex by rememberSaveable { mutableIntStateOf(0) }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 75.dp),
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
                            focusedIndex = index
                            focusedItem = item
                        },
                        rowFocusRequester = if (index == focusedIndex) focusRequester else null,
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
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
        LazyRow(
            modifier =
                rowModifier
                    .onFocusChanged {
                        if (it.isFocused) {
                            firstFocus.requestFocus()
                        }
                    }.focusGroup()
                    .focusRestorer { firstFocus }
                    .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp),
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
                            Modifier.onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    focusedIndex = row.data.size
                                    onFocus(row.data.size, row.filter)
                                }
                            },
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
