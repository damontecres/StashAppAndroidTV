package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.data.DataType
import java.util.EnumMap

class MoviePresenter(callback: LongClickCallBack<MovieData>? = null) :
    StashPresenter<MovieData>(callback) {
    override fun doOnBindViewHolder(
        cardView: ImageCardView,
        item: MovieData,
    ) {
        if (item.front_image_path != null) {
            cardView.titleText = item.name
            cardView.contentText = item.date

            val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
            dataTypeMap[DataType.SCENE] = item.scene_count
            setUpExtraRow(cardView, dataTypeMap, null)

            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            loadImage(cardView, item.front_image_path)
        }
    }

    companion object {
        private const val TAG = "CardPresenter"

        const val CARD_HEIGHT = 381
        const val CARD_WIDTH = CARD_HEIGHT * 2 / 3
    }
}
