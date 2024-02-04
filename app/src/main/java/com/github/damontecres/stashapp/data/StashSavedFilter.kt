package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.type.FilterMode
import kotlinx.parcelize.Parcelize

@Parcelize
data class StashSavedFilter(
    val savedFilterId: String,
    val mode: FilterMode,
    val sortBy: String? = null,
) : Parcelable
