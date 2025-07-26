package com.github.damontecres.stashapp.presenters

import androidx.appcompat.content.res.AppCompatResources
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.FilterArgs

class FilterArgsPresenter(
    callback: LongClickCallBack<FilterArgs>? = null,
) : StashPresenter<FilterArgs>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: FilterArgs,
    ) {
        cardView.titleText = cardView.context.getString(R.string.stashapp_view_all)

        when (item.dataType) {
            DataType.SCENE ->
                cardView.setMainImageDimensions(
                    ScenePresenter.CARD_WIDTH,
                    ScenePresenter.CARD_HEIGHT,
                )

            DataType.STUDIO ->
                cardView.setMainImageDimensions(
                    StudioPresenter.CARD_WIDTH,
                    StudioPresenter.CARD_HEIGHT,
                )

            DataType.PERFORMER ->
                cardView.setMainImageDimensions(
                    PerformerPresenter.CARD_WIDTH,
                    PerformerPresenter.CARD_HEIGHT,
                )

            DataType.TAG ->
                cardView.setMainImageDimensions(
                    TagPresenter.CARD_WIDTH,
                    TagPresenter.CARD_HEIGHT,
                )

            DataType.GROUP -> {
                cardView.setMainImageDimensions(
                    GroupPresenter.CARD_WIDTH,
                    GroupPresenter.CARD_HEIGHT,
                )
            }

            DataType.MARKER -> {
                cardView.setMainImageDimensions(
                    MarkerPresenter.CARD_WIDTH,
                    MarkerPresenter.CARD_HEIGHT,
                )
            }

            DataType.IMAGE -> {
                cardView.setMainImageDimensions(
                    ImagePresenter.CARD_WIDTH,
                    ImagePresenter.CARD_HEIGHT,
                )
            }

            DataType.GALLERY -> {
                cardView.setMainImageDimensions(
                    GalleryPresenter.CARD_WIDTH,
                    GalleryPresenter.CARD_HEIGHT,
                )
            }
        }

        cardView.imageView.setBackgroundColor(cardView.context.getColor(android.R.color.transparent))
        cardView.imageView.setImageDrawable(
            AppCompatResources.getDrawable(
                cardView.context,
                R.drawable.baseline_camera_indoor_48,
            ),
        )
    }

    companion object {
        const val TAG = "FilterArgsPresenter"
    }
}
