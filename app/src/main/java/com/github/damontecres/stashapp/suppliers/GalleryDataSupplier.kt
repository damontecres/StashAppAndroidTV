package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindGalleriesQuery
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType

class GalleryDataSupplier(
    private val findFilter: FindFilterType?,
    private val galleryFilter: GalleryFilterType?,
) :
    StashPagingSource.DataSupplier<FindGalleriesQuery.Data, GalleryData> {
    constructor(galleryFilter: GalleryFilterType? = null) : this(
        DataType.GALLERY.asDefaultFindFilterType,
        galleryFilter,
    )

    override val dataType: DataType get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindGalleriesQuery.Data> {
        return FindGalleriesQuery(
            filter = filter,
            gallery_filter = galleryFilter,
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: DataType.GALLERY.asDefaultFindFilterType
    }

    override fun parseQuery(data: FindGalleriesQuery.Data?): CountAndList<GalleryData> {
        val count = data?.findGalleries?.count ?: -1
        val images =
            data?.findGalleries?.galleries?.map { it.galleryData }.orEmpty()
        return CountAndList(count, images)
    }
}
