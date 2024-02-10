package com.github.damontecres.stashapp.presenters

import android.widget.ImageView
import androidx.leanback.widget.ImageCardView
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.createGlideUrl
import java.util.EnumMap

class StudioPresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val studio = item as StudioData
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = studio.name
        if (studio.parent_studio != null) {
            cardView.contentText =
                cardView.context.getString(R.string.stashapp_part_of, studio.parent_studio.name)
        }

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.SCENE] = studio.scene_count
        dataTypeMap[DataType.PERFORMER] = studio.performer_count
        dataTypeMap[DataType.MOVIE] = studio.movie_count
        setUpExtraRow(cardView, dataTypeMap, null)

        if (!studio.image_path.isNullOrBlank()) {
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            cardView.setMainImageScaleType(ImageView.ScaleType.FIT_CENTER)
            val apiKey =
                PreferenceManager.getDefaultSharedPreferences(vParent.context)
                    .getString("stashApiKey", "")
            val url = createGlideUrl(studio.image_path, apiKey)
            Glide.with(viewHolder.view.context)
                .load(url)
                .fitCenter()
                .error(mDefaultCardImage)
                .into(cardView.mainImageView!!)
        }
    }

    companion object {
        private const val TAG = "StudioPresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 198
    }
}
