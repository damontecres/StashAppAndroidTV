package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountTagsQuery
import com.github.damontecres.stashapp.api.FindTagsQuery
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType

class TagDataSupplier(
    private val findFilter: FindFilterType?,
    private val tagFilter: TagFilterType?,
) :
    StashPagingSource.DataSupplier<FindTagsQuery.Data, TagData, CountTagsQuery.Data> {
    override val dataType: DataType get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindTagsQuery.Data> {
        return FindTagsQuery(
            filter = filter,
            tag_filter = tagFilter,
            ids = null,
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: DataType.TAG.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<CountTagsQuery.Data> {
        return CountTagsQuery(filter, tagFilter)
    }

    override fun parseCountQuery(data: CountTagsQuery.Data): Int {
        return data.findTags.count
    }

    override fun parseQuery(data: FindTagsQuery.Data): List<TagData> {
        return data.findTags.tags.map { it.tagData }
    }
}
