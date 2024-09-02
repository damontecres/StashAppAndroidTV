package com.github.damontecres.stashapp.util

import android.util.Log
import androidx.leanback.widget.DiffCallback
import androidx.recyclerview.widget.DiffUtil
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.PlaylistItem

object StashComparator : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(
        oldItem: Any,
        newItem: Any,
    ): Boolean {
        Log.v("StashComparator", "areItemsTheSame")
        if (oldItem.javaClass != newItem.javaClass) {
            return false
        } else {
            return when (oldItem) {
                is GalleryData -> (oldItem as GalleryData).id == (newItem as GalleryData).id
                is ImageData -> (oldItem as ImageData).id == (newItem as ImageData).id
                is MarkerData -> (oldItem as MarkerData).id == (newItem as MarkerData).id
                is MovieData -> (oldItem as MovieData).id == (newItem as MovieData).id
                is PerformerData -> (oldItem as PerformerData).id == (newItem as PerformerData).id
                is SlimSceneData -> (oldItem as SlimSceneData).id == (newItem as SlimSceneData).id
                is FullSceneData -> (oldItem as FullSceneData).id == (newItem as FullSceneData).id
                is StudioData -> (oldItem as StudioData).id == (newItem as StudioData).id
                is TagData -> (oldItem as TagData).id == (newItem as TagData).id
                else -> throw IllegalStateException("Cannot compare ${oldItem::class.java.name} and ${newItem::class.java.name}")
            }
        }
    }

    override fun areContentsTheSame(
        oldItem: Any,
        newItem: Any,
    ): Boolean {
        if (oldItem.javaClass != newItem.javaClass) {
            return false
        } else {
            return when (oldItem) {
                is GalleryData -> (oldItem as GalleryData) == (newItem as GalleryData)
                is ImageData -> (oldItem as ImageData) == (newItem as ImageData)
                is MarkerData -> (oldItem as MarkerData) == (newItem as MarkerData)
                is MovieData -> (oldItem as MovieData) == (newItem as MovieData)
                is PerformerData -> (oldItem as PerformerData) == (newItem as PerformerData)
                is SlimSceneData -> (oldItem as SlimSceneData) == (newItem as SlimSceneData)
                is FullSceneData -> (oldItem as FullSceneData) == (newItem as FullSceneData)
                is StudioData -> (oldItem as StudioData) == (newItem as StudioData)
                is TagData -> (oldItem as TagData) == (newItem as TagData)
                else -> throw IllegalStateException("Cannot compare ${oldItem::class.java.name} and ${newItem::class.java.name}")
            }
        }
    }
}

object SceneComparator : DiffUtil.ItemCallback<SlimSceneData>() {
    override fun areItemsTheSame(
        oldItem: SlimSceneData,
        newItem: SlimSceneData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: SlimSceneData,
        newItem: SlimSceneData,
    ): Boolean {
        return oldItem == newItem
    }
}

object PerformerComparator : DiffUtil.ItemCallback<PerformerData>() {
    override fun areItemsTheSame(
        oldItem: PerformerData,
        newItem: PerformerData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: PerformerData,
        newItem: PerformerData,
    ): Boolean {
        return oldItem == newItem
    }
}

object StudioComparator : DiffUtil.ItemCallback<StudioData>() {
    override fun areItemsTheSame(
        oldItem: StudioData,
        newItem: StudioData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: StudioData,
        newItem: StudioData,
    ): Boolean {
        return oldItem == newItem
    }
}

object TagComparator : DiffUtil.ItemCallback<TagData>() {
    override fun areItemsTheSame(
        oldItem: TagData,
        newItem: TagData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: TagData,
        newItem: TagData,
    ): Boolean {
        return oldItem == newItem
    }
}

object MovieComparator : DiffUtil.ItemCallback<MovieData>() {
    override fun areItemsTheSame(
        oldItem: MovieData,
        newItem: MovieData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: MovieData,
        newItem: MovieData,
    ): Boolean {
        return oldItem == newItem
    }
}

object MarkerComparator : DiffUtil.ItemCallback<MarkerData>() {
    override fun areItemsTheSame(
        oldItem: MarkerData,
        newItem: MarkerData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: MarkerData,
        newItem: MarkerData,
    ): Boolean {
        return oldItem == newItem
    }
}

object ImageComparator : DiffUtil.ItemCallback<ImageData>() {
    override fun areItemsTheSame(
        oldItem: ImageData,
        newItem: ImageData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: ImageData,
        newItem: ImageData,
    ): Boolean {
        return oldItem == newItem
    }
}

object GalleryComparator : DiffUtil.ItemCallback<GalleryData>() {
    override fun areItemsTheSame(
        oldItem: GalleryData,
        newItem: GalleryData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: GalleryData,
        newItem: GalleryData,
    ): Boolean {
        return oldItem == newItem
    }
}

object TagDiffCallback : DiffCallback<TagData>() {
    override fun areItemsTheSame(
        oldItem: TagData,
        newItem: TagData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: TagData,
        newItem: TagData,
    ): Boolean {
        return oldItem == newItem
    }
}

object MarkerDiffCallback : DiffCallback<MarkerData>() {
    override fun areItemsTheSame(
        oldItem: MarkerData,
        newItem: MarkerData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: MarkerData,
        newItem: MarkerData,
    ): Boolean {
        return oldItem == newItem
    }
}

object PerformerDiffCallback : DiffCallback<PerformerData>() {
    override fun areItemsTheSame(
        oldItem: PerformerData,
        newItem: PerformerData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: PerformerData,
        newItem: PerformerData,
    ): Boolean {
        return oldItem == newItem
    }
}

object MovieDiffCallback : DiffCallback<MovieData>() {
    override fun areItemsTheSame(
        oldItem: MovieData,
        newItem: MovieData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: MovieData,
        newItem: MovieData,
    ): Boolean {
        return oldItem == newItem
    }
}

object StudioDiffCallback : DiffCallback<StudioData>() {
    override fun areItemsTheSame(
        oldItem: StudioData,
        newItem: StudioData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: StudioData,
        newItem: StudioData,
    ): Boolean {
        return oldItem == newItem
    }
}

object GalleryDiffCallback : DiffCallback<GalleryData>() {
    override fun areItemsTheSame(
        oldItem: GalleryData,
        newItem: GalleryData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: GalleryData,
        newItem: GalleryData,
    ): Boolean {
        return oldItem == newItem
    }
}

object SceneDiffCallback : DiffCallback<SlimSceneData>() {
    override fun areItemsTheSame(
        oldItem: SlimSceneData,
        newItem: SlimSceneData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: SlimSceneData,
        newItem: SlimSceneData,
    ): Boolean {
        return oldItem == newItem
    }
}

object ImageDiffCallback : DiffCallback<ImageData>() {
    override fun areItemsTheSame(
        oldItem: ImageData,
        newItem: ImageData,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: ImageData,
        newItem: ImageData,
    ): Boolean {
        return oldItem == newItem
    }
}

object PlaylistItemComparator : DiffUtil.ItemCallback<PlaylistItem>() {
    override fun areItemsTheSame(
        oldItem: PlaylistItem,
        newItem: PlaylistItem,
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: PlaylistItem,
        newItem: PlaylistItem,
    ): Boolean {
        return oldItem == newItem
    }
}
