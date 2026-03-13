package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountGroupRelationshipsQuery
import com.github.damontecres.stashapp.api.FindGroupRelationshipsQuery
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.GroupDescriptionData
import com.github.damontecres.stashapp.api.fragment.GroupRelationshipData
import com.github.damontecres.stashapp.api.fragment.GroupRelationshipType
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.data.DataType

class GroupRelationshipDataSupplier(
    private val groupId: String,
    private val type: GroupRelationshipType,
) : StashPagingSource.DataSupplier<FindGroupRelationshipsQuery.Data, GroupRelationshipData, CountGroupRelationshipsQuery.Data> {
    override val dataType: DataType get() = DataType.GROUP

    override fun createQuery(filter: FindFilterType?): Query<FindGroupRelationshipsQuery.Data> =
        when (type) {
            GroupRelationshipType.CONTAINING -> {
                FindGroupRelationshipsQuery(
                    filter = filter,
                    group_filter =
                        GroupFilterType(
                            sub_groups =
                                Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(groupId)),
                                        modifier = CriterionModifier.INCLUDES,
                                        depth = Optional.present(1),
                                    ),
                                ),
                        ),
                    ids = null,
                )
            }

            GroupRelationshipType.SUB -> {
                FindGroupRelationshipsQuery(
                    filter = filter,
                    group_filter =
                        GroupFilterType(
                            containing_groups =
                                Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(groupId)),
                                        modifier = CriterionModifier.INCLUDES,
                                        depth = Optional.present(1),
                                    ),
                                ),
                        ),
                    ids = null,
                )
            }
        }

    override fun getDefaultFilter(): FindFilterType = DataType.GROUP.asDefaultFindFilterType

    override fun createCountQuery(filter: FindFilterType?): Query<CountGroupRelationshipsQuery.Data> =
        CountGroupRelationshipsQuery(filter, null, listOf(groupId))

    override fun parseCountQuery(data: CountGroupRelationshipsQuery.Data): Int =
        when (type) {
            GroupRelationshipType.SUB -> {
                data.findGroups.groups
                    .firstOrNull()
                    ?.sub_group_count
            }

            GroupRelationshipType.CONTAINING -> {
                data.findGroups.groups
                    .firstOrNull()
                    ?.containing_groups
                    ?.size
            }
        } ?: 0

    override fun parseQuery(data: FindGroupRelationshipsQuery.Data): List<GroupRelationshipData> =
        data.findGroups.groups
            .mapNotNull { group ->
                when (type) {
                    GroupRelationshipType.CONTAINING -> {
                        group.sub_groups
                            .firstOrNull { it.groupDescriptionData.group.groupData.id == groupId }
                            ?.groupDescriptionData
                            ?.let { toRelationship(group.groupData, it) }
                    }

                    GroupRelationshipType.SUB -> {
                        group.containing_groups
                            .firstOrNull { it.groupDescriptionData.group.groupData.id == groupId }
                            ?.groupDescriptionData
                            ?.let { toRelationship(group.groupData, it) }
                    }
                }
            }

    private fun toRelationship(
        group: GroupData,
        groupDescriptionData: GroupDescriptionData,
    ): GroupRelationshipData =
        GroupRelationshipData(
            group,
            type,
            groupDescriptionData.description,
        )
}
