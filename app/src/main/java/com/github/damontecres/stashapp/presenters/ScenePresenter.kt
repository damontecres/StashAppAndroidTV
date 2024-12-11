package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.PlaybackActivity
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.resume_position
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import java.util.EnumMap

class ScenePresenter(callback: LongClickCallBack<SlimSceneData>? = null) :
    StashPresenter<SlimSceneData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: SlimSceneData,
    ) {
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

        val videoFile = item.files.firstOrNull()?.videoFileData
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

        if (!item.paths.screenshot.isNullOrBlank()) {
            loadImage(cardView, item.paths.screenshot)
        }
        if (item.paths.preview.isNotNullOrBlank()) {
            cardView.videoUrl = item.paths.preview
        }
    }

    override fun getDefaultLongClickCallBack(cardView: StashImageCardView): LongClickCallBack<SlimSceneData> {
        return object : LongClickCallBack<SlimSceneData> {
            override fun getPopUpItems(
                context: Context,
                item: SlimSceneData,
            ): List<PopUpItem> {
                return if (item.resume_time != null && item.resume_time > 0) {
                    // Has resume
                    listOf(
                        PopUpItem.getDefault(context),
                        PopUpItem(1, "Resume Scene"),
                        PopUpItem(2, "Restart Scene"),
                    )
                } else {
                    listOf(
                        PopUpItem.getDefault(context),
                        PopUpItem(2, "Play Scene"),
                    )
                }
            }

            override fun onItemLongClick(
                context: Context,
                item: SlimSceneData,
                popUpItem: PopUpItem,
            ) {
                when (popUpItem.id) {
                    0L -> {
                        cardView.performClick()
                    }

                    1L -> {
                        // Resume
                        val intent = Intent(context, PlaybackActivity::class.java)
                        intent.putExtra(Constants.SCENE_ARG, Scene.fromSlimSceneData(item))
                        if (item.resume_time != null) {
                            intent.putExtra(Constants.POSITION_ARG, item.resume_position!!)
                        }
                        context.startActivity(intent)
                    }

                    2L -> {
                        // Restart/Play
                        val intent = Intent(context, PlaybackActivity::class.java)
                        intent.putExtra(Constants.SCENE_ARG, Scene.fromSlimSceneData(item))
                        context.startActivity(intent)
                    }

                    else -> {
                        throw IllegalStateException()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ScenePresenter"

        const val CARD_WIDTH = 345
        const val CARD_HEIGHT = 194
    }
}
