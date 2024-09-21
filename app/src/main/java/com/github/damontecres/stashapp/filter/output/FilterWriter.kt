package com.github.damontecres.stashapp.filter.output

import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationCriterionInput
import com.github.damontecres.stashapp.api.type.PHashDuplicationCriterionInput
import com.github.damontecres.stashapp.api.type.PhashDistanceCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StashIDCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.util.QueryEngine
import kotlin.reflect.full.declaredMemberProperties

/**
 * Converts a [StashDataFilter] into the JSON (Map) representation
 *
 * @param queryEngine The webUI requires a "label" for some IDs, so this is used to query for those
 */
class FilterWriter(private val queryEngine: QueryEngine) {
    suspend fun convertFilter(filter: StashDataFilter): Map<String, Any> {
        val objectFilter =
            buildMap<String, Any> {
                filter.javaClass.kotlin.declaredMemberProperties.forEach { param ->
                    val obj = param.get(filter) as Optional<*>
                    if (obj != Optional.Absent) {
                        val o = obj.getOrNull()!!
                        val dataType = TYPE_MAPPING[param.name]
                        val value =
                            when (o) {
                                is IntCriterionInput -> o.toMap()
                                is FloatCriterionInput -> o.toMap()
                                is StringCriterionInput -> o.toMap()
                                is PhashDistanceCriterionInput -> o.toMap()
                                is PHashDuplicationCriterionInput -> o.toMap()
                                is ResolutionCriterionInput -> o.toMap()
                                is OrientationCriterionInput -> o.toMap()
                                is StashIDCriterionInput -> o.toMap()
                                is TimestampCriterionInput -> o.toMap()
                                is DateCriterionInput -> o.toMap()

                                is Boolean, String -> {
                                    mapOf(
                                        "value" to o.toString(),
                                        "modifier" to CriterionModifier.EQUALS.rawValue,
                                    )
                                }

                                is MultiCriterionInput -> {
                                    val items = queryEngine.getByIds(dataType!!, o.getAllIds())
                                    o.toMap(associateIds(items))
                                }

                                is HierarchicalMultiCriterionInput -> {
                                    val items = queryEngine.getByIds(dataType!!, o.getAllIds())
                                    o.toMap(associateIds(items))
                                }

                                else -> TODO()
                            }
                        put(param.name, value)
                    }
                }
            }
        return objectFilter
    }

    private fun associateIds(items: List<StashData>): Map<String, String> {
        return items.associate {
            when (it) {
                is TagData -> it.id to it.name
                is PerformerData -> it.id to it.name
                else -> TODO()
            }
        }
    }

    companion object {
        /**
         * Map the name of a filter to a [DataType]. Not all filters have a [DataType] though!
         */
        val TYPE_MAPPING =
            mapOf(
                "performers" to DataType.PERFORMER,
                "tags" to DataType.TAG,
                "performer_tags" to DataType.TAG,
                "studios" to DataType.STUDIO,
                "movies" to DataType.GROUP,
                "groups" to DataType.GROUP,
                "galleries" to DataType.GALLERY,
                "images" to DataType.IMAGE,
                "scene_markers" to DataType.MARKER,
                "scenes" to DataType.SCENE,
            )
    }
}
