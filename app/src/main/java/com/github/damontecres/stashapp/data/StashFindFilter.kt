package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import kotlinx.parcelize.Parcelize

@Parcelize
data class StashFindFilter(
    val q: String? = null,
    val sortAndDirection: SortAndDirection? = null,
) : Parcelable {
    constructor(sortAndDirection: SortAndDirection) : this(null, sortAndDirection)

    fun toFindFilterType(
        page: Int? = null,
        perPage: Int? = null,
    ): FindFilterType =
        FindFilterType(
            q = Optional.presentIfNotNull(q),
            sort = Optional.presentIfNotNull(sortAndDirection?.sort),
            direction = Optional.presentIfNotNull(sortAndDirection?.direction),
            page = Optional.presentIfNotNull(page),
            per_page = Optional.presentIfNotNull(perPage),
        )
}

fun FindFilterType.toStashFindFilter(): StashFindFilter =
    StashFindFilter(
        q = q.getOrNull(),
        sortAndDirection =
            if (sort.getOrNull() != null && direction.getOrNull() != null) {
                SortAndDirection(sort.getOrNull()!!, direction.getOrNull()!!)
            } else if (sort.getOrNull() != null) {
                SortAndDirection(sort.getOrNull()!!, SortDirectionEnum.ASC)
            } else {
                null
            },
    )
