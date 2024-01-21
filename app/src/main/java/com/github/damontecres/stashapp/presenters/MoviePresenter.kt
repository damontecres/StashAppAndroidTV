package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ImageCardView
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.util.createGlideUrl

class MoviePresenter : StashPresenter() {
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val movie = item as MovieData
        val cardView = viewHolder.view as ImageCardView

        if (movie.front_image_path != null) {
            cardView.titleText = movie.name
            cardView.contentText = "${movie.scene_count} Scenes"
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            val apiKey =
                PreferenceManager.getDefaultSharedPreferences(vParent.context)
                    .getString("stashApiKey", "")
            val url = createGlideUrl(movie.front_image_path, apiKey)
            Glide.with(viewHolder.view.context)
                .load(url)
                .centerCrop()
                .error(mDefaultCardImage)
                .into(cardView.mainImageView!!)
        }
    }

    companion object {
        private const val TAG = "CardPresenter"

        const val CARD_HEIGHT = 381
        const val CARD_WIDTH = CARD_HEIGHT * 2 / 3
    }
}
