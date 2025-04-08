package com.github.damontecres.stashapp.data

import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.util.presentIfNotNullOrBlank
import kotlinx.serialization.Serializable

/**
 * Represents a "find" filter which is basically a query string and a sort by
 */
@Serializable
data class StashFindFilter(
    val q: String? = null,
    val sortAndDirection: SortAndDirection? = null,
) {
    constructor(sortAndDirection: SortAndDirection) : this(null, sortAndDirection)

    fun toFindFilterType(
        page: Int? = null,
        perPage: Int? = null,
    ): FindFilterType =
        FindFilterType(
            q = Optional.presentIfNotNullOrBlank(q),
            sort = Optional.presentIfNotNullOrBlank(sortAndDirection?.sortKey),
            direction = Optional.presentIfNotNull(sortAndDirection?.direction),
            page = Optional.presentIfNotNull(page),
            per_page = Optional.presentIfNotNull(perPage),
        )

    /**
     * This, but with a different sort key
     */
    fun withSort(sort: String): StashFindFilter =
        copy(
            sortAndDirection =
                SortAndDirection.create(
                    sort,
                    sortAndDirection?.direction,
                ),
        )

    /**
     * This, but with a different sort direction
     */
    fun withDirection(
        direction: SortDirectionEnum,
        dataType: DataType,
    ): StashFindFilter {
        val newSortAndDirection =
            (sortAndDirection ?: dataType.defaultSort).copy(direction = direction)
        return this.copy(sortAndDirection = newSortAndDirection)
    }
}

fun FindFilterType.toStashFindFilter(): StashFindFilter =
    StashFindFilter(
        q = q.getOrNull(),
        sortAndDirection =
            if (sort.getOrNull() != null && direction.getOrNull() != null) {
                SortAndDirection.create(sort.getOrNull()!!, direction.getOrNull()!!)
            } else if (sort.getOrNull() != null) {
                SortAndDirection.create(sort.getOrNull()!!, SortDirectionEnum.ASC)
            } else {
                null
            },
    )

fun <T> Optional<T>.merge(other: Optional<T>): Optional<T> =
    if (other is Optional.Present) {
        other
    } else {
        this
    }

fun FindFilterType.merge(other: FindFilterType?): FindFilterType =
    if (other != null) {
        this.copy(
            q = q.merge(other.q),
            page = page.merge(other.page),
            per_page = per_page.merge(other.per_page),
            sort = sort.merge(other.sort),
            direction = direction.merge(other.direction),
        )
    } else {
        this
    }
