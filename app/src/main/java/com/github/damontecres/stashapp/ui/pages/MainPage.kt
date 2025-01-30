package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.cards.ViewAllCard
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getCaseInsensitive
import com.github.damontecres.stashapp.views.models.ServerViewModel

private const val TAG = "MainPage"

@Composable
fun MainPage(
    server: StashServer,
    uiConfig: ComposeUiConfig,
    cardUiSettings: ServerViewModel.CardUiSettings,
    itemOnClick: (Any) -> Unit,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    val frontPageRows =
        remember {
            mutableStateListOf<FrontPageParser.FrontPageRow.Success>()
        }
    var showPopup by remember { mutableStateOf(false) }
    var itemLongClicked by remember { mutableStateOf<Any?>(null) }

    val queryEngine = QueryEngine(server)
    val filterParser = FilterParser(server.version)
    val frontPageContent =
        server.serverPreferences.uiConfiguration?.getCaseInsensitive("frontPageContent") as List<Map<String, *>>?
    if (frontPageContent != null) {
        Log.d(TAG, "${frontPageContent.size} front page rows")
        val pageSize = cardUiSettings.maxSearchResults
        val frontPageParser =
            FrontPageParser(
                LocalContext.current,
                queryEngine,
                filterParser,
                pageSize,
            )
        LaunchedEffect(server) {
            val jobs = frontPageParser.parse(frontPageContent)
            jobs.mapIndexedNotNull { index, job ->
                job.await().let { row ->
                    if (row is FrontPageParser.FrontPageRow.Success) {
                        frontPageRows.add(row)
                    } else {
                        null
                    }
                }
            }
        }
    }

    if (!showPopup) {
        HomePage(
            modifier = Modifier.padding(16.dp),
            uiConfig = uiConfig,
            rows = frontPageRows,
            itemOnClick = itemOnClick,
            longClicker = longClicker,
        )
    } else {
        BackHandler {
            showPopup = false
        }
        Column(
            modifier =
                Modifier
                    .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                Text(
                    text = extractTitle(itemLongClicked!! as StashData) ?: "",
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally),
                )
            }
            longClicker.getPopUpItems(itemLongClicked!!).forEach {
                ListItem(
                    modifier =
                        Modifier
                            .wrapContentWidth()
                            .align(Alignment.CenterHorizontally),
                    selected = false,
                    onClick = {
                        longClicker.onItemLongClick(itemLongClicked!!, it)
                    },
                    headlineContent = { Text(it.text) },
                )
            }
        }
    }
}

@Composable
fun HomePage(
    uiConfig: ComposeUiConfig,
    rows: List<FrontPageParser.FrontPageRow.Success>,
    itemOnClick: (Any) -> Unit,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 75.dp),
        modifier =
            modifier
                .fillMaxSize()
                .focusGroup()
                .focusRequester(focusRequester),
    ) {
        items(rows) { row ->
            HomePageRow(uiConfig, row, itemOnClick, longClicker)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomePageRow(
    uiConfig: ComposeUiConfig,
    row: FrontPageParser.FrontPageRow.Success,
    itemOnClick: (Any) -> Unit,
    longClicker: LongClicker<Any>,
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
        LazyRow(
            modifier =
                Modifier
                    .focusGroup()
                    .focusRestorer { firstFocus }
                    .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(row.data) { item ->
                val cardModifier =
                    if (item == row.data[0]) {
                        Modifier.focusRequester(firstFocus)
                    } else {
                        Modifier
                    }
                if (item != null) {
                    StashCard(uiConfig, item, itemOnClick, longClicker, cardModifier)
                }
            }
            if (row.data.isNotEmpty()) {
                item {
                    ViewAllCard(filter = row.filter, itemOnClick, longClicker)
                }
            }
        }
    }
}
