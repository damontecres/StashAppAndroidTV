package com.github.damontecres.stashapp.presenters

import android.graphics.Bitmap
import android.graphics.Canvas
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.StashGlide
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

            cardView.setUpExtraRow(dataTypeMap, null)

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
