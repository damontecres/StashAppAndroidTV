package com.github.damontecres.stashapp.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import com.github.damontecres.stashapp.ui.components.StashGridControls
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.DataLoadingState
import com.github.damontecres.stashapp.util.ComposePager
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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

    when (val st = state.pager) {
        is DataLoadingState.Error -> {
            ErrorMessage(st, modifier)
        }

        DataLoadingState.Loading,
        DataLoadingState.Pending,
        -> {
            LoadingPage(modifier)
        }

        is DataLoadingState.Success<ComposePager<StashData>> -> {
            val pager = st.data
            val initialPosition =
                if (scrollToNextPage) {
                    uiConfig.preferences.searchPreferences.maxResults
                } else {
                    0
                }
            Column(
                modifier = modifier,
            ) {
                val gridFocusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    gridFocusRequester.tryRequestFocus()
                }
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
                    modifier = Modifier,
                    uiConfig = uiConfig,
                    pager = pager,
                    filterUiMode = FilterUiMode.SAVED_FILTERS,
                    createFilter = {
                        val dataType = initialFilter.dataType
                        when (it) {
                            CreateFilter.FROM_CURRENT -> {
                                viewModel.navigationManager.navigate(
                                    Destination.CreateFilter(
                                        dataType,
                                        pager.filter,
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
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    initialPosition = initialPosition,
                    updateFilter = viewModel::updateFilter,
                    letterPosition = viewModel::findLetterPosition,
                    gridFocusRequester = gridFocusRequester,
                )
            }
        }
    }
}
