package com.github.damontecres.stashapp

import androidx.recyclerview.widget.DiffUtil
import com.github.damontecres.stashapp.api.fragment.SlimSceneData

object sceneComparator : DiffUtil.ItemCallback<SlimSceneData>() {
    override fun areItemsTheSame(oldItem: SlimSceneData, newItem: SlimSceneData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SlimSceneData, newItem: SlimSceneData): Boolean {
        return oldItem == newItem
    }
}