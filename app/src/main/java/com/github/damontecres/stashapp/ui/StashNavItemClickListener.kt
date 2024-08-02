package com.github.damontecres.stashapp.ui

import android.util.Log
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.navigation.NavController
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.actions.StashActionClickedListener
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter

/**
 * A OnItemViewClickedListener that starts activities for scenes, performers, etc
 */
class StashNavItemClickListener(
    private val navController: NavController,
    private val actionListener: StashActionClickedListener? = null,
) : OnItemViewClickedListener {
    fun onItemClicked(item: Any) {
        onItemClicked(null, item, null, null)
    }

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?,
    ) {
        if (item is SlimSceneData) {
            navController.navigate(Route.DataTypeRoute(DataType.SCENE, item.id))
        } else if (item is PerformerData) {
            navController.navigate(Route.DataTypeRoute(DataType.PERFORMER, item.id))
        } else if (item is TagData) {
            navController.navigate(Route.DataTypeRoute(DataType.TAG, item.id))
        } else if (item is StudioData) {
            navController.navigate(Route.DataTypeRoute(DataType.STUDIO, item.id))
        } else if (item is MovieData) {
            navController.navigate(Route.DataTypeRoute(DataType.MOVIE, item.id))
        } else if (item is MarkerData) {
            val route =
                Route.Playback(item.scene.videoSceneData.id, (item.seconds * 1000).toLong())
            navController.navigate(route)
        } else if (item is ImageData) {
            // TODO handle image switches
            navController.navigate(Route.DataTypeRoute(DataType.IMAGE, item.id))
        } else if (item is GalleryData) {
            navController.navigate(Route.DataTypeRoute(DataType.GALLERY, item.id))
        } else if (item is StashSavedFilter) {
            throw UnsupportedOperationException()
        } else if (item is StashCustomFilter) {
            throw UnsupportedOperationException()
        } else if (item is StashAction) {
            if (actionListener != null) {
                actionListener.onClicked(item)
            } else {
                throw RuntimeException("Action $item clicked, but no actionListener was provided!")
            }
        } else if (item is OCounter) {
            actionListener!!.incrementOCounter(item)
        } else {
            Log.e(TAG, "Unknown item type: $item")
        }
    }

    companion object {
        private const val TAG = "StashNavItemClickListener"
    }
}
