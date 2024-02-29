package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindTagsQuery
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType

class TagDataSupplier(
    private val findFilter: FindFilterType?,
    private val tagFilter: TagFilterType?,
) :
    StashPagingSource.DataSupplier<FindTagsQuery.Data, TagData> {
    constructor(tagFilter: TagFilterType? = null) : this(
        DataType.TAG.asDefaultFindFilterType,
        tagFilter,
    )

    override val dataType: DataType get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindTagsQuery.Data> {
        return FindTagsQuery(
            filter = filter,
            tag_filter = tagFilter,
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: FindFilterType()
    }

    override fun parseQuery(data: FindTagsQuery.Data?): CountAndList<TagData> {
        val count = data?.findTags?.count ?: -1
        val studios =
            data?.findTags?.tags?.map { it.tagData }.orEmpty()
        return CountAndList(count, studios)
    }
}
