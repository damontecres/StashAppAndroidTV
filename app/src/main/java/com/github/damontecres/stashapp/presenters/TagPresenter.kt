package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import java.util.EnumMap

class TagPresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val tag = item as TagData
        val cardView = viewHolder.view as ImageCardView

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.SCENE] = tag.scene_count
        dataTypeMap[DataType.PERFORMER] = tag.performer_count
        dataTypeMap[DataType.MARKER] = tag.scene_marker_count
        setUpExtraRow(cardView, dataTypeMap, null)

        cardView.titleText = tag.name
        cardView.contentText = tag.description
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        // TODO: fetch image
    }

    companion object {
        private const val TAG = "TagPresenter"

        const val CARD_WIDTH = 250
        const val CARD_HEIGHT = 250
    }
}
