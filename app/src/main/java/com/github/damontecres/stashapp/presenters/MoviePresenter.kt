package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.createGlideUrl
import java.util.EnumMap

class MoviePresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val movie = item as MovieData
        val cardView = viewHolder.view as ImageCardView

        if (movie.front_image_path != null) {
            cardView.titleText = movie.name
            cardView.contentText = movie.date

            val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
            dataTypeMap[DataType.SCENE] = movie.scene_count
            setUpExtraRow(cardView, dataTypeMap, null)

            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            val url = createGlideUrl(movie.front_image_path, cardView.context)
            Glide.with(viewHolder.view.context)
                .load(url)
                .centerCrop()
                .error(mDefaultCardImage)
                .into(cardView.mainImageView!!)
        }
    }

    companion object {
        private const val TAG = "CardPresenter"

        const val CARD_HEIGHT = 381
        const val CARD_WIDTH = CARD_HEIGHT * 2 / 3
    }
}
