package com.github.damontecres.stashapp.suppliers

import android.os.Parcel
import android.os.Parcelable
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.FilterHolder
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.data.createFilterHolder
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.Version
import kotlinx.parcelize.Parcelize

/**
 * A factory to create [StashPagingSource.DataSupplier]s
 */
class DataSupplierFactory(val serverVersion: Version) {
    /**
     * Create a [StashPagingSource.DataSupplier] for the given [FilterArgs]
     *
     * If the [FilterArgs] has an override, that is always used.
     */
    fun <T : Query.Data, D : Any, C : Query.Data> create(args: FilterArgs): StashPagingSource.DataSupplier<T, D, C> {
        val filterParser = FilterParser(serverVersion)
        if (args.override != null) {
            return when (args.override) {
                is DataSupplierOverride.PerformerTags -> PerformerTagDataSupplier(args.override.performerId)
                is DataSupplierOverride.GalleryPerformer -> GalleryPerformerDataSupplier(args.override.galleryId)
                is DataSupplierOverride.GalleryTag -> GalleryTagDataSupplier(args.override.galleryId)
            } as StashPagingSource.DataSupplier<T, D, C>
        } else {
            return when (args.dataType) {
                DataType.SCENE ->
                    SceneDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertSceneObjectFilter(args.objectFilter),
                    )

                DataType.TAG ->
                    TagDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertTagObjectFilter(args.objectFilter),
                    )

                DataType.STUDIO ->
                    StudioDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertStudioObjectFilter(args.objectFilter),
                    )

                DataType.MARKER ->
                    MarkerDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertMarkerObjectFilter(args.objectFilter),
                    )

                DataType.IMAGE ->
                    ImageDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertImageObjectFilter(args.objectFilter),
                    )

                DataType.GALLERY ->
                    GalleryDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertGalleryObjectFilter(args.objectFilter),
                    )

                DataType.PERFORMER ->
                    PerformerDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertPerformerObjectFilter(args.objectFilter),
                    )

                DataType.MOVIE ->
                    MovieDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertMovieObjectFilter(args.objectFilter),
                    )
            } as StashPagingSource.DataSupplier<T, D, C>
        }
    }
}

/**
 * Represents a [StashPagingSource.DataSupplier] that is atypical.
 *
 * This usually means filtering based on the ID of one [DataType] but querying for a different [DataType] such as the tags on a performer.
 */
@Parcelize
sealed class DataSupplierOverride : Parcelable {
    data class PerformerTags(val performerId: String) : DataSupplierOverride()

    data class GalleryPerformer(val galleryId: String) : DataSupplierOverride()

    data class GalleryTag(val galleryId: String) : DataSupplierOverride()
}

/**
 * Represents a filter that can be used to create a [StashPagingSource.DataSupplier].
 *
 * Optionally, a find filter and/or object filter can be provided, otherwise they will default the data supplier's implementation.
 *
 * Optionally, a [DataSupplierOverride] can be provided which always overrides which data supplier to use.
 */
data class FilterArgs(
    val dataType: DataType,
    val findFilter: StashFindFilter? = null,
    val objectFilter: Any? = null,
    val override: DataSupplierOverride? = null,
) : Parcelable {
    val sortAndDirection: SortAndDirection
        get() {
            return if (findFilter?.sortAndDirection != null) {
                SortAndDirection(
                    findFilter.sortAndDirection.sort,
                    findFilter.sortAndDirection.direction,
                )
            } else {
                dataType.defaultSort
            }
        }

    /**
     * Returns this [FilterArgs] with the specified [SortAndDirection]
     */
    fun with(newSortAndDirection: SortAndDirection): FilterArgs =
        this.copy(
            findFilter =
                this.findFilter?.copy(sortAndDirection = newSortAndDirection)
                    ?: StashFindFilter(sortAndDirection = newSortAndDirection),
        )

    /**
     * Returns a copy of this object which guarantees that the object filter is a FilterType object and not a Map<String, *>
     *
     * This means that the returned [FilterArgs] is parcelable
     */
    fun ensureParsed(filterParser: FilterParser): FilterArgs {
        val objectFilter =
            when (dataType) {
                DataType.TAG -> filterParser.convertTagObjectFilter(this.objectFilter)
                DataType.STUDIO -> filterParser.convertStudioObjectFilter(this.objectFilter)
                DataType.MOVIE -> filterParser.convertMovieObjectFilter(this.objectFilter)
                DataType.SCENE -> filterParser.convertSceneObjectFilter(this.objectFilter)
                DataType.IMAGE -> filterParser.convertImageObjectFilter(this.objectFilter)
                DataType.GALLERY -> filterParser.convertGalleryObjectFilter(this.objectFilter)
                DataType.MARKER -> filterParser.convertMarkerObjectFilter(this.objectFilter)
                DataType.PERFORMER -> filterParser.convertPerformerObjectFilter(this.objectFilter)
            }
        return this.copy(objectFilter = objectFilter)
    }

    override fun writeToParcel(
        parcel: Parcel,
        flags: Int,
    ) {
        parcel.writeInt(dataType.ordinal)
        parcel.writeParcelable(findFilter, flags)
        parcel.writeParcelable(createFilterHolder(objectFilter), flags)
        parcel.writeParcelable(override, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FilterArgs> {
        override fun createFromParcel(parcel: Parcel): FilterArgs {
            val dataType = DataType.entries[parcel.readInt()]
            val findFilter: StashFindFilter? =
                parcel.readParcelable(StashFindFilter::class.java.classLoader)
            val objectFilterHolder: FilterHolder<Any?>? =
                parcel.readParcelable(FilterHolder::class.java.classLoader)
            val override: DataSupplierOverride? =
                parcel.readParcelable(DataSupplierOverride::class.java.classLoader)
            return FilterArgs(dataType, findFilter, objectFilterHolder?.value, override)
        }

        override fun newArray(size: Int): Array<FilterArgs?> {
            return arrayOfNulls(size)
        }
    }
}
