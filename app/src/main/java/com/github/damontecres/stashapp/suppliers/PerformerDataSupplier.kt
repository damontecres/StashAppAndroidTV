package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.presenters.StashPagingSource

class PerformerDataSupplier(private val performerFilter: PerformerFilterType?) :
    StashPagingSource.DataSupplier<FindPerformersQuery.Data, PerformerData> {

    constructor() : this(null) {
    }

    override fun createQuery(filter: FindFilterType?): Query<FindPerformersQuery.Data> {
        return FindPerformersQuery(
            filter = Optional.presentIfNotNull(filter),
            performer_filter = Optional.presentIfNotNull(performerFilter)
        )
    }

    override fun parseQuery(data: FindPerformersQuery.Data?): CountAndList<PerformerData> {
        val count = data?.findPerformers?.count ?: -1
        val performers = data?.findPerformers?.performers?.map {
            it.performerData
        }.orEmpty()
        return CountAndList(count, performers)
    }
}