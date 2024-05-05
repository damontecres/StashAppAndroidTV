package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.CountPerformersQuery
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.data.DataType

class PerformerDataSupplier(
    private val findFilter: FindFilterType?,
    private val performerFilter: PerformerFilterType?,
) :
    StashPagingSource.DataSupplier<FindPerformersQuery.Data, PerformerData, CountPerformersQuery.Data> {
    constructor(performerFilter: PerformerFilterType? = null) : this(
        DataType.PERFORMER.asDefaultFindFilterType,
        performerFilter,
    )

    override val dataType: DataType get() = DataType.PERFORMER

    override fun createQuery(filter: FindFilterType?): Query<FindPerformersQuery.Data> {
        return FindPerformersQuery(
            filter = filter,
            performer_filter = performerFilter,
            performer_ids = null,
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: DataType.PERFORMER.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<CountPerformersQuery.Data> {
        return CountPerformersQuery(filter, performerFilter, null)
    }

    override fun parseCountQuery(data: CountPerformersQuery.Data): Int {
        return data.findPerformers.count
    }

    override fun parseQuery(data: FindPerformersQuery.Data): List<PerformerData> {
        return data.findPerformers.performers.map { it.performerData }
    }
}
