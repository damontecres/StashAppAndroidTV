package com.github.damontecres.stashapp.filter.output

import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput

fun IntCriterionInput.toMap(): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        put(
            "value",
            buildMap<String, Any> {
                put("value", value)
                if (value2.getOrNull() != null) {
                    put("value2", value2.getOrNull()!!)
                }
            },
        )
    }

fun FloatCriterionInput.toMap(): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        put(
            "value",
            buildMap<String, Any> {
                put("value", value)
                if (value2.getOrNull() != null) {
                    put("value2", value2.getOrNull()!!)
                }
            },
        )
    }

fun StringCriterionInput.toMap(): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        put("value", value)
    }

fun MultiCriterionInput.getAllIds() = value.getOrNull().orEmpty() + excludes.getOrNull().orEmpty()

fun MultiCriterionInput.toMap(labelMapping: Map<String, String>): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        put(
            "value",
            buildMap<String, Any> {
                val items =
                    value.getOrNull().orEmpty().map { id ->
                        buildMap {
                            put("id", id)
                            put("label", labelMapping[id])
                        }
                    }
                if (items.isNotEmpty()) {
                    put("items", items)
                }
                val excludes =
                    excludes.getOrNull().orEmpty().map { id ->
                        buildMap {
                            put("id", id)
                            put("label", labelMapping[id])
                        }
                    }
                if (excludes.isNotEmpty()) {
                    put("excludes", excludes)
                }
            },
        )
    }

fun HierarchicalMultiCriterionInput.getAllIds() = value.getOrNull().orEmpty() + excludes.getOrNull().orEmpty()

fun HierarchicalMultiCriterionInput.toMap(labelMapping: Map<String, String>): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        put("depth", depth.getOrNull() ?: 0)
        put(
            "value",
            buildMap<String, Any> {
                val items =
                    value.getOrNull().orEmpty().map { id ->
                        buildMap {
                            put("id", id)
                            put("label", labelMapping[id])
                        }
                    }
                if (items.isNotEmpty()) {
                    put("items", items)
                }
                val excludes =
                    excludes.getOrNull().orEmpty().map { id ->
                        buildMap {
                            put("id", id)
                            put("label", labelMapping[id])
                        }
                    }
                if (excludes.isNotEmpty()) {
                    put("excludes", excludes)
                }
            },
        )
    }
