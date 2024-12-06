package com.github.damontecres.stashapp.util

import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SavedFindFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs

class AlphabetSearchUtils {
    companion object {
        const val LETTERS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        /**
         * Find the deepest non-null AND filter for the given filter
         *
         * X -> X
         * X AND Y -> Y
         * X AND Y AND Z -> Z
         */
        fun findNullAndFilter(filter: StashDataFilter): StashDataFilter {
            val andFilter =
                when (filter) {
                    is SceneFilterType -> filter.AND
                    is PerformerFilterType -> filter.AND
                    is GalleryFilterType -> filter.AND
                    is GroupFilterType -> filter.AND
                    is ImageFilterType -> filter.AND
                    is MovieFilterType -> filter.AND
                    is StudioFilterType -> filter.AND
                    is TagFilterType -> filter.AND
                    is SceneMarkerFilterType -> throw IllegalArgumentException()

                    // TODO, these shouldn't be StashDataFilter I think
                    is FindFilterType -> throw IllegalArgumentException()
                    is SavedFindFilterType -> throw IllegalArgumentException()
                }.getOrNull()
            return if (andFilter != null) {
                findNullAndFilter(andFilter)
            } else {
                filter
            }
        }

        suspend fun findPosition(
            letter: Char,
            filterArgs: FilterArgs,
            queryEngine: QueryEngine,
            dataSupplierFactory: DataSupplierFactory,
        ): Int {
            val index = LETTERS.indexOf(letter, ignoreCase = true)
            if (index < 0) {
                throw IllegalArgumentException("'$letter' is not a valid letter")
            }
            val regex =
                if (index == 0) {
                    // Number, so count symbols
                    "^([!-/])"
                } else if (index == 1) {
                    // A, so count symbols & numbers
                    "^([!-/0-9:-@])"
                } else if (index == 2) {
                    // B, so count symbols, numbers, & A/a
                    "^([!-/0-9:-@Aa])"
                } else {
                    // C+, so count symbols, numbers, and earlier letters
                    val startingLetter = LETTERS[index - 1]
                    "^([!-/0-9:-@A-${startingLetter}a-${startingLetter.lowercase()}])"
                }
            val stringCriterion =
                Optional.present(
                    StringCriterionInput(
                        value = regex,
                        modifier = CriterionModifier.MATCHES_REGEX,
                    ),
                )

            val initialObjectFilter =
                filterArgs.objectFilter ?: when (filterArgs.dataType) {
                    DataType.SCENE -> SceneFilterType()
                    DataType.GROUP -> GroupFilterType()
                    DataType.MARKER -> SceneMarkerFilterType()
                    DataType.PERFORMER -> PerformerFilterType()
                    DataType.STUDIO -> StudioFilterType()
                    DataType.TAG -> TagFilterType()
                    DataType.IMAGE -> ImageFilterType()
                    DataType.GALLERY -> GalleryFilterType()
                }
            val andObjectFilter = findNullAndFilter(initialObjectFilter)
            val transformed =
                when (filterArgs.dataType) {
                    DataType.GROUP -> (andObjectFilter as GroupFilterType).copy(name = stringCriterion)
                    DataType.PERFORMER -> (andObjectFilter as PerformerFilterType).copy(name = stringCriterion)
                    DataType.STUDIO -> (andObjectFilter as StudioFilterType).copy(name = stringCriterion)
                    DataType.TAG -> (andObjectFilter as TagFilterType).copy(name = stringCriterion)

                    // TODO use filename
                    DataType.SCENE -> (andObjectFilter as SceneFilterType).copy(title = stringCriterion)
                    DataType.IMAGE -> (andObjectFilter as ImageFilterType).copy(title = stringCriterion)
                    DataType.GALLERY -> (andObjectFilter as GalleryFilterType).copy(title = stringCriterion)

                    DataType.MARKER -> TODO()
                }

            val newFilter = filterArgs.copy(objectFilter = transformed)
            val dataSupplier =
                dataSupplierFactory.create<Query.Data, StashData, Query.Data>(newFilter)
            val query = dataSupplier.createCountQuery(filterArgs.findFilter?.toFindFilterType())
            return dataSupplier.parseCountQuery(queryEngine.executeQuery(query).data!!)
        }
    }
}
