package com.github.damontecres.stashapp.presenters

import android.widget.ImageView
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import java.util.EnumMap

class StudioPresenter(callback: LongClickCallBack<StudioData>? = null) :
    StashPresenter<StudioData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: StudioData,
    ) {
        cardView.titleText = item.name

        val partOfText =
            if (item.parent_studio != null) {
                cardView.context.getString(R.string.stashapp_part_of, item.parent_studio.name)
            } else {
                null
            }
        val childText =
            if (item.child_studios.isNotEmpty()) {
                cardView.context.getString(
                    R.string.stashapp_parent_of,
                    item.child_studios.size.toString(),
                )
            } else {
                null
            }

        cardView.contentText = listOf(childText, partOfText).joinNotNullOrBlank(" - ")

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.SCENE] = item.scene_count
        dataTypeMap[DataType.PERFORMER] = item.performer_count
        dataTypeMap[DataType.GROUP] = item.group_count
        dataTypeMap[DataType.IMAGE] = item.image_count
        dataTypeMap[DataType.GALLERY] = item.gallery_count
        dataTypeMap[DataType.TAG] = item.tags.size

        cardView.setUpExtraRow(dataTypeMap, null)

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        cardView.setMainImageScaleType(ImageView.ScaleType.FIT_CENTER)
        if (item.image_path.isNotNullOrBlank()) {
            loadImage(cardView, item.image_path)
        }

        cardView.setRating100(item.rating100)

        if (item.favorite) {
            cardView.setIsFavorite()
        }
    }

    companion object {
        private const val TAG = "StudioPresenter"

        const val CARD_WIDTH = ScenePresenter.CARD_WIDTH
        const val CARD_HEIGHT = ScenePresenter.CARD_HEIGHT
    }
}
