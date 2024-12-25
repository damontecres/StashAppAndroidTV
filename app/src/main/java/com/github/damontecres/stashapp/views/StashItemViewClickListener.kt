package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.Log
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.actions.StashActionClickedListener
import com.github.damontecres.stashapp.data.OCounter

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
        if (item is StashAction) {
            if (actionListener != null) {
                actionListener.onClicked(item)
            } else {
                throw RuntimeException("Action $item clicked, but no actionListener was provided!")
            }
        } else if (item is OCounter) {
            actionListener!!.incrementOCounter(item)
        } else {
            Log.e(TAG, "Unknown item type: ${item.javaClass}")
        }
    }

    companion object {
        private const val TAG = "StashItemViewClickListener"
    }
}
