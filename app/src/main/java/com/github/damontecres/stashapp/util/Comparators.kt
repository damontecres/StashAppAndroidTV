package com.github.damontecres.stashapp.util

import androidx.leanback.widget.DiffCallback
import androidx.recyclerview.widget.DiffUtil
import com.github.damontecres.stashapp.data.PlaylistItem
import com.github.damontecres.stashapp.data.StashData

object StashComparator : DiffUtil.ItemCallback<StashData>() {
    override fun areItemsTheSame(
        oldItem: StashData,
        newItem: StashData,
    ): Boolean = oldItem::class == newItem::class && oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: StashData,
        newItem: StashData,
    ): Boolean = oldItem::class == newItem::class && oldItem == newItem
}

object StashDiffCallback : DiffCallback<StashData>() {
    override fun areItemsTheSame(
        oldItem: StashData,
        newItem: StashData,
    ): Boolean = StashComparator.areItemsTheSame(oldItem, newItem)

    override fun areContentsTheSame(
        oldItem: StashData,
        newItem: StashData,
    ): Boolean = StashComparator.areContentsTheSame(oldItem, newItem)
}

object PlaylistItemComparator : DiffUtil.ItemCallback<PlaylistItem>() {
    override fun areItemsTheSame(
        oldItem: PlaylistItem,
        newItem: PlaylistItem,
    ): Boolean = oldItem == newItem

    override fun areContentsTheSame(
        oldItem: PlaylistItem,
        newItem: PlaylistItem,
    ): Boolean = oldItem == newItem
}
