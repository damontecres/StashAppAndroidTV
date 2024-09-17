package com.github.damontecres.stashapp.filter.output

import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.QueryEngine
import kotlin.reflect.full.declaredMemberProperties

class FilterWriter(private val queryEngine: QueryEngine) {
    private suspend fun convert(filter: FilterArgs): Map<String, Any> {
        val sortAndDirection = filter.findFilter?.sortAndDirection ?: filter.dataType.defaultSort
        val objectFilter =
            when (filter.objectFilter) {
                is SceneFilterType -> convertFilter(filter.objectFilter)
                null -> null
                else -> TODO()
            }
        return buildMap {
            put("name", filter.name!!)
            put("mode", filter.dataType.filterMode.rawValue)
            put(
                "find_filter",
                buildMap {
                    put("direction", sortAndDirection.direction.rawValue)
                    put("sort", sortAndDirection.sort)
                    put("q", "")
                    put("page", 1)
                    put("per_page", 40)
                },
            )
            if (objectFilter != null) {
                put("object_filter", objectFilter)
            }
            put(
                "ui_options",
                buildMap {
                    put("display_mode", 0)
                    put("zoom_index", 1)
                },
            )
        }
    }

    fun uiOptions() =
        buildMap {
            put("display_mode", 0)
            put("zoom_index", 1)
        }

    fun convertFindFilter(sortAndDirection: SortAndDirection): Map<String, Any> {
        return buildMap {
            put("direction", sortAndDirection.direction.rawValue)
            put("sort", sortAndDirection.sort)
            put("q", "")
            put("page", 1)
            put("per_page", 40)
        }
    }

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
                                is Boolean -> {
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
        val TYPE_MAPPING =
            mapOf(
                "performers" to DataType.PERFORMER,
                "tags" to DataType.TAG,
            )
    }
}
