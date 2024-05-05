package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType

/**
 * A DataSupplier that returns the tags for a performer
 */
class PerformerTagDataSupplier(private val performerId: String) :
    StashPagingSource.DataSupplier<FindPerformersQuery.Data, TagData, FindPerformersQuery.Data> {
    override val dataType: DataType
        get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindPerformersQuery.Data> {
        return FindPerformersQuery(
            filter = filter,
            performer_filter = null,
            performer_ids = listOf(performerId.toInt()),
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return DataType.TAG.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<FindPerformersQuery.Data> {
        return createQuery(filter)
    }

    override fun parseCountQuery(data: FindPerformersQuery.Data): Int {
        return data.findPerformers.performers.firstOrNull()?.performerData?.tags?.size ?: 0
    }

    override fun parseQuery(data: FindPerformersQuery.Data): List<TagData> {
        return data.findPerformers.performers.firstOrNull()?.performerData?.tags?.map { it.tagData }
            .orEmpty()
    }
}
