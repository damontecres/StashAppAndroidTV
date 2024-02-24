package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType

class PerformerDataSupplier(
    private val findFilter: FindFilterType?,
    private val performerFilter: PerformerFilterType?,
) :
    StashPagingSource.DataSupplier<FindPerformersQuery.Data, PerformerData> {
    override val dataType: DataType get() = DataType.PERFORMER

    override fun createQuery(filter: FindFilterType?): Query<FindPerformersQuery.Data> {
        return FindPerformersQuery(
            filter = filter,
            performer_filter = performerFilter,
            performer_ids = null,
            ids = null,
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: FindFilterType()
    }

    override fun parseQuery(data: FindPerformersQuery.Data?): CountAndList<PerformerData> {
        val count = data?.findPerformers?.count ?: -1
        val performers =
            data?.findPerformers?.performers?.map {
                it.performerData
            }.orEmpty()
        return CountAndList(count, performers)
    }
}
