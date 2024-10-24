package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.name
import java.util.EnumMap

class GalleryPresenter(callback: LongClickCallBack<GalleryData>? = null) : StashPresenter<GalleryData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: GalleryData,
    ) {
        cardView.titleText = item.name

        val details = mutableListOf<String?>()
        details.add(item.studio?.name)
        details.add(item.date)
        cardView.contentText = concatIfNotBlank(" - ", details)

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.TAG] = item.tags.size
        dataTypeMap[DataType.PERFORMER] = item.performers.size
        dataTypeMap[DataType.SCENE] = item.scenes.size
        dataTypeMap[DataType.IMAGE] = item.image_count

        cardView.setUpExtraRow(dataTypeMap, null)

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        val coverImage = item.paths.cover
        loadImage(cardView, coverImage)

        cardView.setRating100(item.rating100)
    }

    companion object {
        private const val TAG = "GalleryPresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 237
    }
}
