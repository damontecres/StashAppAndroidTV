package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.FilterMode
import kotlinx.parcelize.Parcelize

@Parcelize
data class StashSavedFilter(
    val savedFilterId: String,
    val mode: FilterMode,
    override val sortBy: String? = null,
    override val direction: String? = null,
) : Parcelable, StashFilter {
    constructor(savedFilterData: SavedFilterData) : this(
        savedFilterData.id,
        savedFilterData.mode,
        savedFilterData.find_filter?.sort,
        savedFilterData.find_filter?.direction?.toString(),
    )

    override val filterType: FilterType
        get() = FilterType.SAVED_FILTER
    override val dataType: DataType
        get() = DataType.fromFilterMode(mode)!!
}
