package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountStudiosQuery
import com.github.damontecres.stashapp.api.FindStudioTagsQuery
import com.github.damontecres.stashapp.api.FindStudiosQuery
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.merge

class StudioDataSupplier(
    private val findFilter: FindFilterType?,
    private val studioFilter: StudioFilterType?,
) : StashPagingSource.DataSupplier<FindStudiosQuery.Data, StudioData, CountStudiosQuery.Data> {
    override val dataType: DataType get() = DataType.STUDIO

    override fun createQuery(filter: FindFilterType?): Query<FindStudiosQuery.Data> =
        FindStudiosQuery(
            filter = filter,
            studio_filter = studioFilter,
            ids = null,
        )

    override fun getDefaultFilter(): FindFilterType = DataType.STUDIO.asDefaultFindFilterType.merge(findFilter)

    override fun createCountQuery(filter: FindFilterType?): Query<CountStudiosQuery.Data> = CountStudiosQuery(filter, studioFilter)

    override fun parseCountQuery(data: CountStudiosQuery.Data): Int = data.findStudios.count

    override fun parseQuery(data: FindStudiosQuery.Data): List<StudioData> = data.findStudios.studios.map { it.studioData }
}

/**
 * A DataSupplier that returns the tags for a studio
 */
class StudioTagDataSupplier(
    private val studioId: String,
) : StashPagingSource.DataSupplier<FindStudioTagsQuery.Data, TagData, FindStudioTagsQuery.Data> {
    override val dataType: DataType
        get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindStudioTagsQuery.Data> = FindStudioTagsQuery(studioId)

    override fun getDefaultFilter(): FindFilterType = DataType.TAG.asDefaultFindFilterType

    override fun createCountQuery(filter: FindFilterType?): Query<FindStudioTagsQuery.Data> = createQuery(filter)

    override fun parseCountQuery(data: FindStudioTagsQuery.Data): Int = data.findStudio?.tags?.size ?: 0

    override fun parseQuery(data: FindStudioTagsQuery.Data): List<TagData> =
        data.findStudio
            ?.tags
            ?.map { it.tagData }
            .orEmpty()
}
