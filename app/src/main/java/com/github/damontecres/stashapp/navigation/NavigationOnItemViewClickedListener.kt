package com.github.damontecres.stashapp.navigation

import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.github.damontecres.stashapp.data.StashData

class NavigationOnItemViewClickedListener(
    private val navigationManager: NavigationManager,
) : OnItemViewClickedListener {
    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?,
    ) {
        when (item) {
            is StashData -> navigationManager.navigate(Destination.fromStashData(item))
            else -> TODO()
        }
    }
}
