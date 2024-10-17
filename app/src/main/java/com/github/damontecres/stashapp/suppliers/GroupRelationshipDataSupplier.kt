package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountGroupsQuery
import com.github.damontecres.stashapp.api.FindGroupRelationshipsQuery
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.GroupRelationshipData
import com.github.damontecres.stashapp.data.GroupRelationshipType
import com.github.damontecres.stashapp.data.toRelationship

class GroupRelationshipDataSupplier(
    private val groupId: String,
    private val type: GroupRelationshipType,
) :
    StashPagingSource.DataSupplier<FindGroupRelationshipsQuery.Data, GroupRelationshipData, CountGroupsQuery.Data> {
    override val dataType: DataType get() = DataType.GROUP

    override fun createQuery(filter: FindFilterType?): Query<FindGroupRelationshipsQuery.Data> {
        return FindGroupRelationshipsQuery(
            filter = filter,
            group_filter = null,
            ids = listOf(groupId),
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return DataType.GROUP.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<CountGroupsQuery.Data> {
        return CountGroupsQuery(filter, null, listOf(groupId))
    }

    override fun parseCountQuery(data: CountGroupsQuery.Data): Int {
        return data.findGroups.count
    }

    override fun parseQuery(data: FindGroupRelationshipsQuery.Data): List<GroupRelationshipData> {
        return data.findGroups.groups.map {
            when (type) {
                GroupRelationshipType.SUB -> it.sub_groups.map { it.groupDescriptionData.toRelationship(type) }
                GroupRelationshipType.CONTAINING -> it.containing_groups.map { it.groupDescriptionData.toRelationship(type) }
            }
        }.flatten()
    }
}
