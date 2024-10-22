package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.content.Intent
import com.github.damontecres.stashapp.DataTypeActivity
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Marker
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.putDataType
import com.github.damontecres.stashapp.util.titleOrFilename
import java.util.EnumMap
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MarkerPresenter(callback: LongClickCallBack<MarkerData>? = null) :
    StashPresenter<MarkerData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: MarkerData,
    ) {
        val title =
            item.title.ifBlank {
                item.primary_tag.tagData.name
            }
        cardView.titleText = "$title - ${item.seconds.toInt().toDuration(DurationUnit.SECONDS)}"
        cardView.contentText =
            listOf(
                if (item.title.isNotBlank()) item.primary_tag.tagData.name else null,
                item.scene.videoSceneData.titleOrFilename,
            ).joinNotNullOrBlank(" - ")

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.TAG] = item.tags.size
        cardView.setUpExtraRow(dataTypeMap, null)

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        loadImage(cardView, item.screenshot)
        cardView.videoUrl = item.stream
    }

    override fun getDefaultLongClickCallBack(cardView: StashImageCardView): LongClickCallBack<MarkerData> {
        return object : LongClickCallBack<MarkerData> {
            override fun getPopUpItems(
                context: Context,
                item: MarkerData,
            ): List<PopUpItem> {
                return listOf(
                    PopUpItem(0L, context.getString(R.string.go_to)),
                    PopUpItem(1L, context.getString(R.string.go_to_scene)),
                    PopUpItem(2L, context.getString(R.string.stashapp_details)),
                )
            }

            override fun onItemLongClick(
                context: Context,
                item: MarkerData,
                popUpItem: PopUpItem,
            ) {
                when (popUpItem.id) {
                    0L -> {
                        cardView.performClick()
                    }

                    1L -> {
                        val intent = Intent(cardView.context, DataTypeActivity::class.java)
                        intent.putDataType(DataType.SCENE)
                        intent.putExtra(Constants.SCENE_ID_ARG, item.scene.videoSceneData.id)
                        cardView.context.startActivity(intent)
                    }

                    2L -> {
                        val intent = Intent(cardView.context, DataTypeActivity::class.java)
                        intent.putDataType(DataType.MARKER)
                        intent.putExtra("marker", Marker(item))
                        cardView.context.startActivity(intent)
                    }

                    else -> {
                        throw IllegalStateException()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MarkerPresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 198
    }
}
