package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountGalleriesQuery
import com.github.damontecres.stashapp.api.FindGalleriesQuery
import com.github.damontecres.stashapp.api.FindGalleryPerformersQuery
import com.github.damontecres.stashapp.api.FindGalleryTagsQuery
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.data.DataType

class GalleryDataSupplier(
    private val findFilter: FindFilterType?,
    private val galleryFilter: GalleryFilterType?,
) :
    StashPagingSource.DataSupplier<FindGalleriesQuery.Data, GalleryData, CountGalleriesQuery.Data> {
    constructor(galleryFilter: GalleryFilterType? = null) : this(
        DataType.GALLERY.asDefaultFindFilterType,
        galleryFilter,
    )

    override val dataType: DataType get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindGalleriesQuery.Data> {
        return FindGalleriesQuery(
            filter = filter,
            gallery_filter = galleryFilter,
            ids = null,
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: DataType.GALLERY.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<CountGalleriesQuery.Data> {
        return CountGalleriesQuery(filter, galleryFilter)
    }

    override fun parseCountQuery(data: CountGalleriesQuery.Data): Int {
        return data.findGalleries.count
    }

    override fun parseQuery(data: FindGalleriesQuery.Data): List<GalleryData> {
        return data.findGalleries.galleries.map { it.galleryData }
    }
}

class GalleryPerformerDataSupplier(private val galleryId: String) :
    StashPagingSource.DataSupplier<FindGalleryPerformersQuery.Data, PerformerData, FindGalleryPerformersQuery.Data> {
    override val dataType: DataType
        get() = DataType.PERFORMER

    override fun createQuery(filter: FindFilterType?): Query<FindGalleryPerformersQuery.Data> {
        return FindGalleryPerformersQuery(galleryId)
    }

    override fun getDefaultFilter(): FindFilterType {
        return DataType.PERFORMER.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<FindGalleryPerformersQuery.Data> {
        return FindGalleryPerformersQuery(galleryId)
    }

    override fun parseCountQuery(data: FindGalleryPerformersQuery.Data): Int {
        return data.findGallery?.performers?.size ?: 0
    }

    override fun parseQuery(data: FindGalleryPerformersQuery.Data): List<PerformerData> {
        return data.findGallery?.performers?.map { it.performerData }.orEmpty()
    }
}

class GalleryTagDataSupplier(private val galleryId: String) :
    StashPagingSource.DataSupplier<FindGalleryTagsQuery.Data, TagData, FindGalleryTagsQuery.Data> {
    override val dataType: DataType
        get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindGalleryTagsQuery.Data> {
        return FindGalleryTagsQuery(galleryId)
    }

    override fun getDefaultFilter(): FindFilterType {
        return DataType.TAG.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<FindGalleryTagsQuery.Data> {
        return FindGalleryTagsQuery(galleryId)
    }

    override fun parseCountQuery(data: FindGalleryTagsQuery.Data): Int {
        return data.findGallery?.tags?.size ?: 0
    }

    override fun parseQuery(data: FindGalleryTagsQuery.Data): List<TagData> {
        return data.findGallery?.tags?.map { it.tagData }.orEmpty()
    }
}
