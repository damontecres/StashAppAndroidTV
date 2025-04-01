package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.name
import java.util.EnumMap

class GalleryPresenter(
    server: StashServer,
    callback: LongClickCallBack<GalleryData>? = null,
) : StashPresenter<GalleryData>(server, callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: GalleryData,
    ) {
        cardView.blackImageBackground = false
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
        loadImage(cardView, coverImage, defaultDrawable = R.drawable.default_gallery)

        cardView.setRating100(item.rating100)
    }

    override fun imageMatchParent(item: GalleryData): Boolean = item.paths.cover.isBlank() || item.paths.cover.isDefaultUrl

    companion object {
        private const val TAG = "GalleryPresenter"

        const val CARD_WIDTH = ImagePresenter.CARD_WIDTH
        const val CARD_HEIGHT = ImagePresenter.CARD_HEIGHT
    }
}
