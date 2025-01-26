package com.github.damontecres.stashapp.ui

import androidx.compose.ui.Modifier
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.components.FilterUiMode
import com.github.damontecres.stashapp.ui.components.StashGridControls

abstract class ComposeTabFragment : ComposeFragment() {
    protected val itemOnClick = { item: Any ->
        serverViewModel.navigationManager.navigate(
            Destination.fromStashData(
                item as StashData,
            ),
        )
    }

    fun createTab(
        dataType: DataType,
        initialFilter: FilterArgs,
    ): TabProvider =
        TabProvider(getString(dataType.pluralStringId)) { positionCallback ->
            StashGridControls(
                initialFilter = initialFilter,
                itemOnClick = itemOnClick,
                filterUiMode = FilterUiMode.CREATE_FILTER,
                modifier = Modifier,
                positionCallback = positionCallback,
            )
        }
}
