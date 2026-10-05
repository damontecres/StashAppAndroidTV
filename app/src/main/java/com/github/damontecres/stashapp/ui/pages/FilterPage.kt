package com.github.damontecres.stashapp.ui.pages

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FilterViewModel
import com.github.damontecres.stashapp.ui.compat.isTvDevice
import com.github.damontecres.stashapp.ui.components.CreateFilter
import com.github.damontecres.stashapp.ui.components.ErrorMessage
import com.github.damontecres.stashapp.ui.components.FilterUiMode
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LoadingPage
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.StashGrid
import com.github.damontecres.stashapp.ui.components.StashGridControls
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.DataLoadingState
import com.github.damontecres.stashapp.util.ComposePager
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FilterPage(
    initialFilter: FilterArgs,
    scrollToNextPage: Boolean,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    viewModel: FilterViewModel =
        koinViewModel {
            parametersOf(initialFilter)
        },
) {
    val state by viewModel.state.collectAsState()

    val searchInteractionSource = remember { MutableInteractionSource() }
    val searchIsFocused by searchInteractionSource.collectIsFocusedAsState()
    val gridFocusRequester = remember { FocusRequester() }
    val rowFocusRequester = remember { FocusRequester() }

    var showTopRow by rememberSaveable { mutableStateOf(!scrollToNextPage) }
    var startPosition by
        remember {
            mutableIntStateOf(
                if (scrollToNextPage) {
                    uiConfig.preferences.searchPreferences.maxResults
                } else {
                    0
                },
            )
        }

    Column(
        modifier = modifier,
    ) {
        if (isTvDevice) {
            val interfaceState by viewModel.interfaceState.collectAsState()
            interfaceState.title?.let { title ->
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
            }
        }

        StashGridControls(
            modifier = Modifier.focusRequester(rowFocusRequester),
            uiConfig = uiConfig,
            filterArgs = viewModel.filter,
            filterUiMode = FilterUiMode.SAVED_FILTERS,
            createFilter = {
                val dataType = initialFilter.dataType
                when (it) {
                    CreateFilter.FROM_CURRENT -> {
                        viewModel.navigationManager.navigate(
                            Destination.CreateFilter(
                                dataType,
                                viewModel.filter,
                            ),
                        )
                    }

                    CreateFilter.NEW_FILTER -> {
                        viewModel.navigationManager.navigate(
                            Destination.CreateFilter(
                                dataType,
                                null,
                            ),
                        )
                    }
                }
            },
            updateFilter = viewModel::updateFilter,
            gridFocusRequester = gridFocusRequester,
            searchInteractionSource = searchInteractionSource,
            showTopRow = showTopRow,
        )

        when (val st = state.pager) {
            is DataLoadingState.Error -> {
                ErrorMessage(st, Modifier)
            }

            DataLoadingState.Loading,
            DataLoadingState.Pending,
            -> {
                LoadingPage(
                    focusEnabled = !searchIsFocused,
                    modifier = Modifier,
                )
            }

            is DataLoadingState.Success<ComposePager<StashData>> -> {
                LaunchedEffect(Unit) {
                    if (!searchIsFocused) {
                        gridFocusRequester.tryRequestFocus("grid")
                    }
                    startPosition = 0
                }

                StashGrid(
                    pager = st.data,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    letterPosition = viewModel::findLetterPosition,
                    initialPosition = startPosition,
                    positionCallback = { columns, position ->
                        showTopRow = position < columns
//                        positionCallback?.invoke(columns, position)
                    },
                    gridFocusRequester = gridFocusRequester,
                    cardContext = null,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .focusProperties {
                                onExit = {
                                    if (requestedFocusDirection == FocusDirection.Up) {
                                        rowFocusRequester.tryRequestFocus("onExit")
                                    } else {
                                        FocusRequester.Default.tryRequestFocus("onExit2")
                                    }
                                }
                            },
                )
            }
        }
    }
}
