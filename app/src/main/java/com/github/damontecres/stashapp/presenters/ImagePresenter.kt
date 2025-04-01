package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import java.util.EnumMap

class ImagePresenter(
    server: StashServer,
    callback: LongClickCallBack<ImageData>? = null,
) : StashPresenter<ImageData>(server, callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: ImageData,
    ) {
        cardView.blackImageBackground = false

        cardView.titleText = item.titleOrFilename

        val details = mutableListOf<String?>()
        details.add(item.studio?.name)
        details.add(item.date)
        cardView.contentText = concatIfNotBlank(" - ", details)

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.TAG] = item.tags.size
        dataTypeMap[DataType.PERFORMER] = item.performers.size
        dataTypeMap[DataType.GALLERY] = item.galleries.size

        cardView.setUpExtraRow(dataTypeMap, item.o_counter)

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        loadImage(
            cardView,
            item.paths.thumbnail ?: item.paths.image,
            defaultDrawable = R.drawable.default_image,
        )

        if (item.paths.preview.isNotNullOrBlank()) {
            cardView.videoUrl = item.paths.preview
        }

        cardView.setRating100(item.rating100)
    }

    override fun imageMatchParent(item: ImageData): Boolean =
        item.paths.thumbnail?.contains("default=true") == true ||
            item.paths.image?.contains("default=true") == true ||
            item.paths.thumbnail.isNullOrBlank() ||
            item.paths.image.isNullOrBlank()

    companion object {
        private const val TAG = "ImagePresenter"

        const val CARD_WIDTH = ScenePresenter.CARD_WIDTH
        const val CARD_HEIGHT = CARD_WIDTH * 3 / 4
    }
}
