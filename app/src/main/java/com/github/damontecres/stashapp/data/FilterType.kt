package com.github.damontecres.stashapp.data

import android.os.Parcelable

enum class FilterType {
    CUSTOM_FILTER,
    SAVED_FILTER,
    ;

    companion object {
        fun safeValueOf(value: String?): FilterType? {
            return entries.firstOrNull { it.name == value }
        }
    }
}

interface StashFilter : Parcelable {
    val filterType: FilterType
}
