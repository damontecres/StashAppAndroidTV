package com.github.damontecres.stashapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer

@Composable
fun StashGrid(
    filterArgs: FilterArgs,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val server = StashServer.requireCurrentServer()
    val dataSupplierFactory = DataSupplierFactory(server.version)
    val dataSupplier =
        dataSupplierFactory.create<Query.Data, StashData, Query.Data>(filterArgs)
    val pagingSource = StashPagingSource(QueryEngine(server), dataSupplier) { _, _, item -> item }
    val pager = ComposePager(pagingSource, scope)

    LaunchedEffect(filterArgs) {
        pager.init()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            modifier
                .padding(16.dp)
                .fillMaxSize(),
    ) {
        if (pager.size() < 0) {
            item {
                Text(
                    text = "Waiting for items to load from the backend",
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
                val item = pager[index]
                if (item == null) {
                    Text(
                        text = "Loading...",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally),
                    )
                } else {
                    StashCard(
                        ComposeUiConfig(true),
                        item = item,
                        itemOnClick = { _ -> },
                    )
                }
            }
        }
    }
}
