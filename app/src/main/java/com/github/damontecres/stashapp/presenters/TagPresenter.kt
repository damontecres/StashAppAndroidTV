package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Tag
import java.util.EnumMap

class TagPresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val tag = item as Tag
        val cardView = viewHolder.view as ImageCardView

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.SCENE] = tag.sceneCount
        dataTypeMap[DataType.PERFORMER] = tag.performerCount
        setUpExtraRow(cardView, dataTypeMap, null)

        cardView.titleText = tag.name
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        // TODO: fetch image
    }

    companion object {
        private const val TAG = "TagPresenter"

        const val CARD_WIDTH = 250
        const val CARD_HEIGHT = 250
    }
}
