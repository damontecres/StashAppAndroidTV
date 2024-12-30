package com.github.damontecres.stashapp.api.fragment

enum class GroupRelationshipType {
    CONTAINING,
    SUB,
}

data class GroupRelationshipData(
    val group: GroupData,
    val type: GroupRelationshipType,
    val description: String?,
) : StashData {
    override val id: String = group.id
}

fun GroupDescriptionData.toRelationship(type: GroupRelationshipType): GroupRelationshipData =
    GroupRelationshipData(
        group.groupData,
        type,
        description,
    )
