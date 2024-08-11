package com.github.damontecres.stashapp.presenters

import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter

class StashFilterPresenter(callback: LongClickCallBack<Any>? = null) :
    StashPresenter<Any>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: Any,
    ) {
        cardView.titleText = cardView.context.getString(R.string.stashapp_view_all)

        val mode: FilterMode =
            when (item) {
                is StashSavedFilter -> {
                    item.mode
                }

                is StashCustomFilter -> {
                    item.mode
                }

                else -> {
                    throw RuntimeException("Unsupported item $item")
                }
            }

        when (mode) {
            FilterMode.SCENES ->
                cardView.setMainImageDimensions(
                    ScenePresenter.CARD_WIDTH,
                    ScenePresenter.CARD_HEIGHT,
                )

            FilterMode.STUDIOS ->
                cardView.setMainImageDimensions(
                    StudioPresenter.CARD_WIDTH,
                    StudioPresenter.CARD_HEIGHT,
                )

            FilterMode.PERFORMERS ->
                cardView.setMainImageDimensions(
                    PerformerPresenter.CARD_WIDTH,
                    PerformerPresenter.CARD_HEIGHT,
                )

            FilterMode.TAGS ->
                cardView.setMainImageDimensions(
                    TagPresenter.CARD_WIDTH,
                    TagPresenter.CARD_HEIGHT,
                )

            FilterMode.MOVIES -> {
                cardView.setMainImageDimensions(
                    MoviePresenter.CARD_WIDTH,
                    MoviePresenter.CARD_HEIGHT,
                )
            }

            FilterMode.SCENE_MARKERS -> {
                cardView.setMainImageDimensions(
                    MarkerPresenter.CARD_WIDTH,
                    MarkerPresenter.CARD_HEIGHT,
                )
            }

            FilterMode.IMAGES -> {
                cardView.setMainImageDimensions(
                    ImagePresenter.CARD_WIDTH,
                    ImagePresenter.CARD_HEIGHT,
                )
            }

            FilterMode.GALLERIES -> {
                cardView.setMainImageDimensions(
                    GalleryPresenter.CARD_WIDTH,
                    GalleryPresenter.CARD_HEIGHT,
                )
            }

            else -> {
                Log.w(TAG, "Unsupported FilterMode=$mode")
            }
        }

        cardView.mainImageView.setImageDrawable(
            AppCompatResources.getDrawable(
                cardView.context,
                R.drawable.baseline_camera_indoor_48,
            ),
        )
    }

    companion object {
        const val TAG = "StashFilterPresenter"
    }
}
