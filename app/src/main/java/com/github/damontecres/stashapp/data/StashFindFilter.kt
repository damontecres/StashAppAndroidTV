package com.github.damontecres.stashapp.data

import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.FindFilterType

data class StashFindFilter(
    val q: String? = null,
    val sortAndDirection: SortAndDirection? = null,
) {
    fun toFindFilterType(): FindFilterType =
        FindFilterType(
            q = Optional.presentIfNotNull(q),
            sort = Optional.presentIfNotNull(sortAndDirection?.sort),
            direction = Optional.presentIfNotNull(sortAndDirection?.direction),
        )
}
