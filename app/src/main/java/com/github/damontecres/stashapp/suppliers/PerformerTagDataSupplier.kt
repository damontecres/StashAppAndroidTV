package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.FindPerformerTagsQuery
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType

/**
 * A DataSupplier that returns the tags for a performer
 */
class PerformerTagDataSupplier(private val performerId: String) :
    StashPagingSource.DataSupplier<FindPerformerTagsQuery.Data, TagData, FindPerformerTagsQuery.Data> {
    override val dataType: DataType
        get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindPerformerTagsQuery.Data> {
        return FindPerformerTagsQuery(
            filter = filter,
            performer_filter = null,
            ids = listOf(performerId),
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return DataType.TAG.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<FindPerformerTagsQuery.Data> {
        return createQuery(filter)
    }

    override fun parseCountQuery(data: FindPerformerTagsQuery.Data): Int {
        return data.findPerformers.performers.firstOrNull()?.tags?.size ?: 0
    }

    override fun parseQuery(data: FindPerformerTagsQuery.Data): List<TagData> {
        return data.findPerformers.performers.firstOrNull()?.tags?.map { it.tagData }.orEmpty()
    }
}
