package com.github.damontecres.stashapp.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FilterViewModel
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.filterArgsSaver
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.OneTimeLaunchedEffect
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TabPage(
    name: AnnotatedString,
    tabs: List<TabProvider>,
    dataType: DataType,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val rememberTab =
        preferences.getBoolean(context.getString(R.string.pref_key_ui_remember_tab), false)
    val rememberTabKey = context.getString(R.string.pref_key_ui_remember_tab) + ".${dataType.name}"
    val rememberedTabIndex = if (rememberTab) preferences.getInt(rememberTabKey, 0) else 0

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(rememberedTabIndex) }
    val tabRowFocusRequester = remember { FocusRequester() }
    var showTabRowRaw by rememberSaveable { mutableStateOf(true) }
    val showTabRow by remember { derivedStateOf { showTabRowRaw } }
    val focusRequesters = remember { List(tabs.size) { FocusRequester() } }

    var resolvedTabIndex by remember { mutableIntStateOf(selectedTabIndex) }
    LaunchedEffect(selectedTabIndex) {
        // Add a slight delay so if scrolling quickly through tabs, can skip rending the skipped tabs
        delay(200.milliseconds)
        resolvedTabIndex = selectedTabIndex
        if (rememberTab) {
            preferences.edit { putInt(rememberTabKey, resolvedTabIndex) }
        }
    }

    OneTimeLaunchedEffect {
        tabRowFocusRequester.tryRequestFocus()
    }
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineLarge,
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally),
        )
        AnimatedVisibility(
            showTabRow,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier =
                    Modifier
                        .focusRestorer(focusRequesters[selectedTabIndex])
                        .focusRequester(tabRowFocusRequester),
            ) {
                tabs.forEachIndexed { index, tab ->
                    key(index) {
                        Tab(
                            selected = index == selectedTabIndex,
                            onFocus = { selectedTabIndex = index },
                            modifier =
                                Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .focusRequester(focusRequesters[index]),
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
//            Log.i("Tabs", "resolvedTabIndex=$resolvedTabIndex")
            tabs[resolvedTabIndex].content(this) { columns, position ->
                showTabRowRaw = position < columns
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
    composeUiConfig: ComposeUiConfig,
): (initialFilter: FilterArgs) -> TabProvider =
    { initialFilter ->
        val name =
            StashApplication.getApplication().getString(initialFilter.dataType.pluralStringId)
        TabProvider(name) { positionCallback ->
            var filter by rememberSaveable(name, saver = filterArgsSaver) {
                mutableStateOf(
                    initialFilter,
                )
            }
            StashGridTab(
                name = name,
                server = server,
                initialFilter = filter,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                modifier = Modifier,
                positionCallback = positionCallback,
                composeUiConfig = composeUiConfig,
                onFilterChange = {
                    filter = it
                },
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
    composeUiConfig: ComposeUiConfig,
    onFilterChange: (FilterArgs) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilterViewModel = viewModel(key = name),
    positionCallback: ((columns: Int, position: Int) -> Unit)? = null,
    subToggleLabel: String? = null,
    onSubToggleCheck: ((Boolean) -> Unit)? = null,
    subToggleChecked: Boolean = false,
) {
    val navigationManager = LocalGlobalContext.current.navigationManager
    LaunchedEffect(server, initialFilter) {
        viewModel.setFilter(server, initialFilter)
    }
    val pager by viewModel.pager.observeAsState()
    pager?.let { newPager ->
        StashGridControls(
            server = server,
            pager = newPager,
            initialPosition = -1,
            itemOnClick = itemOnClick,
            longClicker = longClicker,
            filterUiMode = FilterUiMode.CREATE_FILTER,
            createFilter = {
                navigationManager.navigate(
                    Destination.CreateFilter(
                        dataType = newPager.filter.dataType,
                        startingFilter = newPager.filter,
                    ),
                )
            },
            modifier = modifier,
            positionCallback = positionCallback,
            uiConfig = composeUiConfig,
            updateFilter = { onFilterChange?.invoke(it) },
            letterPosition = viewModel::findLetterPosition,
            subToggleLabel = subToggleLabel,
            onSubToggleCheck = onSubToggleCheck,
            subToggleChecked = subToggleChecked,
            requestFocus = false,
        )
    }
}

fun tabFindFilter(
    server: StashServer,
    pageFilterKey: PageFilterKey,
): StashFindFilter? =
    server.serverPreferences
        .getDefaultPageFilter(pageFilterKey)
        .findFilter
        ?.withResolvedRandom()
