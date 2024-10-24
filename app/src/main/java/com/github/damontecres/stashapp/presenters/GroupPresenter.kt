package com.github.damontecres.stashapp.presenters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Spannable
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.StashImageCardView.Companion.FA_FONT
import com.github.damontecres.stashapp.presenters.StashImageCardView.Companion.ICON_SPACING
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.views.FontSpan
import java.security.MessageDigest
import java.util.EnumMap

class GroupPresenter(callback: LongClickCallBack<GroupData>? = null) :
    StashPresenter<GroupData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: GroupData,
    ) {
        if (item.front_image_path != null) {
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

            StashGlide.with(cardView.context, item.front_image_path)
                .transform(GroupPosterScale())
                .error(glideError(cardView.context))
                .into(cardView.mainImageView!!)
        }

        cardView.setRating100(item.rating100)
    }

    /**
     * Scales an image by width while maintaining the aspect ratio
     */
    private class GroupPosterScale : BitmapTransformation() {
        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update(ID)
        }

        override fun transform(
            pool: BitmapPool,
            toTransform: Bitmap,
            outWidth: Int,
            outHeight: Int,
        ): Bitmap {
            val result = Bitmap.createBitmap(outWidth, outHeight, toTransform.config)
            val scaledHeight = (outWidth / toTransform.width.toDouble() * outHeight).toInt()
            val scaled =
                Bitmap.createScaledBitmap(toTransform, outWidth, scaledHeight, true)
            val canvas = Canvas(result)
            canvas.drawBitmap(scaled, 0.0F, 0.0F, null)
            return result
        }

        companion object {
            private val ID = GroupPresenter::class.qualifiedName!!.toByteArray()
        }
    }

    companion object {
        private const val TAG = "CardPresenter"

        const val CARD_HEIGHT = 381
        const val CARD_WIDTH = CARD_HEIGHT * 2 / 3
    }
}
