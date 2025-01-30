package com.github.damontecres.stashapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TabPage(
    name: AnnotatedString,
    tabs: List<TabProvider>,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
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
