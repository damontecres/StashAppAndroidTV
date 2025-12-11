package com.github.damontecres.stashapp.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FilterViewModel
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.CreateFilter
import com.github.damontecres.stashapp.ui.components.FilterUiMode
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.StashGridControls
import com.github.damontecres.stashapp.util.StashServer

@Composable
fun FilterPage(
    server: StashServer,
    navigationManager: NavigationManagerCompose,
    initialFilter: FilterArgs,
    scrollToNextPage: Boolean,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    onUpdateTitle: ((AnnotatedString) -> Unit)? = null,
    viewModel: FilterViewModel = viewModel(),
) {
    if (viewModel.currentFilter == null) {
        // If the view model is populated, don't do it again
        LaunchedEffect(server, initialFilter) {
            viewModel.setFilter(server, initialFilter, uiConfig.cardSettings.columns)
        }
    }
    val pager by viewModel.pager.observeAsState()

    val initialPosition =
        if (scrollToNextPage) {
            uiConfig.preferences.searchPreferences.maxResults
        } else {
            0
        }
    Column(
        modifier = modifier,
    ) {
        val title = pager?.filter?.name ?: stringResource(initialFilter.dataType.pluralStringId)
        if (onUpdateTitle == null) {
            ProvideTextStyle(MaterialTheme.typography.displayMedium) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            LaunchedEffect(title) { onUpdateTitle.invoke(AnnotatedString(title)) }
        }
        if (pager != null) {
            StashGridControls(
                modifier = Modifier,
                uiConfig = uiConfig,
                server = server,
                pager = pager!!,
                filterUiMode = FilterUiMode.SAVED_FILTERS,
                createFilter = {
                    val dataType = initialFilter.dataType
                    val currentFilter = viewModel.currentFilter
                    when (it) {
                        CreateFilter.FROM_CURRENT -> {
                            navigationManager.navigate(
                                Destination.CreateFilter(
                                    dataType,
                                    currentFilter,
                                ),
                            )
                        }

                        CreateFilter.NEW_FILTER -> {
                            navigationManager.navigate(
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
                updateFilter = {
                    viewModel.setFilter(server, it, uiConfig.cardSettings.columns)
                },
                letterPosition = viewModel::findLetterPosition,
                requestFocus = true,
            )
        } else {
            CircularProgress()
        }
    }
}
