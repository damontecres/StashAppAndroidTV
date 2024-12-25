package com.github.damontecres.stashapp.navigation

import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.playback.PlaybackMode
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
        val destination =
            when (item) {
                is MarkerData ->
                    Destination.Playback(
                        item.scene.videoSceneData.id,
                        (item.seconds + 1000L).toLong(),
                        PlaybackMode.CHOOSE,
                    )

                is ImageData -> {
                    val filterAndPosition = imageFilterLookup!!.invoke(item)
                    if (filterAndPosition != null) {
                        Destination.Slideshow(
                            filterAndPosition.filter,
                            filterAndPosition.position,
                            false,
                        )
                    } else {
                        TODO()
                    }
                }

                is StashData -> Destination.fromStashData(item)

                is FilterArgs -> Destination.Filter(item, true)

                else -> TODO()
            }
        navigationManager.navigate(destination)
    }
}
