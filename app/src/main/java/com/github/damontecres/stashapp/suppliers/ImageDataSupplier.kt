package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountImagesQuery
import com.github.damontecres.stashapp.api.FindImagesQuery
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.data.DataType

class ImageDataSupplier(
    private val findFilter: FindFilterType?,
    private val imageFilter: ImageFilterType?,
) :
    StashPagingSource.DataSupplier<FindImagesQuery.Data, ImageData, CountImagesQuery.Data> {
    constructor(imageFilter: ImageFilterType? = null) : this(
        DataType.IMAGE.asDefaultFindFilterType,
        imageFilter,
    )

    override val dataType: DataType get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindImagesQuery.Data> {
        return FindImagesQuery(
            filter = filter,
            image_filter = imageFilter,
            ids = null,
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: DataType.IMAGE.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<CountImagesQuery.Data> {
        return CountImagesQuery(filter, imageFilter)
    }

    override fun parseCountQuery(data: CountImagesQuery.Data): Int {
        return data.findImages.count
    }

    override fun parseQuery(data: FindImagesQuery.Data): List<ImageData> {
        return data.findImages.images.map { it.imageData }
    }
}
