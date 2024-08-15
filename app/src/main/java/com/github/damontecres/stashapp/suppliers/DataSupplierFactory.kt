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

class DataSupplierFactory(val serverVersion: Version) {
    fun <T : Query.Data, D : Any, C : Query.Data> create(args: FilterArgs): StashPagingSource.DataSupplier<T, D, C> {
        if (args.override != null) {
            TODO()
        } else {
            val filterParser = FilterParser(serverVersion)
            return when (args.dataType) {
                DataType.SCENE ->
                    SceneDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertSceneObjectFilter(args.objectFilter),
                    ) as StashPagingSource.DataSupplier<T, D, C>

                DataType.TAG ->
                    TagDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertTagObjectFilter(args.objectFilter),
                    ) as StashPagingSource.DataSupplier<T, D, C>

                DataType.STUDIO ->
                    StudioDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertStudioObjectFilter(args.objectFilter),
                    ) as StashPagingSource.DataSupplier<T, D, C>

                DataType.MARKER ->
                    MarkerDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertMarkerObjectFilter(args.objectFilter),
                    ) as StashPagingSource.DataSupplier<T, D, C>

                DataType.IMAGE ->
                    ImageDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertImageObjectFilter(args.objectFilter),
                    ) as StashPagingSource.DataSupplier<T, D, C>

                DataType.GALLERY ->
                    GalleryDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertGalleryObjectFilter(args.objectFilter),
                    ) as StashPagingSource.DataSupplier<T, D, C>

                DataType.PERFORMER ->
                    PerformerDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertPerformerObjectFilter(args.objectFilter),
                    ) as StashPagingSource.DataSupplier<T, D, C>

                DataType.MOVIE ->
                    MovieDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertMovieObjectFilter(args.objectFilter),
                    ) as StashPagingSource.DataSupplier<T, D, C>
            }
        }
    }
}

enum class DataSupplierOverride {
    PERFORMER_TAGS,
}

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
        parcel.writeInt(override?.ordinal ?: -1)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FilterArgs> {
        override fun createFromParcel(parcel: Parcel): FilterArgs {
            val dataType = DataType.entries[parcel.readInt()]
            val findFilter: StashFindFilter? =
                parcel.readParcelable(StashFindFilter::class.java.classLoader)
            val objectFilterHolder: FilterHolder<Any?> =
                parcel.readParcelable(FilterHolder::class.java.classLoader)!!
            val overrideIndex = parcel.readInt()
            val override =
                if (overrideIndex >= 0) {
                    DataSupplierOverride.entries[overrideIndex]
                } else {
                    null
                }
            return FilterArgs(dataType, findFilter, objectFilterHolder.value, override)
        }

        override fun newArray(size: Int): Array<FilterArgs?> {
            return arrayOfNulls(size)
        }
    }
}
