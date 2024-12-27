package com.github.damontecres.stashapp.navigation

import android.util.Log
import android.widget.Toast
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.suppliers.FilterArgs

/**
 * An [OnItemViewClickedListener] for clicking on [StashData] and [FilterArgs] to navigating to their page
 *
 * @param navigationManager the manager
 * @param imageFilterLookup get the filter and position of the current image clicked for slideshow
 */
class NavigationOnItemViewClickedListener(
    private val navigationManager: NavigationManager,
    private val imageFilterLookup: ((item: ImageData) -> FilterAndPosition?)? = null,
) : OnItemViewClickedListener {
    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?,
    ) {
        val destination =
            when (item) {
                is MarkerData ->
                    Destination.Playback(
                        item.scene.videoSceneData.id,
                        (item.seconds * 1000L).toLong(),
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
                        throw IllegalStateException("imageFilterLookup is null")
                    }
                }

                is StashData -> Destination.fromStashData(item)

                is FilterArgs -> Destination.Filter(item, true)

                else -> null
            }
        if (destination != null) {
            navigationManager.navigate(destination)
        } else {
            val itemInfo = if (item == null) "null" else item::class.java.name
            Log.w(TAG, "Unsupported item: $itemInfo")
            Toast
                .makeText(
                    StashApplication.getApplication(),
                    "Unknown item: $itemInfo, this is probably a bug!",
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    companion object {
        private const val TAG = "NavigationOnItemViewClickedListener"
    }
}
