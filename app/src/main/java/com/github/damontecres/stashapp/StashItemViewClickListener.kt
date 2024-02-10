package com.github.damontecres.stashapp

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityOptionsCompat
import androidx.leanback.widget.ImageCardView
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
    private val activity: Activity,
    private val actionListener: StashActionClickedListener? = null,
) : OnItemViewClickedListener {
    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder,
        item: Any,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?,
    ) {
        if (item is SlimSceneData) {
            val intent = Intent(activity, VideoDetailsActivity::class.java)
            intent.putExtra(VideoDetailsActivity.MOVIE, item.id)

            val bundle =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    (itemViewHolder.view as ImageCardView).mainImageView,
                    VideoDetailsActivity.SHARED_ELEMENT_NAME,
                )
                    .toBundle()
            activity.startActivity(intent, bundle)
        } else if (item is PerformerData) {
            val intent = Intent(activity, PerformerActivity::class.java)
            intent.putExtra("performer", Performer(item))
            val bundle =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    (itemViewHolder.view as ImageCardView).mainImageView,
                    VideoDetailsActivity.SHARED_ELEMENT_NAME,
                )
                    .toBundle()
            activity.startActivity(intent, bundle)
        } else if (item is TagData) {
            val intent = Intent(activity, TagActivity::class.java)
            intent.putExtra("tagId", item.id)
            intent.putExtra("tagName", item.name)
            val bundle =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    (itemViewHolder.view as ImageCardView).mainImageView,
                    VideoDetailsActivity.SHARED_ELEMENT_NAME,
                ).toBundle()
            activity.startActivity(intent, bundle)
        } else if (item is StudioData) {
            val intent = Intent(activity, StudioActivity::class.java)
            intent.putExtra("studioId", item.id.toInt())
            intent.putExtra("studioName", item.name)
            val bundle =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    (itemViewHolder.view as ImageCardView).mainImageView,
                    VideoDetailsActivity.SHARED_ELEMENT_NAME,
                ).toBundle()
            activity.startActivity(intent, bundle)
        } else if (item is MovieData) {
            val intent = Intent(activity, MovieActivity::class.java)
            intent.putExtra("movie", Movie(item))
            activity.startActivity(intent)
        } else if (item is MarkerData) {
            val intent = Intent(activity, PlaybackActivity::class.java)
            intent.putExtra(
                VideoDetailsActivity.MOVIE,
                Scene.fromSlimSceneData(item.scene.slimSceneData),
            )
            intent.putExtra(POSITION_ARG, (item.seconds * 1000).toLong())
            activity.startActivity(intent)
        } else if (item is StashSavedFilter) {
            val intent = Intent(activity, FilterListActivity::class.java)
            intent.putExtra("savedFilterId", item.savedFilterId)
            intent.putExtra("dataType", DataType.fromFilterMode(item.mode)!!.name)
            intent.putExtra("useRandom", false)
            intent.putExtra("sortBy", item.sortBy)
            intent.putExtra("moveOnePage", true)
            activity.startActivity(intent)
        } else if (item is StashCustomFilter) {
            val intent = Intent(activity, FilterListActivity::class.java)
            intent.putExtra("direction", item.direction)
            intent.putExtra("sortBy", item.sortBy)
            intent.putExtra("dataType", DataType.fromFilterMode(item.mode)!!.name)
            intent.putExtra("description", item.description)
            intent.putExtra("useRandom", false)
            intent.putExtra("moveOnePage", true)
            activity.startActivity(intent)
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
