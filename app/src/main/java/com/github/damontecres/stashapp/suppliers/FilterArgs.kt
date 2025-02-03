package com.github.damontecres.stashapp.suppliers

import com.github.damontecres.stashapp.api.fragment.SavedFilter
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.getRandomSort
import kotlinx.serialization.Serializable

/**
 * Represents a filter that can be used to create a [StashPagingSource.DataSupplier].
 *
 * Optionally, a find filter and/or object filter can be provided, otherwise they will default the data supplier's implementation.
 *
 * Optionally, a [DataSupplierOverride] can be provided which always overrides which data supplier to use.
 */
@Serializable
data class FilterArgs(
    val dataType: DataType,
    val name: String? = null,
    val findFilter: StashFindFilter? = null,
    val objectFilter: StashDataFilter? = null,
    val override: DataSupplierOverride? = null,
) {
    val sortAndDirection: SortAndDirection
        get() {
            return findFilter?.sortAndDirection
                ?: dataType.defaultSort
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
     * If the [sortAndDirection] is random, resolve it and return an updated [FilterArgs]
     */
    fun withResolvedRandom(): FilterArgs =
        if (sortAndDirection.isRandom) {
            with(sortAndDirection.copy(randomSeed = getRandomSort()))
        } else {
            this
        }

    fun withQuery(query: String?): FilterArgs =
        this.copy(
            findFilter =
                this.findFilter?.copy(q = query)
                    ?: StashFindFilter(q = query),
        )
}

fun SavedFilter.toFilterArgs(filterParser: FilterParser): FilterArgs {
    val dataType = DataType.fromFilterMode(mode)!!
    val findFilter =
        if (find_filter != null) {
            StashFindFilter(
                find_filter.q,
                SortAndDirection.create(dataType, find_filter.sort, find_filter.direction),
            )
        } else {
            StashFindFilter(null, dataType.defaultSort)
        }
    val objectFilter = filterParser.convertFilter(dataType, object_filter)
    return FilterArgs(dataType, name.ifBlank { null }, findFilter, objectFilter)
}
