package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import java.util.EnumMap

class TagPresenter(callback: LongClickCallBack<TagData>? = null) :
    StashPresenter<TagData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: TagData,
    ) {
        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.SCENE] = item.scene_count
        dataTypeMap[DataType.PERFORMER] = item.performer_count
        dataTypeMap[DataType.MARKER] = item.scene_marker_count
        cardView.setUpExtraRow(dataTypeMap, null)

        cardView.titleText = item.name
        cardView.contentText = item.description
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        if (item.image_path != null) {
            loadImage(cardView, item.image_path)
        }
    }

    companion object {
        private const val TAG = "TagPresenter"

        const val CARD_WIDTH = 250
        const val CARD_HEIGHT = 250
    }
}
