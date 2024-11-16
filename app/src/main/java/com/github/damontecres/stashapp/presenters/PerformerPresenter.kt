package com.github.damontecres.stashapp.presenters

import android.graphics.Color
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.ageInYears
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import java.util.EnumMap

open class PerformerPresenter(callback: LongClickCallBack<PerformerData>? = null) :
    StashPresenter<PerformerData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: PerformerData,
    ) {
        cardView.titleText =
            SpannableStringBuilder().apply {
                append(item.name)
                val start = length
                if (item.disambiguation.isNotNullOrBlank()) {
                    append(" (")
                    append(item.disambiguation)
                    append(")")
                }
                val end = length
                setSpan(RelativeSizeSpan(.75f), start, end, 0)
                setSpan(ForegroundColorSpan(Color.LTGRAY), start, end, 0)
            }

        cardView.contentText = getContentText(cardView, item)

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.SCENE] = item.scene_count
        dataTypeMap[DataType.TAG] = item.tags.size
        dataTypeMap[DataType.GROUP] = item.group_count
        dataTypeMap[DataType.IMAGE] = item.image_count
        dataTypeMap[DataType.GALLERY] = item.gallery_count

        cardView.setUpExtraRow(dataTypeMap, item.o_counter)

        if (item.favorite) {
            cardView.setIsFavorite()
        }

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        if (item.image_path != null) {
            loadImage(cardView, item.image_path)
        }

        cardView.setRating100(item.rating100)
    }

    open fun getContentText(
        cardView: StashImageCardView,
        item: PerformerData,
    ): CharSequence? {
        return if (item.birthdate != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val yearsOldStr = cardView.context.getString(R.string.stashapp_years_old)
            "${item.ageInYears} $yearsOldStr"
        } else if (item.birthdate.isNotNullOrBlank()) {
            item.birthdate
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "CardPresenter"

        const val CARD_HEIGHT = 381
        const val CARD_WIDTH = CARD_HEIGHT * 2 / 3
    }
}
