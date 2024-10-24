package com.github.damontecres.stashapp.data

import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import kotlinx.serialization.Serializable

/**
 * Represents a way to sort something
 */
@Serializable
data class SortAndDirection(
    val sort: SortOption,
    val direction: SortDirectionEnum,
    val randomSeed: Int = -1,
) {
    val asFindFilterType
        get() =
            FindFilterType(
                sort = Optional.present(sortKey),
                direction = Optional.present(direction),
            )

    /**
     * Is this sorting by random?
     */
    val isRandom get() = sort == SortOption.RANDOM

    val sortKey get() = if (isRandom && randomSeed >= 0) "random_$randomSeed" else sort.key

    companion object {
        val NAME_ASC = SortAndDirection(SortOption.NAME, SortDirectionEnum.ASC)
        val PATH_ASC = SortAndDirection(SortOption.PATH, SortDirectionEnum.ASC)

        /**
         * Create a random sort
         */
        fun random() = SortAndDirection(SortOption.RANDOM, SortDirectionEnum.ASC)

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
                create(sort, direction)
            } else {
                dataType.defaultSort
            }
        }

        fun create(
            sort: String,
            direction: SortDirectionEnum?,
        ): SortAndDirection {
            val sortOption = SortOption.getByKey(sort)
            val randomSeed =
                if (sortOption == SortOption.RANDOM && sort.contains("_")) {
                    sort.split("_")[1].toInt()
                } else {
                    -1
                }
            return SortAndDirection(
                SortOption.getByKey(sort),
                direction ?: SortDirectionEnum.ASC,
                randomSeed,
            )
        }
    }
}
