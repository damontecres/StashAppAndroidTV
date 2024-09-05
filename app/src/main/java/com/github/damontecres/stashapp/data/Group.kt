package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.GroupData
import kotlinx.parcelize.Parcelize

@Parcelize
data class Group(
    val id: String,
    val name: String,
    val aliases: String?,
    val studioId: String?,
    val frontImagePath: String?,
    val backImagePath: String?,
) : Parcelable {
    constructor(group: GroupData) : this(
        group.id,
        group.name,
        group.aliases,
        group.studio?.id,
        group.front_image_path,
        group.back_image_path,
    )
}
