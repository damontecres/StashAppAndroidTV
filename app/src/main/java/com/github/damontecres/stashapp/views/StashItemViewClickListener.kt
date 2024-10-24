package com.github.damontecres.stashapp.views

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.github.damontecres.stashapp.DataTypeActivity
import com.github.damontecres.stashapp.FilterListActivity
import com.github.damontecres.stashapp.GalleryFragment
import com.github.damontecres.stashapp.ImageActivity
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.actions.StashActionClickedListener
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Group
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.Performer
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.data.toGallery
import com.github.damontecres.stashapp.playback.PlaybackActivity
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.addToIntent
import com.github.damontecres.stashapp.util.putDataType
import com.github.damontecres.stashapp.util.putFilterArgs

/**
 * A OnItemViewClickedListener that starts activities for scenes, performers, etc
 */
class StashItemViewClickListener(
    private val context: Context,
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
            val intent = Intent(context, DataTypeActivity::class.java)
            intent.putDataType(DataType.SCENE)
            intent.putExtra(Constants.SCENE_ID_ARG, item.id)
            context.startActivity(intent)
        } else if (item is PerformerData) {
            val intent = Intent(context, DataTypeActivity::class.java)
            intent.putDataType(DataType.PERFORMER)
            intent.putExtra("performer", Performer(item))
            context.startActivity(intent)
        } else if (item is TagData) {
            val intent = Intent(context, DataTypeActivity::class.java)
            intent.putDataType(DataType.TAG)
            intent.putExtra("tagId", item.id)
            intent.putExtra("tagName", item.name)
            context.startActivity(intent)
        } else if (item is StudioData) {
            val intent = Intent(context, DataTypeActivity::class.java)
            intent.putDataType(DataType.STUDIO)
            intent.putExtra("studioId", item.id)
            intent.putExtra("studioName", item.name)
            context.startActivity(intent)
        } else if (item is GroupData) {
            val intent = Intent(context, DataTypeActivity::class.java)
            intent.putDataType(DataType.GROUP)
            intent.putExtra("group", Group(item))
            context.startActivity(intent)
        } else if (item is MarkerData) {
            val intent = Intent(context, PlaybackActivity::class.java)
            intent.putDataType(DataType.MARKER)
            intent.putExtra(
                Constants.SCENE_ARG,
                Scene.fromVideoSceneData(item.scene.videoSceneData),
            )
            intent.putExtra(Constants.POSITION_ARG, (item.seconds * 1000).toLong())
            context.startActivity(intent)
        } else if (item is ImageData) {
            val intent = Intent(context, ImageActivity::class.java)
            intent.putDataType(DataType.IMAGE)
            item.addToIntent(intent)
            context.startActivity(intent)
        } else if (item is GalleryData) {
            val intent =
                Intent(context, DataTypeActivity::class.java)
                    .putExtra(GalleryFragment.INTENT_GALLERY_OBJ, item.toGallery())
            intent.putDataType(DataType.GALLERY)
            context.startActivity(intent)
        } else if (item is FilterArgs) {
            val intent =
                Intent(context, FilterListActivity::class.java)
                    .putFilterArgs(FilterListActivity.INTENT_FILTER_ARGS, item)
                    .putExtra(FilterListActivity.INTENT_SCROLL_NEXT_PAGE, true)
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
