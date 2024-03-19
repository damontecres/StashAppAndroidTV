package com.github.damontecres.stashapp.presenters

import android.graphics.Typeface
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import java.util.EnumMap

class ScenePresenter(callback: LongClickCallBack<SlimSceneData>? = null) :
    StashPresenter<SlimSceneData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: SlimSceneData,
    ) {
        cardView.titleText = item.titleOrFilename

        val details = mutableListOf<String?>()
        details.add(item.studio?.name)
        details.add(item.date)
        cardView.contentText = concatIfNotBlank(" - ", details)

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.TAG] = item.tags.size
        dataTypeMap[DataType.PERFORMER] = item.performers.size
        dataTypeMap[DataType.MOVIE] = item.movies.size
        dataTypeMap[DataType.MARKER] = item.scene_markers.size

        cardView.setUpExtraRow(dataTypeMap, item.o_counter)

        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        val videoFile = item.files.firstOrNull()?.videoFileData
        if (videoFile != null) {
            val duration = Constants.durationToString(videoFile.duration)
            cardView.setTextOverlayText(StashImageCardView.OverlayPosition.BOTTOM_RIGHT, duration)

            // TODO: 2160P => 4k, etc
            val resolution = "${videoFile.height}P"
            val resText = cardView.getTextOverlay(StashImageCardView.OverlayPosition.BOTTOM_LEFT)
            resText.setTypeface(null, Typeface.BOLD)
            resText.text = resolution

            if (item.resume_time != null) {
                val percentWatched = item.resume_time / videoFile.duration
                cardView.setProgress(percentWatched)
            }
        }

        cardView.setRating100(item.rating100)
        if (item.studio != null) {
            if (ServerPreferences(cardView.context).showStudioAsText) {
                cardView.setTextOverlayText(
                    StashImageCardView.OverlayPosition.TOP_RIGHT,
                    item.studio.name,
                )
            } else {
                cardView.setTopRightImage(item.studio.image_path, item.studio.name)
            }
        }

        if (!item.paths.screenshot.isNullOrBlank()) {
            StashGlide.with(cardView.context, item.paths.screenshot)
                .centerCrop()
                // .transform(CenterCrop(), TextOverlay(cardView.context, item))
                .error(glideError(cardView.context))
                .into(cardView.mainImageView!!)
        }
        if (item.paths.preview.isNotNullOrBlank()) {
            cardView.videoUrl = item.paths.preview
        }
    }

    companion object {
        private const val TAG = "ScenePresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 198

        const val TEXT_PADDING = 5F
        const val WATCHED_HEIGHT = 3F
    }
}
