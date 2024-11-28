package com.github.damontecres.stashapp.util

import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.FilterArgs

class AlphabetSearchUtils {
    companion object {
        fun transform(
            letter: Char,
            filterArgs: FilterArgs,
        ): FilterArgs {
            // (^[Aa].*)
            val regex = if (letter == '#') "(^[0-9].*)" else "(^[$letter${letter.lowercase()}].*)"
            val stringCrit =
                Optional.present(
                    StringCriterionInput(
                        value = regex,
                        modifier = CriterionModifier.MATCHES_REGEX,
                    ),
                )

            val objectFilter =
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
            val transformed =
                when (filterArgs.dataType) {
                    DataType.SCENE -> (objectFilter as SceneFilterType).copy(title = stringCrit)
                    DataType.GROUP -> (objectFilter as GroupFilterType).copy(name = stringCrit)
                    DataType.MARKER -> TODO()
                    DataType.PERFORMER -> (objectFilter as PerformerFilterType).copy(name = stringCrit)
                    DataType.STUDIO -> (objectFilter as StudioFilterType).copy(name = stringCrit)
                    DataType.TAG -> (objectFilter as TagFilterType).copy(name = stringCrit)
                    DataType.IMAGE -> (objectFilter as ImageFilterType).copy(title = stringCrit)
                    DataType.GALLERY -> (objectFilter as GalleryFilterType).copy(title = stringCrit)
                }
            return filterArgs.copy(objectFilter = transformed)
        }
    }
}
