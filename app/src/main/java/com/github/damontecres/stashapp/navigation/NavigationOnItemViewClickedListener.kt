package com.github.damontecres.stashapp.navigation

import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.suppliers.FilterArgs

class NavigationOnItemViewClickedListener(
    private val navigationManager: NavigationManager,
    private val imageFilterLookup: ((item: ImageData) -> FilterAndPosition?)? = null,
) : OnItemViewClickedListener {
    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?,
    ) {
        when (item) {
            is StashData -> {
                if (imageFilterLookup != null && item is ImageData) {
                    val filterAndPosition = imageFilterLookup.invoke(item)
                    if (filterAndPosition != null) {
                        navigationManager.navigate(
                            Destination.Slideshow(
                                filterAndPosition.filter,
                                filterAndPosition.position,
                                false,
                            ),
                        )
                    } else {
                        TODO()
                    }
                } else {
                    navigationManager.navigate(Destination.fromStashData(item))
                }
            }

            is FilterArgs -> navigationManager.navigate(Destination.Filter(item, true))
            else -> TODO()
        }
    }

    data class FilterAndPosition(
        val filter: FilterArgs,
        val position: Int,
    )
}
