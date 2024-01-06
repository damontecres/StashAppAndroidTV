package com.github.damontecres.stashapp.presenters

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.Tag
import kotlin.properties.Delegates


class TagPresenter : StashPresenter() {
    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val tag = item as Tag
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = tag.name
        // TODO: content text
        cardView.contentText = "${tag.sceneCount} scenes"
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        // TODO: fetch image
    }

    companion object {
        private const val TAG = "TagPresenter"

        private const val CARD_WIDTH = 250
        private const val CARD_HEIGHT = 250
    }
}