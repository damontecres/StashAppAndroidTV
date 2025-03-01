package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
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
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.cards.ViewAllCard
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getCaseInsensitive
import com.github.damontecres.stashapp.views.models.ServerViewModel
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
    cardUiSettings: ServerViewModel.CardUiSettings,
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

    val frontPageRows by viewModel.frontPageRows.observeAsState()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(server, frontPageRows) {
        focusRequester.requestFocus()
    }
    HomePage(
        modifier =
            Modifier
                .padding(16.dp)
                .focusRequester(focusRequester),
        uiConfig = uiConfig,
        rows = frontPageRows!!,
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
    val focusRequester = remember { FocusRequester() }
    var focusedIndex by rememberSaveable { mutableIntStateOf(0) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 75.dp),
        modifier =
            modifier
                .fillMaxSize()
                .focusGroup()
                .focusRestorer { focusRequester },
    ) {
        itemsIndexed(rows) { index, row ->
            HomePageRow(
                uiConfig,
                row,
                itemOnClick,
                longClicker,
                onFocus = {
                    focusedIndex = index
                },
                rowFocusRequester = if (index == focusedIndex) focusRequester else null,
                modifier = Modifier,
            )
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
    onFocus: (Int) -> Unit,
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
            items(row.data.size) { index ->
                val item = row.data[index]
                val cardModifier =
                    if (index == focusedIndex) {
                        Modifier.focusRequester(firstFocus)
                    } else {
                        Modifier
                    }.onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            focusedIndex = index
                            onFocus(index)
                        }
                    }
                if (item != null) {
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
                        filter = row.filter,
                        itemOnClick = {
                            itemOnClick.onClick(
                                row.filter,
                                FilterAndPosition(row.filter, row.data.size),
                            )
                        },
                        longClicker = longClicker,
                        getFilterAndPosition = { FilterAndPosition(row.filter, row.data.size) },
                    )
                }
            }
        }
    }
}
