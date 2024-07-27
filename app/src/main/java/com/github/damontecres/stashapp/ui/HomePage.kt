package com.github.damontecres.stashapp.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.ServerInfoQuery
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.QueryRepository
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.getCaseInsensitive
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomePageViewModel
    @Inject
    constructor(
        private val queryRepository: QueryRepository,
    ) : ViewModel() {
        val rows = mutableStateListOf<FrontPageParser.FrontPageRow>()

        suspend fun fetchFrontPage() {
            val config = queryRepository.getServerConfiguration()
            if (config != null) {
                val version =
                    Version.tryFromString(config.version.version) ?: Version.MINIMUM_STASH_VERSION
                val ui = config.configuration.ui

                // TODO A little hacky
                ServerPreferences(StashApplication.getApplication()).updatePreferences(
                    config.configuration,
                    ServerInfoQuery.Data(
                        ServerInfoQuery.Version(config.version.version),
                        ServerInfoQuery.FindScenes(-1),
                    ),
                )

                val frontPageContent =
                    (ui as Map<String, *>).getCaseInsensitive("frontPageContent") as List<Map<String, *>>
                val frontPageParser =
                    FrontPageParser(
                        QueryEngine(StashApplication.getApplication()),
                        FilterParser(version),
                    )
                frontPageParser.parse(frontPageContent).forEach { deferredRow ->
                    val result = deferredRow.await()
                    if (result.successful) {
                        rows.add(result)
                    }
                }
            }
        }
    }

@Suppress("ktlint:standard:function-naming")
@Composable
fun HomePage() {
    val viewModel = hiltViewModel<HomePageViewModel>()

    val rows = remember { viewModel.rows }

    LaunchedEffect(Unit) {
        viewModel.fetchFrontPage()
    }

    TvLazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .padding(12.dp),
    ) {
        items(rows, key = { rows.indexOf(it) }) { row ->
            HomePageRow(row)
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun HomePageRow(row: FrontPageParser.FrontPageRow) {
    val rowData = row.data!!
    Text(text = rowData.name, modifier = Modifier.padding(top = 20.dp, bottom = 20.dp))
    TvLazyRow(
        modifier = Modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(rowData.data) { item ->
            if (item != null) {
                StashCard(item)
            }
        }
    }
}
