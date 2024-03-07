package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.type.FilterMode
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
}
