package com.github.damontecres.stashapp.itemclick

import android.content.Context
import android.content.Intent
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.github.damontecres.stashapp.ImageActivity
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.data.StashFilter
import com.github.damontecres.stashapp.util.addToIntent

class OnImageFilterClickedListener(
    private val context: Context,
    private val callback: (ImageData) -> FilterPosition,
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
        intent.putExtra(ImageActivity.INTENT_POSITION, filterPosition.position)
        intent.putExtra(ImageActivity.INTENT_FILTER, filterPosition.filter)
        intent.putExtra(ImageActivity.INTENT_FILTER_TYPE, filterPosition.filter?.filterType?.name)
        context.startActivity(intent)
    }

    data class FilterPosition(val filter: StashFilter?, val position: Int?)
}
