package com.github.damontecres.stashapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.QueryRepository
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.getCaseInsensitive
import com.github.damontecres.stashapp.util.titleOrFilename
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomePageViewModel
    @Inject
    constructor(
        val queryRepository: QueryRepository,
    ) : ViewModel() {
        val rows = mutableStateListOf<FrontPageParser.FrontPageRow>()

        suspend fun fetchFrontPage() {
            val config = queryRepository.getServerConfiguration()
            if (config != null) {
                val version =
                    Version.tryFromString(config.version.version) ?: Version.MINIMUM_STASH_VERSION
                val ui = config.configuration.ui
                val frontPageContent =
                    (ui as Map<String, *>).getCaseInsensitive("frontPageContent") as List<Map<String, *>>
                val frontPageParser =
                    FrontPageParser(
                        QueryEngine(StashApplication.getApplication()),
                        FilterParser(version),
                    )
                frontPageParser.parse(frontPageContent).forEach { deferredRow ->
                    val result = deferredRow.await()
                    if (result.successful && result.data?.filter?.dataType == DataType.SCENE) {
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

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxHeight().padding(12.dp),
    ) {
        items(rows, key = { rows.indexOf(it) }) { row ->
            HomePageRow(row)
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun HomePageRow(row: FrontPageParser.FrontPageRow) {
    val context = LocalContext.current
    LazyRow {
        items(row.data!!.data) { item ->
            if (item is SlimSceneData) {
                SceneCard(item) {
                    Toast.makeText(context, "Clicked ${item.titleOrFilename}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}
