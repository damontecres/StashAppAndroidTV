package com.github.damontecres.stashapp.presenters

import android.os.Build
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.ageInYears
import java.util.EnumMap

class PerformerPresenter(callback: LongClickCallBack<PerformerData>? = null) :
    StashPresenter<PerformerData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: PerformerData,
    ) {
        val title =
            item.name + (if (!item.disambiguation.isNullOrBlank()) " (${item.disambiguation})" else "")
        cardView.titleText = title

        cardView.contentText =
            if (item.birthdate != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val yearsOldStr = cardView.context.getString(R.string.stashapp_years_old)
                "${item.ageInYears} $yearsOldStr"
            } else {
                null
            }

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.SCENE] = item.scene_count

        cardView.setUpExtraRow(dataTypeMap, item.o_counter)

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        if (item.image_path != null) {
            loadImage(cardView, item.image_path)
        }

        cardView.setRating100(item.rating100)
    }

    companion object {
        private const val TAG = "CardPresenter"

        const val CARD_HEIGHT = 381
        const val CARD_WIDTH = CARD_HEIGHT * 2 / 3
    }
}
