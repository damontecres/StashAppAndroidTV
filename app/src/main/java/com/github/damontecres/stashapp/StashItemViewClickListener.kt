package com.github.damontecres.stashapp

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.data.Tag
import com.github.damontecres.stashapp.data.performerFromPerformerData
import com.github.damontecres.stashapp.data.sceneFromSlimSceneData

/**
 * A OnItemViewClickedListener that starts activities for scenes, performers, etc
 */
class StashItemViewClickListener(private val activity: Activity) : OnItemViewClickedListener {

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder,
        item: Any,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?
    ) {

        if (item is SlimSceneData) {
            val intent = Intent(activity, DetailsActivity::class.java)
            intent.putExtra(DetailsActivity.MOVIE, sceneFromSlimSceneData(item))

            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                (itemViewHolder.view as ImageCardView).mainImageView,
                DetailsActivity.SHARED_ELEMENT_NAME
            )
                .toBundle()
            activity.startActivity(intent, bundle)
        } else if (item is PerformerData) {
            val intent = Intent(activity, PerformerActivity::class.java)
            intent.putExtra("performer", performerFromPerformerData(item))
            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                (itemViewHolder.view as ImageCardView).mainImageView,
                DetailsActivity.SHARED_ELEMENT_NAME
            )
                .toBundle()
            activity.startActivity(intent, bundle)
        } else if (item is Tag) {
            val intent = Intent(activity, TagActivity::class.java)
            intent.putExtra("tag", item)
            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                (itemViewHolder.view as ImageCardView).mainImageView,
                DetailsActivity.SHARED_ELEMENT_NAME
            ).toBundle()
            activity.startActivity(intent, bundle)
        } else if (item is StudioData) {
            val intent = Intent(activity, StudioActivity::class.java)
            intent.putExtra("studioId", item.id.toInt())
            intent.putExtra("studioName", item.name)
            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                (itemViewHolder.view as ImageCardView).mainImageView,
                DetailsActivity.SHARED_ELEMENT_NAME
            ).toBundle()
            activity.startActivity(intent, bundle)
        } else if (item is StashSavedFilter) {
            val intent = Intent(activity, FilterListActivity::class.java)
            intent.putExtra("savedFilterId", item.savedFilterId)
            intent.putExtra("mode", item.mode.rawValue)
            activity.startActivity(intent)
        } else if (item is StashCustomFilter) {
            val intent = Intent(activity, FilterListActivity::class.java)
            intent.putExtra("direction", item.direction)
            intent.putExtra("sortBy", item.sortBy)
            intent.putExtra("mode", item.mode.rawValue)
            intent.putExtra("description", item.description)
            activity.startActivity(intent)
        } else if (item is String) {
            if (item.contains(activity.getString(R.string.error_fragment))) {
                val intent = Intent(activity, BrowseErrorActivity::class.java)
                activity.startActivity(intent)
            } else {
                Toast.makeText(activity, item, Toast.LENGTH_SHORT).show()
            }
        }
    }
}