package com.github.damontecres.stashapp.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FilterViewModel
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.StashServer

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TabPage(
    name: AnnotatedString,
    tabs: List<TabProvider>,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabRowFocusRequester = remember { FocusRequester() }
    var showTabRowRaw by remember { mutableStateOf(true) }
    val showTabRow by remember { derivedStateOf { showTabRowRaw } }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        ProvideTextStyle(MaterialTheme.typography.headlineLarge) {
            Text(
                text = name,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally),
            )
        }
        if (showTabRow) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
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
                                    text = tab.name,
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
        }
        if (tabs.isNotEmpty()) {
            Log.i("Tabs", "selectedTabIndex=$selectedTabIndex")
            tabs[selectedTabIndex].content(this) { columns, position ->
                showTabRowRaw = position <= columns
            }
        }
    }
}

data class TabProvider(
    val name: String,
    val content: @Composable ColumnScope.(
        /**
         * Callback when grid position changes, passed to [StashGrid]. None-StashGrid can probably ignore this
         */
        positionCallback: (columns: Int, position: Int) -> Unit,
    ) -> Unit,
)

fun createTabFunc(
    server: StashServer,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
): (initialFilter: FilterArgs) -> TabProvider =
    { initialFilter ->
        val name =
            StashApplication.getApplication().getString(initialFilter.dataType.pluralStringId)
        TabProvider(name) { positionCallback ->
            StashGridTab(
                name = name,
                server = server,
                initialFilter = initialFilter,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                modifier = Modifier,
                positionCallback = positionCallback,
            )
        }
    }

@Composable
fun StashGridTab(
    name: String,
    server: StashServer,
    initialFilter: FilterArgs,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    positionCallback: ((columns: Int, position: Int) -> Unit)? = null,
) {
    val viewModel = viewModel<FilterViewModel>(key = name)
    LaunchedEffect(server, initialFilter) {
        viewModel.setFilter(server, initialFilter)
    }
    val pager by viewModel.pager.observeAsState()
    pager?.let { newPager ->
        StashGridControls(
            server = server,
            pager = newPager,
            itemOnClick = itemOnClick,
            longClicker = longClicker,
            filterUiMode = FilterUiMode.CREATE_FILTER,
            modifier = modifier,
            positionCallback = positionCallback,
            uiConfig = ComposeUiConfig.fromStashServer(server),
            updateFilter = { viewModel.setFilter(server, it) },
            letterPosition = viewModel::findLetterPosition,
        )
    }
}

fun tabFindFilter(
    server: StashServer,
    pageFilterKey: PageFilterKey,
): StashFindFilter? = server.serverPreferences.getDefaultPageFilter(pageFilterKey).findFilter
