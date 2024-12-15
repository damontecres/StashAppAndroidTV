package com.github.damontecres.stashapp.presenters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.PlaylistItem
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.enableMarquee
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener

class PlaylistItemPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val root = inflater.inflate(R.layout.playlist_item, parent, false)
        root.onFocusChangeListener = StashOnFocusChangeListener(parent.context)
        return PlaylistItemViewHolder(root)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val vh = viewHolder as PlaylistItemViewHolder
        if (item != null) {
            item as PlaylistItem
            vh.indexView.text = (item.index + 1).toString()
            vh.titleView.text = item.title
            vh.subtitleView.text = item.subtitle
            vh.details1View.text = item.details1
            vh.details2View.text = item.details2

            if (item.imageUrl.isNotNullOrBlank()) {
                StashGlide
                    .with(vh.view.context, item.imageUrl)
                    .into(vh.imageView)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val vh = viewHolder as PlaylistItemViewHolder
        vh.indexView.text = null
        vh.titleView.text = null
        vh.subtitleView.text = null
        vh.imageView.setImageDrawable(null)
    }

    private class PlaylistItemViewHolder(
        view: View,
    ) : ViewHolder(view) {
        val indexView: TextView = view.findViewById(R.id.index)
        val titleView: TextView = view.findViewById(R.id.title)
        val subtitleView: TextView = view.findViewById(R.id.subtitle)
        val details1View: TextView = view.findViewById(R.id.details_1)
        val details2View: TextView = view.findViewById(R.id.details_2)
        val imageView: ImageView = view.findViewById(R.id.image)

        init {
            val lp = imageView.layoutParams
            lp.width = ScenePresenter.CARD_WIDTH * 2 / 3
            lp.height = ScenePresenter.CARD_HEIGHT * 2 / 3
            imageView.layoutParams = lp

            titleView.enableMarquee()
            subtitleView.enableMarquee()
            details1View.enableMarquee()
            details2View.enableMarquee()
        }
    }
}
