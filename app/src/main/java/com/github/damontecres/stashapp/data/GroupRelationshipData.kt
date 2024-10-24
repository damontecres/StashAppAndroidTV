package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.GroupDescriptionData

enum class GroupRelationshipType {
    CONTAINING,
    SUB,
}

data class GroupRelationshipData(val group: GroupData, val type: GroupRelationshipType, val description: String?) : StashData {
    override val id: String = group.id
}

fun GroupDescriptionData.toRelationship(type: GroupRelationshipType): GroupRelationshipData {
    return GroupRelationshipData(
        group.groupData,
        type,
        description,
    )
}
