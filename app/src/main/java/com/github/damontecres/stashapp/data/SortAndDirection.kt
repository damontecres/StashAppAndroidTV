package com.github.damontecres.stashapp.data

import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.util.getRandomSort
import kotlinx.serialization.Serializable

@Serializable
data class SortAndDirection(val sort: String, val direction: SortDirectionEnum) {
    val asFindFilterType get() = FindFilterType(sort = Optional.present(sort), direction = Optional.present(direction))

    val isRandom get() = sort.startsWith("random")

    companion object {
        val NAME_ASC = SortAndDirection("name", SortDirectionEnum.ASC)
        val PATH_ASC = SortAndDirection("path", SortDirectionEnum.ASC)

        fun random() = SortAndDirection(getRandomSort(), SortDirectionEnum.ASC)

        fun create(
            dataType: DataType,
            sort: String?,
            direction: String?,
        ): SortAndDirection {
            return create(
                dataType,
                sort,
                SortDirectionEnum.entries.firstOrNull { it.rawValue == direction },
            )
        }

        fun create(
            dataType: DataType,
            sort: String?,
            direction: SortDirectionEnum?,
        ): SortAndDirection {
            return if (sort != null) {
                SortAndDirection(sort, direction ?: SortDirectionEnum.ASC)
            } else {
                dataType.defaultSort
            }
        }
    }
}
