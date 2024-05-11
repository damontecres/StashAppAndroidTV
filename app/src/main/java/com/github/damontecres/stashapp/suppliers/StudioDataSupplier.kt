package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.CountStudiosQuery
import com.github.damontecres.stashapp.api.FindStudiosQuery
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.data.DataType

class StudioDataSupplier(
    private val findFilter: FindFilterType?,
    private val studioFilter: StudioFilterType?,
) :
    StashPagingSource.DataSupplier<FindStudiosQuery.Data, StudioData, CountStudiosQuery.Data> {
    constructor(studioFilter: StudioFilterType? = null) : this(
        DataType.STUDIO.asDefaultFindFilterType,
        studioFilter,
    )

    override val dataType: DataType get() = DataType.STUDIO

    override fun createQuery(filter: FindFilterType?): Query<FindStudiosQuery.Data> {
        return FindStudiosQuery(
            filter = filter,
            studio_filter = studioFilter,
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: DataType.STUDIO.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<CountStudiosQuery.Data> {
        return CountStudiosQuery(filter, studioFilter)
    }

    override fun parseCountQuery(data: CountStudiosQuery.Data): Int {
        return data.findStudios.count
    }

    override fun parseQuery(data: FindStudiosQuery.Data): List<StudioData> {
        return data.findStudios.studios.map { it.studioData }
    }
}
