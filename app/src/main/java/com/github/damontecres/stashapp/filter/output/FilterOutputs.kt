package com.github.damontecres.stashapp.filter.output

import com.github.damontecres.stashapp.api.type.CircumcisionCriterionInput
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.GenderCriterionInput
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationEnum
import com.github.damontecres.stashapp.api.type.PHashDuplicationCriterionInput
import com.github.damontecres.stashapp.api.type.PhashDistanceCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionEnum
import com.github.damontecres.stashapp.api.type.StashIDCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput

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

fun MultiCriterionInput.toMap(labelMapping: Map<String, String?>): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        put(
            "value",
            buildMap<String, Any> {
                val items =
                    value.getOrNull().orEmpty().map { id ->
                        buildMap {
                            put("id", id)
                            put("label", labelMapping[id] ?: id)
                        }
                    }
                put("items", items)

                val excluded =
                    excludes.getOrNull().orEmpty().map { id ->
                        buildMap {
                            put("id", id)
                            put("label", labelMapping[id] ?: id)
                        }
                    }
                put("excluded", excluded)
            },
        )
    }

fun HierarchicalMultiCriterionInput.getAllIds() = value.getOrNull().orEmpty() + excludes.getOrNull().orEmpty()

fun HierarchicalMultiCriterionInput.toMap(labelMapping: Map<String, String?>): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        put(
            "value",
            buildMap<String, Any> {
                put("depth", depth.getOrNull() ?: 0)

                val items =
                    value.getOrNull().orEmpty().map { id ->
                        buildMap {
                            put("id", id)
                            put("label", labelMapping[id] ?: id)
                        }
                    }
                put("items", items)

                val excluded =
                    excludes.getOrNull().orEmpty().map { id ->
                        buildMap {
                            put("id", id)
                            put("label", labelMapping[id] ?: id)
                        }
                    }
                put("excluded", excluded)
            },
        )
    }

fun PhashDistanceCriterionInput.toMap(): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        put(
            "value",
            buildMap<String, Any> {
                put("value", value)
                if (distance.getOrNull() != null) {
                    put("distance", distance.getOrNull()!!)
                }
            },
        )
    }

fun PHashDuplicationCriterionInput.toMap(): Map<String, Any> =
    buildMap {
        put("modifier", CriterionModifier.EQUALS.rawValue)
        if (duplicated.getOrNull() == false) {
            put("value", false)
        } else {
            put("value", true)
        }
    }

fun ResolutionCriterionInput.toMap(): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        val name =
            when (value) {
                ResolutionEnum.VERY_LOW -> "144p"
                ResolutionEnum.LOW -> "240p"
                ResolutionEnum.R360P -> "360p"
                ResolutionEnum.STANDARD -> "480p"
                ResolutionEnum.WEB_HD -> "540p"
                ResolutionEnum.STANDARD_HD -> "720p"
                ResolutionEnum.FULL_HD -> "1080p"
                ResolutionEnum.QUAD_HD -> "1440p"
                ResolutionEnum.VR_HD -> "4k"
                ResolutionEnum.FOUR_K -> "4k"
                ResolutionEnum.FIVE_K -> "5k"
                ResolutionEnum.SIX_K -> "6k"
                ResolutionEnum.SEVEN_K -> "7k"
                ResolutionEnum.EIGHT_K -> "8k"
                ResolutionEnum.HUGE -> "Huge"
                ResolutionEnum.UNKNOWN__ -> throw IllegalArgumentException()
            }
        put("value", name)
    }

fun OrientationCriterionInput.toMap(): Map<String, Any> =
    buildMap {
        put("modifier", CriterionModifier.EQUALS.rawValue)
        put(
            "value",
            value.map {
                when (it) {
                    OrientationEnum.LANDSCAPE -> "Landscape"
                    OrientationEnum.PORTRAIT -> "Portrait"
                    OrientationEnum.SQUARE -> "Square"
                    OrientationEnum.UNKNOWN__ -> throw IllegalArgumentException()
                }
            },
        )
    }

fun StashIDCriterionInput.toMap(): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        put(
            "value",
            buildMap {
                put("stashID", stash_id.getOrNull() ?: "")
                put("endpoint", endpoint.getOrNull() ?: "")
            },
        )
    }

fun TimestampCriterionInput.toMap(): Map<String, Any> =
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

fun DateCriterionInput.toMap(): Map<String, Any> =
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

fun GenderCriterionInput.toMap(): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        val values =
            value_list.getOrNull() ?: listOfNotNull(value.getOrNull())
                .map {
                    when (it) {
                        GenderEnum.MALE -> "Male"
                        GenderEnum.FEMALE -> "Female"
                        GenderEnum.TRANSGENDER_MALE -> "Transgender Male"
                        GenderEnum.TRANSGENDER_FEMALE -> "Transgender Female"
                        GenderEnum.INTERSEX -> "Intersex"
                        GenderEnum.NON_BINARY -> "Non-Binary"
                        GenderEnum.UNKNOWN__ -> "Unknown"
                    }
                }
        put("value", values)
    }

fun CircumcisionCriterionInput.toMap(): Map<String, Any> =
    buildMap {
        put("modifier", modifier.rawValue)
        val values =
            value
                .getOrNull()
                ?.map {
                    when (it) {
                        CircumisedEnum.CUT -> "Cut"
                        CircumisedEnum.UNCUT -> "Uncut"
                        CircumisedEnum.UNKNOWN__ -> "Unknown"
                    }
                }.orEmpty()
        put("value", values)
    }
