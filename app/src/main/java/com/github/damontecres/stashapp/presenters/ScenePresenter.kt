package com.github.damontecres.stashapp.presenters

import android.graphics.Typeface
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.resume_position
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import java.util.EnumMap

class ScenePresenter(
    callback: LongClickCallBack<SlimSceneData>? = null,
) : StashPresenter<SlimSceneData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: SlimSceneData,
    ) {
        cardView.blackImageBackground = true
        cardView.titleText = item.titleOrFilename

        val details = mutableListOf<String?>()
        details.add(item.date)
        cardView.contentText = concatIfNotBlank(" - ", details)

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.TAG] = item.tags.size
        dataTypeMap[DataType.PERFORMER] = item.performers.size
        dataTypeMap[DataType.GROUP] = item.groups.size
        dataTypeMap[DataType.MARKER] = item.scene_markers.size
        dataTypeMap[DataType.GALLERY] = item.galleries.size

        cardView.setUpExtraRow(dataTypeMap, item.o_counter)

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        val videoFile = item.files.firstOrNull()?.videoFile
        if (videoFile != null) {
            val duration = durationToString(videoFile.duration)
            cardView.setTextOverlayText(StashImageCardView.OverlayPosition.BOTTOM_RIGHT, duration)

            val resText = cardView.getTextOverlay(StashImageCardView.OverlayPosition.BOTTOM_LEFT)
            resText.setTypeface(null, Typeface.BOLD)
            resText.text = videoFile.resolutionName()

            if (item.resume_time != null) {
                val percentWatched = item.resume_time / videoFile.duration
                cardView.setProgress(percentWatched)
            }
        }

        cardView.setRating100(item.rating100)
        if (item.studio != null) {
            if (StashServer.requireCurrentServer().serverPreferences.showStudioAsText) {
                cardView.setTextOverlayText(
                    StashImageCardView.OverlayPosition.TOP_RIGHT,
                    item.studio.name,
                )
            } else {
                cardView.setTopRightImage(
                    item.studio.image_path,
                    item.studio.name,
                )
            }
        }

        loadImage(cardView, item.paths.screenshot, defaultDrawable = R.drawable.default_scene)
        if (item.paths.preview.isNotNullOrBlank()) {
            cardView.videoUrl = item.paths.preview
        }
    }

    override fun getDefaultLongClickCallBack(): LongClickCallBack<SlimSceneData> =
        LongClickCallBack<SlimSceneData>()
            .addAction(PopUpItem.DEFAULT) { cardView, _ -> cardView.performClick() }
            .addAction(
                PopUpItem(2, "Play Scene"),
                { it.resume_position == null || it.resume_position!! <= 0 },
            ) { _, item ->
                StashApplication.navigationManager.navigate(
                    Destination.Playback(
                        item.id,
                        0L,
                        PlaybackMode.Choose,
                    ),
                )
            }.addAction(
                PopUpItem(3, "Resume Scene"),
                { it.resume_position != null && it.resume_position!! > 0 },
            ) { _, item ->
                StashApplication.navigationManager.navigate(
                    Destination.Playback(
                        item.id,
                        item.resume_position ?: 0L,
                        PlaybackMode.Choose,
                    ),
                )
            }.addAction(
                PopUpItem(4, "Restart Scene"),
                { it.resume_position != null && it.resume_position!! > 0 },
            ) { _, item ->
                StashApplication.navigationManager.navigate(
                    Destination.Playback(
                        item.id,
                        0L,
                        PlaybackMode.Choose,
                    ),
                )
            }

    companion object {
        private const val TAG = "ScenePresenter"

        const val CARD_WIDTH = 345
        const val CARD_HEIGHT = 194
    }
}
