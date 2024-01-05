package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindStudiosQuery
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.StashPagingSource

class StudioDataSupplier(
    private val findFilter: FindFilterType?,
    private val studioFilter: StudioFilterType?
) :
    StashPagingSource.DataSupplier<FindStudiosQuery.Data, StudioData> {

    override val dataType: DataType get() = DataType.STUDIO

    override fun createQuery(filter: FindFilterType?): Query<FindStudiosQuery.Data> {
        return FindStudiosQuery(
            filter = filter,
            studio_filter = studioFilter
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: FindFilterType()
    }

    override fun parseQuery(data: FindStudiosQuery.Data?): CountAndList<StudioData> {
        val count = data?.findStudios?.count ?: -1
        val studios = data?.findStudios?.studios?.map {
            it.studioData
        }.orEmpty()
        return CountAndList(count, studios)
    }
}