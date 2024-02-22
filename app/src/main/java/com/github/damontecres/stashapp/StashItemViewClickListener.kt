package com.github.damontecres.stashapp

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.github.damontecres.stashapp.VideoDetailsFragment.Companion.POSITION_ARG
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.actions.StashActionClickedListener
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Movie
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.Performer
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter

/**
 * A OnItemViewClickedListener that starts activities for scenes, performers, etc
 */
class StashItemViewClickListener(
    private val context: Context,
    private val actionListener: StashActionClickedListener? = null,
) : OnItemViewClickedListener {
    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?,
    ) {
        if (item is SlimSceneData) {
            val intent = Intent(context, VideoDetailsActivity::class.java)
            intent.putExtra(VideoDetailsActivity.MOVIE, item.id)
            context.startActivity(intent)
        } else if (item is PerformerData) {
            val intent = Intent(context, PerformerActivity::class.java)
            intent.putExtra("performer", Performer(item))
            context.startActivity(intent, null)
        } else if (item is TagData) {
            val intent = Intent(context, TagActivity::class.java)
            intent.putExtra("tagId", item.id)
            intent.putExtra("tagName", item.name)
            context.startActivity(intent)
        } else if (item is StudioData) {
            val intent = Intent(context, StudioActivity::class.java)
            intent.putExtra("studioId", item.id.toInt())
            intent.putExtra("studioName", item.name)
            context.startActivity(intent)
        } else if (item is MovieData) {
            val intent = Intent(context, MovieActivity::class.java)
            intent.putExtra("movie", Movie(item))
            context.startActivity(intent)
        } else if (item is MarkerData) {
            val intent = Intent(context, PlaybackActivity::class.java)
            intent.putExtra(
                VideoDetailsActivity.MOVIE,
                Scene.fromSlimSceneData(item.scene.slimSceneData),
            )
            intent.putExtra(POSITION_ARG, (item.seconds * 1000).toLong())
            context.startActivity(intent)
        } else if (item is StashSavedFilter) {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("savedFilterId", item.savedFilterId)
            intent.putExtra("dataType", DataType.fromFilterMode(item.mode)!!.name)
            intent.putExtra("useRandom", false)
            intent.putExtra("sortBy", item.sortBy)
            intent.putExtra("moveOnePage", true)
            context.startActivity(intent)
        } else if (item is StashCustomFilter) {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("direction", item.direction)
            intent.putExtra("sortBy", item.sortBy)
            intent.putExtra("dataType", DataType.fromFilterMode(item.mode)!!.name)
            intent.putExtra("description", item.description)
            intent.putExtra("useRandom", false)
            intent.putExtra("moveOnePage", true)
            intent.putExtra("query", item.query)
            context.startActivity(intent)
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
        private const val TAG = "StashItemViewClickListener"
    }
}
