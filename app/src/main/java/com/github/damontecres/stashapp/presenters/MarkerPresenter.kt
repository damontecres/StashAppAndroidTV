package com.github.damontecres.stashapp.presenters

import android.content.Context
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import java.util.EnumMap
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MarkerPresenter(
    callback: LongClickCallBack<MarkerData>? = null,
) : StashPresenter<MarkerData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: MarkerData,
    ) {
        val title =
            item.title.ifBlank {
                item.primary_tag.slimTagData.name
            }
        cardView.titleText = "$title - ${item.seconds.toInt().toDuration(DurationUnit.SECONDS)}"
        cardView.contentText =
            listOf(
                if (item.title.isNotBlank()) item.primary_tag.slimTagData.name else null,
                item.scene.videoSceneData.titleOrFilename,
            ).joinNotNullOrBlank(" - ")

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.TAG] = item.tags.size
        cardView.setUpExtraRow(dataTypeMap, null)

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        loadImage(cardView, item.screenshot)
        cardView.videoUrl = item.stream
    }

    override fun getDefaultLongClickCallBack(cardView: StashImageCardView): LongClickCallBack<MarkerData> =
        object : LongClickCallBack<MarkerData> {
            override fun getPopUpItems(
                context: Context,
                item: MarkerData,
            ): List<PopUpItem> =
                listOf(
                    PopUpItem(0L, context.getString(R.string.go_to)),
                    PopUpItem(1L, context.getString(R.string.go_to_scene)),
                    PopUpItem(2L, context.getString(R.string.stashapp_details)),
                )

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
                        StashApplication.navigationManager.navigate(
                            Destination.Item(
                                DataType.SCENE,
                                item.scene.videoSceneData.id,
                            ),
                        )
                    }

                    2L -> {
                        StashApplication.navigationManager.navigate(
                            Destination.MarkerDetails(
                                item.id,
                                item.scene.videoSceneData.id,
                            ),
                        )
                    }

                    else -> {
                        throw IllegalStateException()
                    }
                }
            }
        }

    companion object {
        private const val TAG = "MarkerPresenter"

        const val CARD_WIDTH = ScenePresenter.CARD_WIDTH
        const val CARD_HEIGHT = ScenePresenter.CARD_HEIGHT
    }
}
