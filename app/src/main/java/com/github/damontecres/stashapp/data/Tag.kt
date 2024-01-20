package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.TagData
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(
    var id: Int,
    var name: String,
    var sceneCount: Int?,
    var performerCount: Int?,
    var imagePath: String?,
) : Parcelable {
    constructor(tag: TagData) : this(
        tag.id.toInt(),
        tag.name,
        tag.scene_count,
        tag.performer_count,
        tag.image_path,
    )
}
