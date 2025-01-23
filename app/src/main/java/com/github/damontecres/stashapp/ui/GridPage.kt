package com.github.damontecres.stashapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StashGrid(
    filterArgs: FilterArgs,
    itemOnClick: (Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val server = StashServer.requireCurrentServer()
    val dataSupplierFactory = DataSupplierFactory(server.version)
    val dataSupplier =
        dataSupplierFactory.create<Query.Data, StashData, Query.Data>(filterArgs)
    val pagingSource = StashPagingSource(QueryEngine(server), dataSupplier) { _, _, item -> item }
    val pager = remember { ComposePager(pagingSource, scope) }

    LaunchedEffect(filterArgs) {
        pager.init()
    }

    val firstFocus = remember { FocusRequester() }
    var focusedIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .focusGroup()
                    .focusRestorer { firstFocus },
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
                        if (index == focusedIndex) {
                            Modifier.focusRequester(firstFocus)
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
                text = "$focusedIndex / ${pager.size()}",
            )
        }
    }
}
