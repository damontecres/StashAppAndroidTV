package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import kotlinx.parcelize.Parcelize

@Parcelize
data class StashCustomFilter(
    val mode: FilterMode,
    val direction: String?,
    val sortBy: String?,
    val description: String,
    val query: String? = null,
) : Parcelable, StashFilter {
    override val filterType: FilterType
        get() = FilterType.CUSTOM_FILTER
    override val dataType: DataType
        get() = DataType.fromFilterMode(mode)!!

    fun asFindFilterType(): FindFilterType {
        val direction =
            if (direction != null) {
                SortDirectionEnum.valueOf(direction)
            } else {
                val d = DataType.fromFilterMode(mode)?.defaultSort?.direction?.name
                if (d != null) {
                    SortDirectionEnum.valueOf(d)
                } else {
                    null
                }
            }
        return FindFilterType(
            q = Optional.presentIfNotNull(query),
            page = Optional.absent(),
            per_page = Optional.absent(),
            sort = Optional.presentIfNotNull(sortBy),
            direction = Optional.presentIfNotNull(direction),
        )
    }
}
