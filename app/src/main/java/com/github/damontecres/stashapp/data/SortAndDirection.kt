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
    val isRandom get() = sort == SortOption.Random

    val sortKey get() = if (isRandom && randomSeed >= 0) "random_$randomSeed" else sort.key

    companion object {
        val NAME_ASC = SortAndDirection(SortOption.Name, SortDirectionEnum.ASC)
        val PATH_ASC = SortAndDirection(SortOption.Path, SortDirectionEnum.ASC)

        /**
         * Create a random sort
         */
        fun random() = SortAndDirection(SortOption.Random, SortDirectionEnum.ASC)

        fun create(
            dataType: DataType,
            sort: String?,
            direction: String?,
        ): SortAndDirection =
            create(
                dataType,
                sort,
                SortDirectionEnum.entries.firstOrNull { it.rawValue == direction },
            )

        fun create(
            dataType: DataType,
            sort: String?,
            direction: SortDirectionEnum?,
        ): SortAndDirection =
            if (sort != null) {
                create(sort, direction)
            } else {
                dataType.defaultSort
            }

        fun create(
            sort: String,
            direction: SortDirectionEnum?,
        ): SortAndDirection {
            val sortOption = SortOption.getByKey(sort)
            val randomSeed =
                if (sortOption == SortOption.Random && sort.contains("_")) {
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
