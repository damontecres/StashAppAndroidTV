package com.github.damontecres.stashapp.filter.output

import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CircumcisionCriterionInput
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.GenderCriterionInput
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
import kotlin.reflect.full.declaredMemberProperties

/**
 * Converts a [StashDataFilter] into the JSON (Map) representation
 *
 * @param associateIdsToNames The webUI requires a "label" for some IDs, so this is used to query for those
 */
class FilterWriter(
    private val filterDataType: DataType,
    private val associateIdsToNames: suspend (dataType: DataType, ids: List<String>) -> Map<String, String?>,
) {
    suspend fun convertFilter(filter: StashDataFilter): Map<String, Any> {
        val objectFilter =
            buildMap<String, Any> {
                filter.javaClass.kotlin.declaredMemberProperties.forEach { param ->
                    val obj = param.get(filter) as Optional<*>
                    if (obj != Optional.Absent) {
                        val o = obj.getOrNull()!!
                        val dataType = getType(filterDataType, param.name)
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
                                is GenderCriterionInput -> o.toMap()
                                is CircumcisionCriterionInput -> o.toMap()

                                is Boolean, is String -> {
                                    mapOf(
                                        "value" to o.toString(),
                                        "modifier" to CriterionModifier.EQUALS.rawValue,
                                    )
                                }

                                is MultiCriterionInput -> {
                                    val items = associateIdsToNames(dataType!!, o.getAllIds())
                                    if (param.name == "galleries") {
                                        o.toGalleryMap(items)
                                    } else {
                                        o.toMap(items)
                                    }
                                }

                                is HierarchicalMultiCriterionInput -> {
                                    val items = associateIdsToNames(dataType!!, o.getAllIds())
                                    o.toMap(items)
                                }

                                is StashDataFilter -> convertFilter(o)

                                else -> throw UnsupportedOperationException(
                                    "Unable to convert ${filter::class.simpleName}.${param.name} (${o::class.qualifiedName})",
                                )
                            }
                        put(param.name, value)
                    }
                }
            }
        return objectFilter
    }

    companion object {
        /**
         * Map the name of a filter to a [DataType]. Not all filters have a [DataType] though!
         */
        fun getType(
            parentFilterDataType: DataType,
            name: String,
        ): DataType? = TYPE_MAPPING[name] ?: TYPE_MAPPING_BY_TYPE[parentFilterDataType]?.get(name)

        private val TYPE_MAPPING =
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
                "scene_tags" to DataType.TAG,
                "containing_groups" to DataType.GROUP,
                "sub_groups" to DataType.GROUP,
            )
        private val TYPE_MAPPING_BY_TYPE =
            mapOf(
                DataType.TAG to
                    mapOf(
                        "parents" to DataType.TAG,
                        "children" to DataType.TAG,
                    ),
                DataType.STUDIO to
                    mapOf(
                        "parents" to DataType.STUDIO,
                        "children" to DataType.STUDIO,
                    ),
            )
    }
}
