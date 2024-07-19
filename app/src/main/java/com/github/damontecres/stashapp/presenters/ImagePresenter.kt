package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import java.util.EnumMap

class ImagePresenter(callback: LongClickCallBack<ImageData>? = null) : StashPresenter<ImageData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: ImageData,
    ) {
        cardView.titleText = item.title

        val details = mutableListOf<String?>()
        details.add(item.studio?.studioData?.name)
        details.add(item.date)
        cardView.contentText = concatIfNotBlank(" - ", details)

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.TAG] = item.tags.size
        dataTypeMap[DataType.PERFORMER] = item.performers.size
        dataTypeMap[DataType.GALLERY] = item.galleries.size

        cardView.setUpExtraRow(dataTypeMap, item.o_counter)

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        if (item.paths.thumbnail.isNotNullOrBlank()) {
            loadImage(cardView, item.paths.thumbnail)
        } else if (item.paths.image.isNotNullOrBlank() && !item.isImageClip) {
            loadImage(cardView, item.paths.image)
        }
        if (item.paths.preview.isNotNullOrBlank()) {
            cardView.videoUrl = item.paths.preview
        }

        cardView.setRating100(item.rating100)
    }

    companion object {
        private const val TAG = "ImagePresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 237
    }
}
