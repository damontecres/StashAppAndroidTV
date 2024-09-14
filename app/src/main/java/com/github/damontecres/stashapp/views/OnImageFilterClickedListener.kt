package com.github.damontecres.stashapp.views

import android.content.Context
import android.content.Intent
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.github.damontecres.stashapp.ImageActivity
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.addToIntent
import com.github.damontecres.stashapp.util.putFilterArgs

class OnImageFilterClickedListener(
    private val context: Context,
    private val callback: (ImageData) -> FilterPosition?,
) : OnItemViewClickedListener {
    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?,
    ) {
        val image = item as ImageData

        val filterPosition = callback(image)
        val intent = Intent(context, ImageActivity::class.java)
        image.addToIntent(intent)
        if (filterPosition != null) {
            intent.putExtra(ImageActivity.INTENT_POSITION, filterPosition.position)
            intent.putFilterArgs(ImageActivity.INTENT_FILTER_ARGS, filterPosition.filter)
        }
        context.startActivity(intent)
    }

    data class FilterPosition(val filter: FilterArgs, val position: Int)
}
