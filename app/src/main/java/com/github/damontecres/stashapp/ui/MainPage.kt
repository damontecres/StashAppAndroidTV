package com.github.damontecres.stashapp.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.cards.ViewAllCard
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.StashServer

@Composable
fun HomePage(
    uiConfig: ComposeUiConfig,
    rows: List<FrontPageParser.FrontPageRow.Success>,
    itemOnClick: (Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    setSingletonImageLoaderFactory { context ->
        ImageLoader
            .Builder(context)
            .crossfade(true)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            StashServer.requireCurrentServer().okHttpClient
                        },
                    ),
                )
            }.build()
    }

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
            HomePageRow(uiConfig, row, itemOnClick)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomePageRow(
    uiConfig: ComposeUiConfig,
    row: FrontPageParser.FrontPageRow.Success,
    itemOnClick: (Any) -> Unit,
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
                    StashCard(uiConfig, item, itemOnClick, cardModifier)
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
