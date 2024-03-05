package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType

/**
 * A DataSupplier that returns the tags for a performer
 */
class PerformerTagDataSupplier(private val performerId: String) : StashPagingSource.DataSupplier<FindPerformersQuery.Data, TagData> {
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

    override fun parseQuery(data: FindPerformersQuery.Data?): CountAndList<TagData> {
        val tags = data?.findPerformers?.performers?.firstOrNull()?.performerData?.tags?.map { it.tagData }.orEmpty()
        return CountAndList(tags.size, tags)
    }
}
