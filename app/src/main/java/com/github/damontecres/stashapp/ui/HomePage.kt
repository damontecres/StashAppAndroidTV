package com.github.damontecres.stashapp.ui

import android.content.Context
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.cards.ViewAllCard
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.QueryRepository
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.getCaseInsensitive
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class HomePageViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val queryRepository: QueryRepository,
        private val stashServer: StashServer,
    ) : ViewModel() {
        private val _rows = mutableStateListOf<FrontPageParser.FrontPageRow.Success>()
        val rows: SnapshotStateList<FrontPageParser.FrontPageRow.Success> get() = _rows

        suspend fun fetchFrontPage() {
            val config = queryRepository.getServerConfiguration()
            if (config != null) {
                val version =
                    Version.tryFromString(config.version.version) ?: Version.MINIMUM_STASH_VERSION
                val ui = config.configuration.ui

                stashServer.serverPreferences.updatePreferences(config)

                val frontPageContent =
                    (ui as Map<String, *>).getCaseInsensitive("frontPageContent") as List<Map<String, *>>
                val frontPageParser =
                    FrontPageParser(
                        context,
                        QueryEngine(stashServer),
                        FilterParser(stashServer.version),
                    )
                frontPageParser.parse(frontPageContent).forEach { deferredRow ->
                    val result = deferredRow.await()
                    if (result is FrontPageParser.FrontPageRow.Success) {
                        _rows.add(result)
                    }
                }
            }
        }
    }

@Suppress("ktlint:standard:function-naming")
@Composable
fun HomePage(itemOnClick: (Any) -> Unit) {
    val viewModel = hiltViewModel<HomePageViewModel>()

    val rows = remember { viewModel.rows }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.fetchFrontPage()
    }

    TvLazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 75.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .focusGroup()
                .focusRequester(focusRequester),
    ) {
        items(rows, key = { rows.indexOf(it) }) { row ->
            HomePageRow(row, itemOnClick)
        }
    }
//    if (rows.isNotEmpty()) {
//        focusRequester.requestFocus()
//    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun HomePageRow(
    row: FrontPageParser.FrontPageRow.Success,
    itemOnClick: (Any) -> Unit,
) {
    Column(modifier = Modifier) {
        ProvideTextStyle(MaterialTheme.typography.titleLarge) {
            Text(
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp, start = 16.dp),
                text = row.name,
            )
        }
        TvLazyRow(
            modifier =
                Modifier
                    .focusGroup()
                    .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(row.data) { item ->
                if (item != null) {
                    StashCard(item, itemOnClick)
                }
            }
            if (row.data.isNotEmpty()) {
                item {
                    ViewAllCard(filter = row.filter, itemOnClick)
                }
            }
        }
    }
}
