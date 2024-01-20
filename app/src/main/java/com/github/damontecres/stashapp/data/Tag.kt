package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.FindTagsQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.TagData
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(
    var id: Int,
    var name: String,
    var sceneCount: Int?,
    var performerCount: Int?,
    var imagePath: String?,
) : Parcelable

fun fromSlimSceneDataTag(sceneTag: SlimSceneData.Tag): Tag {
    return Tag(
        sceneTag.id.toInt(),
        sceneTag.name,
        sceneTag.scene_count,
        sceneTag.performer_count,
        sceneTag.image_path,
    )
}

fun fromFindTag(findTag: FindTagsQuery.Tag): Tag {
    return Tag(
        findTag.id.toInt(),
        findTag.name,
        findTag.scene_count,
        findTag.performer_count,
        findTag.image_path,
    )
}

fun fromTagData(tag: TagData): Tag {
    return Tag(
        tag.id.toInt(),
        tag.name,
        tag.scene_count,
        tag.performer_count,
        tag.image_path,
    )
}
