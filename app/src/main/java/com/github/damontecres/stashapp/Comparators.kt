package com.github.damontecres.stashapp

import androidx.recyclerview.widget.DiffUtil
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData

object sceneComparator : DiffUtil.ItemCallback<SlimSceneData>() {
    override fun areItemsTheSame(oldItem: SlimSceneData, newItem: SlimSceneData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SlimSceneData, newItem: SlimSceneData): Boolean {
        return oldItem == newItem
    }
}

object performerComparator : DiffUtil.ItemCallback<PerformerData>() {
    override fun areItemsTheSame(oldItem: PerformerData, newItem: PerformerData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PerformerData, newItem: PerformerData): Boolean {
        return oldItem == newItem
    }
}

object studioComparator : DiffUtil.ItemCallback<StudioData>() {
    override fun areItemsTheSame(oldItem: StudioData, newItem: StudioData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: StudioData, newItem: StudioData): Boolean {
        return oldItem == newItem
    }
}