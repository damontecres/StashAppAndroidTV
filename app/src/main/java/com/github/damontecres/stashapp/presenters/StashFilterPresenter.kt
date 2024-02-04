package com.github.damontecres.stashapp.presenters

import androidx.appcompat.content.res.AppCompatResources
import androidx.leanback.widget.ImageCardView
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter

class StashFilterPresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val cardView = viewHolder.view as ImageCardView
        cardView.titleText = "View All"

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

            else -> {}
        }

        cardView.mainImageView.setImageDrawable(
            AppCompatResources.getDrawable(
                viewHolder.view.context,
                R.drawable.baseline_camera_indoor_48,
            ),
        )
    }
}
