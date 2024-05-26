package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.R
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
        cardView.hideOverlayOnSelection = false

        cardView.titleText = item.name
        cardView.contentText = item.description
        if (item.parent_count > 0) {
            val parentText =
                cardView.context.getString(
                    R.string.stashapp_parent_of,
                    item.parent_count.toString(),
                )
            cardView.setTextOverlayText(StashImageCardView.OverlayPosition.TOP_LEFT, parentText)
        }
        if (item.child_count > 0) {
            val childText =
                cardView.context.getString(
                    R.string.stashapp_sub_tag_of,
                    item.child_count.toString(),
                )
            cardView.setTextOverlayText(StashImageCardView.OverlayPosition.BOTTOM_LEFT, childText)
        }
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
