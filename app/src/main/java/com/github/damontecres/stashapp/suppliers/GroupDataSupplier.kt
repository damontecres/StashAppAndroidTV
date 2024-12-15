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
) : StashPagingSource.DataSupplier<FindGroupsQuery.Data, GroupData, CountGroupsQuery.Data> {
    override val dataType: DataType get() = DataType.GROUP

    override fun createQuery(filter: FindFilterType?): Query<FindGroupsQuery.Data> =
        FindGroupsQuery(
            filter = filter,
            group_filter = groupFilter,
            ids = null,
        )

    override fun getDefaultFilter(): FindFilterType = findFilter ?: DataType.GROUP.asDefaultFindFilterType

    override fun createCountQuery(filter: FindFilterType?): Query<CountGroupsQuery.Data> = CountGroupsQuery(filter, groupFilter, null)

    override fun parseCountQuery(data: CountGroupsQuery.Data): Int = data.findGroups.count

    override fun parseQuery(data: FindGroupsQuery.Data): List<GroupData> = data.findGroups.groups.map { it.groupData }
}

/**
 * A DataSupplier that returns the tags for a group
 */
class GroupTagDataSupplier(
    private val groupId: String,
) : StashPagingSource.DataSupplier<FindGroupTagsQuery.Data, TagData, FindGroupTagsQuery.Data> {
    override val dataType: DataType
        get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindGroupTagsQuery.Data> = FindGroupTagsQuery(groupId)

    override fun getDefaultFilter(): FindFilterType = DataType.TAG.asDefaultFindFilterType

    override fun createCountQuery(filter: FindFilterType?): Query<FindGroupTagsQuery.Data> = createQuery(filter)

    override fun parseCountQuery(data: FindGroupTagsQuery.Data): Int = data.findGroup?.tags?.size ?: 0

    override fun parseQuery(data: FindGroupTagsQuery.Data): List<TagData> =
        data.findGroup
            ?.tags
            ?.map { it.tagData }
            .orEmpty()
}
