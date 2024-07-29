package com.github.damontecres.stashapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.StashPagingSource

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TabbedFilterGrid(
    name: String,
    tabs: List<String>,
    contentProvider: (Int) -> StashPagingSource<out Query.Data, Any, out Query.Data>,
    itemOnClick: (Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    Column(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        ProvideTextStyle(MaterialTheme.typography.headlineLarge) {
            Text(
                text = name,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally),
            )
        }
        // https://developer.android.com/reference/kotlin/androidx/tv/material3/package-summary#TabRow(kotlin.Int,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,kotlin.Function0,kotlin.Function2,kotlin.Function1)
        // TODO center tabs?
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .focusRestorer(),
        ) {
            tabs.forEachIndexed { index, tab ->
                key(index) {
                    Tab(
                        selected = index == selectedTabIndex,
                        onFocus = { selectedTabIndex = index },
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally),
                    ) {
                        ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                            Text(
                                text = tab,
                                modifier =
                                    Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp,
                                    ),
                            )
                        }
                    }
                }
            }
        }
        val resolvedFilter =
            ResolvedFilterState.Success(
                ResolvedFilter(DataType.SCENE),
                contentProvider(selectedTabIndex),
            )
        ResolvedFilterGrid(
            resolvedFilter,
            showHeader = false,
            itemOnClick = itemOnClick,
            contentPadding = PaddingValues(top = 16.dp),
            modifier = modifier,
        )
    }
}
