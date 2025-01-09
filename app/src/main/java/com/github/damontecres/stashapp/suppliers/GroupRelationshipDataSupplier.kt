package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountGroupRelationshipsQuery
import com.github.damontecres.stashapp.api.FindGroupRelationshipsQuery
import com.github.damontecres.stashapp.api.fragment.GroupRelationshipData
import com.github.damontecres.stashapp.api.fragment.GroupRelationshipType
import com.github.damontecres.stashapp.api.fragment.toRelationship
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType

class GroupRelationshipDataSupplier(
    private val groupId: String,
    private val type: GroupRelationshipType,
) : StashPagingSource.DataSupplier<FindGroupRelationshipsQuery.Data, GroupRelationshipData, CountGroupRelationshipsQuery.Data> {
    override val dataType: DataType get() = DataType.GROUP

    override fun createQuery(filter: FindFilterType?): Query<FindGroupRelationshipsQuery.Data> =
        FindGroupRelationshipsQuery(
            filter = filter,
            group_filter = null,
            ids = listOf(groupId),
        )

    override fun getDefaultFilter(): FindFilterType = DataType.GROUP.asDefaultFindFilterType

    override fun createCountQuery(filter: FindFilterType?): Query<CountGroupRelationshipsQuery.Data> =
        CountGroupRelationshipsQuery(filter, null, listOf(groupId))

    override fun parseCountQuery(data: CountGroupRelationshipsQuery.Data): Int =
        when (type) {
            GroupRelationshipType.SUB ->
                data.findGroups.groups
                    .firstOrNull()
                    ?.sub_group_count
            GroupRelationshipType.CONTAINING ->
                data.findGroups.groups
                    .firstOrNull()
                    ?.containing_groups
                    ?.size
        } ?: 0

    override fun parseQuery(data: FindGroupRelationshipsQuery.Data): List<GroupRelationshipData> =
        data.findGroups.groups
            .map {
                when (type) {
                    GroupRelationshipType.SUB ->
                        it.sub_groups.map { subGroup ->
                            subGroup.groupDescriptionData.toRelationship(
                                type,
                            )
                        }

                    GroupRelationshipType.CONTAINING ->
                        it.containing_groups.map { containingGroup ->
                            containingGroup.groupDescriptionData.toRelationship(
                                type,
                            )
                        }
                }
            }.flatten()
}
