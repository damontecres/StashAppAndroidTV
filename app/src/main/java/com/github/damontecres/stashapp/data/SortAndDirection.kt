package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import kotlinx.parcelize.Parcelize

@Parcelize
data class SortAndDirection(val sort: String, val direction: SortDirectionEnum) : Parcelable {
    val asFindFilterType get() = FindFilterType(sort = Optional.present(sort), direction = Optional.present(direction))

    companion object {
        val NAME_ASC = SortAndDirection("name", SortDirectionEnum.ASC)
        val PATH_ASC = SortAndDirection("path", SortDirectionEnum.ASC)
    }
}
