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
            useRandomSeed: Boolean = true,
        ): SortAndDirection =
            if (sort != null) {
                create(sort, direction, useRandomSeed)
            } else {
                dataType.defaultSort
            }

        /**
         * Create a [SortAndDirection] from a sort key and direction. If the sort key is `random`,
         * optionally store the random seed which is the number in `random_123456` or ignore it to
         * possibly resolve later
         *
         * @param sort the sort by key (eg `random`, `random_123456`, `name`, etc)
         * @param direction the direction, ascending or descending
         * @param useRandomSeed whether to use the random seed or ignore it
         */
        fun create(
            sort: String,
            direction: SortDirectionEnum?,
            useRandomSeed: Boolean = true,
        ): SortAndDirection {
            val sortOption = SortOption.getByKey(sort)
            val randomSeed =
                if (sortOption == SortOption.Random && sort.contains("_") && useRandomSeed) {
                    sort.split("_")[1].toInt()
                } else {
                    -1
                }
            return SortAndDirection(
                sortOption,
                direction ?: SortDirectionEnum.ASC,
                randomSeed,
            )
        }
    }
}

fun SortDirectionEnum?.flip(): SortDirectionEnum = if (this == SortDirectionEnum.ASC) SortDirectionEnum.DESC else SortDirectionEnum.ASC
