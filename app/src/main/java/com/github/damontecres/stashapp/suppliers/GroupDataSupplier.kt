package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountGroupsQuery
import com.github.damontecres.stashapp.api.FindGroupTagsQuery
import com.github.damontecres.stashapp.api.FindGroupsQuery
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.data.DataType

class GroupDataSupplier(
    private val findFilter: FindFilterType?,
    private val groupFilter: GroupFilterType?,
) :
    StashPagingSource.DataSupplier<FindGroupsQuery.Data, GroupData, CountGroupsQuery.Data> {
    constructor(groupFilter: GroupFilterType? = null) : this(
        DataType.GROUP.asDefaultFindFilterType,
        groupFilter,
    )

    override val dataType: DataType get() = DataType.PERFORMER

    override fun createQuery(filter: FindFilterType?): Query<FindGroupsQuery.Data> {
        return FindGroupsQuery(
            filter = filter,
            group_filter = groupFilter,
            ids = null,
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: DataType.GROUP.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<CountGroupsQuery.Data> {
        return CountGroupsQuery(filter, groupFilter)
    }

    override fun parseCountQuery(data: CountGroupsQuery.Data): Int {
        return data.findGroups.count
    }

    override fun parseQuery(data: FindGroupsQuery.Data): List<GroupData> {
        return data.findGroups.groups.map { it.groupData }
    }
}

/**
 * A DataSupplier that returns the tags for a group
 */
class GroupTagDataSupplier(private val groupId: String) :
    StashPagingSource.DataSupplier<FindGroupTagsQuery.Data, TagData, FindGroupTagsQuery.Data> {
    override val dataType: DataType
        get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindGroupTagsQuery.Data> {
        return FindGroupTagsQuery(groupId)
    }

    override fun getDefaultFilter(): FindFilterType {
        return DataType.TAG.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<FindGroupTagsQuery.Data> {
        return createQuery(filter)
    }

    override fun parseCountQuery(data: FindGroupTagsQuery.Data): Int {
        return if (data.findGroup != null) {
            1
        } else {
            0
        }
    }

    override fun parseQuery(data: FindGroupTagsQuery.Data): List<TagData> {
        return data.findGroup?.tags?.map { it.tagData }.orEmpty()
    }
}
