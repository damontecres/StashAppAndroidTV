package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindTagsQuery
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Tag
import com.github.damontecres.stashapp.data.fromFindTag

class TagDataSupplier(
    private val findFilter: FindFilterType?,
    private val tagFilter: TagFilterType?,
) :
    StashPagingSource.DataSupplier<FindTagsQuery.Data, Tag> {
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

    override fun parseQuery(data: FindTagsQuery.Data?): CountAndList<Tag> {
        val count = data?.findTags?.count ?: -1
        val studios =
            data?.findTags?.tags?.map {
                fromFindTag(it)
            }.orEmpty()
        return CountAndList(count, studios)
    }
}
