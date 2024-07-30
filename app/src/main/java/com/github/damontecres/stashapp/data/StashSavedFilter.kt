package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.FilterMode
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class StashSavedFilter(
    val savedFilterId: String,
    override val dataType: DataType,
    override val sortBy: String? = null,
    override val direction: String? = null,
) : Parcelable, StashFilter {
    constructor(
        savedFilterId: String,
        mode: FilterMode,
        sortBy: String? = null,
        direction: String? = null,
    ) : this(savedFilterId, DataType.fromFilterMode(mode)!!, sortBy, direction)

    constructor(savedFilterData: SavedFilterData) : this(
        savedFilterData.id,
        savedFilterData.mode,
        savedFilterData.find_filter?.sort,
        savedFilterData.find_filter?.direction?.toString(),
    )

    override val filterType: FilterType
        get() = FilterType.SAVED_FILTER

    val mode: FilterMode
        get() = dataType.filterMode
}
