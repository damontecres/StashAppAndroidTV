package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindImagesQuery
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType

class ImageDataSupplier(
    private val findFilter: FindFilterType?,
    private val imageFilter: ImageFilterType?,
) :
    StashPagingSource.DataSupplier<FindImagesQuery.Data, ImageData> {
    override val dataType: DataType get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindImagesQuery.Data> {
        return FindImagesQuery(
            filter = filter,
            image_filter = imageFilter,
            image_ids = emptyList(),
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: FindFilterType()
    }

    override fun parseQuery(data: FindImagesQuery.Data?): CountAndList<ImageData> {
        val count = data?.findImages?.count ?: -1
        val images =
            data?.findImages?.images?.map { it.imageData }.orEmpty()
        return CountAndList(count, images)
    }
}
