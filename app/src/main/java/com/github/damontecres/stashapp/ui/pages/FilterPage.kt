package com.github.damontecres.stashapp.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.FilterUiMode
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.StashGridControls

@Composable
fun FilterPage(
    filterArgs: FilterArgs,
    scrollToNextPage: Boolean,
    uiConfig: ComposeUiConfig,
    itemOnClick: (Any) -> Unit,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        ProvideTextStyle(MaterialTheme.typography.displayMedium) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = filterArgs.name ?: stringResource(filterArgs.dataType.pluralStringId),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        StashGridControls(
            modifier = Modifier.padding(16.dp),
            uiConfig = uiConfig,
            initialFilter = filterArgs,
            filterUiMode = FilterUiMode.SAVED_FILTERS,
            itemOnClick = itemOnClick,
            longClicker = longClicker,
            // TODO scrollToNextPage = scrollToNextPage,
        )
    }
}
