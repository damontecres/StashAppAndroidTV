package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.util.toFind_filter
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class StashCustomFilter(
    override val dataType: DataType,
    override val direction: String?,
    override val sortBy: String?,
    val description: String,
    val query: String? = null,
) : Parcelable, StashFilter {
    constructor(
        mode: FilterMode,
        direction: String?,
        sortBy: String?,
        description: String,
        query: String? = null,
    ) :
        this(DataType.fromFilterMode(mode)!!, direction, sortBy, description, query)

    override val filterType: FilterType
        get() = FilterType.CUSTOM_FILTER
    val mode: FilterMode
        get() = dataType.filterMode

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

    fun toSavedFilterData(): SavedFilterData {
        return SavedFilterData(
            id = "-1",
            mode = dataType.filterMode,
            name = description,
            find_filter = asFindFilterType().toFind_filter(),
            object_filter = null,
            ui_options = null,
            __typename = javaClass.name,
        )
    }
}
