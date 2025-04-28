package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountTagsQuery
import com.github.damontecres.stashapp.api.FindTagsQuery
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.merge

class TagDataSupplier(
    private val findFilter: FindFilterType?,
    private val tagFilter: TagFilterType?,
) : StashPagingSource.DataSupplier<FindTagsQuery.Data, TagData, CountTagsQuery.Data> {
    override val dataType: DataType get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindTagsQuery.Data> =
        FindTagsQuery(
            filter = filter,
            tag_filter = tagFilter,
            ids = null,
        )

    override fun getDefaultFilter(): FindFilterType = DataType.TAG.asDefaultFindFilterType.merge(findFilter)

    override fun createCountQuery(filter: FindFilterType?): Query<CountTagsQuery.Data> = CountTagsQuery(filter, tagFilter)

    override fun parseCountQuery(data: CountTagsQuery.Data): Int = data.findTags.count

    override fun parseQuery(data: FindTagsQuery.Data): List<TagData> = data.findTags.tags.map { it.tagData }
}
