package com.github.damontecres.stashapp.presenters

import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.presenters.StashPresenter.PopUpAction
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
        cardView.blackImageBackground = true
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

    override fun getDefaultLongClickCallBack(): LongClickCallBack<MarkerData> =
        LongClickCallBack<MarkerData>(
            PopUpItem.DEFAULT to PopUpAction { cardView, _ -> cardView.performClick() },
            PopUpItem(1L, R.string.go_to_scene) to
                PopUpAction { _, item ->
                    StashApplication.navigationManager.navigate(
                        Destination.Item(
                            DataType.SCENE,
                            item.scene.videoSceneData.id,
                        ),
                    )
                },
            PopUpItem(2L, R.string.stashapp_details) to
                PopUpAction { _, item ->
                    StashApplication.navigationManager.navigate(
                        Destination.MarkerDetails(
                            item.id,
                            item.scene.videoSceneData.id,
                        ),
                    )
                },
        )

    companion object {
        private const val TAG = "MarkerPresenter"

        const val CARD_WIDTH = ScenePresenter.CARD_WIDTH
        const val CARD_HEIGHT = ScenePresenter.CARD_HEIGHT
    }
}
