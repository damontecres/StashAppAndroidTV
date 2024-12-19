package com.github.damontecres.stashapp.presenters

import android.text.Spannable
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.StashImageCardView.Companion.FA_FONT
import com.github.damontecres.stashapp.presenters.StashImageCardView.Companion.ICON_SPACING
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.FontSpan
import java.util.EnumMap

class GroupPresenter(
    callback: LongClickCallBack<GroupData>? = null,
) : StashPresenter<GroupData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: GroupData,
    ) {
        cardView.titleText = item.name
        cardView.contentText = item.date

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.SCENE] = item.scene_count
        dataTypeMap[DataType.TAG] = item.tags.size

        cardView.setUpExtraRow(dataTypeMap, null) {
            if (isNotEmpty()) {
                append(ICON_SPACING)
            }
            val marks = mutableListOf<Int>()
            if (item.containing_groups.isNotEmpty() || item.sub_group_count > 0) {
                marks.add(length)
                append(cardView.context.getString(DataType.GROUP.iconStringId) + " ")
            }
            if (item.containing_groups.isNotEmpty()) {
                append(item.containing_groups.size.toString())

                marks.add(length)
                append(cardView.context.getString(R.string.fa_arrow_up_long))
//                    if (item.sub_group_count > 0) {
//                        append(" ")
//                    }
            }
            if (item.sub_group_count > 0) {
                append(item.sub_group_count.toString())

                marks.add(length)
                append(cardView.context.getString(R.string.fa_arrow_down_long))
            }
            marks.forEach { pos ->
                setSpan(
                    FontSpan(FA_FONT),
                    pos,
                    pos + 1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE,
                )
            }
        }

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        if (item.front_image_path.isNotNullOrBlank()) {
            loadImage(cardView, item.front_image_path)
        }

        cardView.setRating100(item.rating100)
    }

    companion object {
        private const val TAG = "GroupPresenter"

        const val CARD_HEIGHT = TagPresenter.CARD_HEIGHT
        const val CARD_WIDTH = TagPresenter.CARD_WIDTH
    }
}
