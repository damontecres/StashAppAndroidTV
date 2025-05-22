package com.github.damontecres.stashapp.util

import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.suppliers.FilterArgs

fun createSceneSuggestionFilter(newScene: FullSceneData): FilterArgs? {
    val idFilter =
        Optional.present(
            IntCriterionInput(
                value = newScene.id.toInt(),
                modifier = CriterionModifier.NOT_EQUALS,
            ),
        )
    val filters =
        buildList {
            if (newScene.tags.isNotEmpty()) {
                add(
                    SceneFilterType(
                        id = idFilter,
                        tags =
                            Optional.present(
                                HierarchicalMultiCriterionInput(
                                    value = Optional.present(newScene.tags.map { it.tagData.id }),
                                    modifier = CriterionModifier.INCLUDES,
                                ),
                            ),
                    ),
                )
            }
            if (newScene.performers.isNotEmpty()) {
                add(
                    SceneFilterType(
                        id = idFilter,
                        performers =
                            Optional.presentIfNotNull(
                                MultiCriterionInput(
                                    value = Optional.present(newScene.performers.map { it.id }),
                                    modifier = CriterionModifier.INCLUDES,
                                ),
                            ),
                    ),
                )
            }
            if (newScene.studio?.studioData != null) {
                add(
                    SceneFilterType(
                        id = idFilter,
                        studios =
                            Optional.present(
                                HierarchicalMultiCriterionInput(
                                    value = Optional.present(listOf(newScene.studio.studioData.id)),
                                    modifier = CriterionModifier.EQUALS,
                                ),
                            ),
                    ),
                )
            }
            if (newScene.groups.isNotEmpty()) {
                add(
                    SceneFilterType(
                        id = idFilter,
                        groups =
                            Optional.present(
                                HierarchicalMultiCriterionInput(
                                    value = Optional.present(newScene.groups.map { it.group.groupData.id }),
                                    modifier = CriterionModifier.INCLUDES,
                                ),
                            ),
                    ),
                )
            }
        }.toMutableList()
    return if (filters.isNotEmpty()) {
        for (i in (1..<filters.size).reversed()) {
            filters[i - 1] =
                filters[i - 1].copy(OR = Optional.present(filters[i]))
        }
        val objectFilter = filters.first()
        FilterArgs(
            DataType.SCENE,
            objectFilter = objectFilter,
            findFilter = StashFindFilter(sortAndDirection = SortAndDirection.random()),
        ).withResolvedRandom()
    } else {
        null
    }
}
