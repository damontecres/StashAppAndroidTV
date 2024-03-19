package com.github.damontecres.stashapp.presenters

import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.isNotNullOrBlank
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
        val coverPaths = item.cover?.paths
        if (coverPaths?.thumbnail.isNotNullOrBlank()) {
            StashGlide.with(cardView.context, coverPaths!!.thumbnail!!)
                .transform(CenterCrop())
                .error(glideError(cardView.context))
                .into(cardView.mainImageView!!)
        }
        if (coverPaths?.preview.isNotNullOrBlank()) {
            cardView.videoUrl = coverPaths?.preview
        }

        cardView.setRating100(item.rating100)
    }

    companion object {
        private const val TAG = "GalleryPresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 237
    }
}
