package com.github.damontecres.stashapp.presenters

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.createGlideUrl
import java.io.File
import kotlin.properties.Delegates

class ScenePresenter : StashPresenter() {

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val scene = item as SlimSceneData
        val cardView = viewHolder.view as ImageCardView
        if (scene.title.isNullOrBlank()) {
            val path = scene.files.firstOrNull()?.videoFileData?.path
            if (path != null) {
                cardView.titleText = File(path).name
            }
        } else {
            cardView.titleText = scene.title
        }
        cardView.contentText =
            """${scene.date.orEmpty()} (${scene.performers.size}P, ${scene.tags.size}T)"""

        if (!scene.paths.screenshot.isNullOrBlank()) {
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            val apiKey = PreferenceManager.getDefaultSharedPreferences(vParent.context)
                .getString("stashApiKey", "")
            val url = createGlideUrl(scene.paths.screenshot, apiKey)
            Glide.with(viewHolder.view.context)
                .load(url)
                .centerCrop()
                .error(mDefaultCardImage)
                .into(cardView.mainImageView!!)
        }
    }

    companion object {
        private val TAG = "ScenePresenter"

        const val CARD_WIDTH = 351
        const val CARD_HEIGHT = 198
    }
}
