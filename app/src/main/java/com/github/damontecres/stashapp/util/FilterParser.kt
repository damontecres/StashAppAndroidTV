package com.github.damontecres.stashapp.util

import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CircumcisionCriterionInput
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.CustomFieldCriterionInput
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
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StashIDCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.suppliers.FilterArgs

/**
 * Parse a server-side filter from JSON (Map<String, *>)
 */
class FilterParser(
    private val serverVersion: Version,
) {
    companion object {
        const val TAG = "FilterParser"
    }

    fun convertIntCriterionInput(it: Map<String, *>?): IntCriterionInput? =
        if (it != null) {
            val values = it["value"] as Map<String, *>?
            IntCriterionInput(
                value = values?.get("value")?.toString()?.toInt() ?: 0,
                value2 = Optional.presentIfNotNull(values?.get("value2")?.toString()?.toInt()),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }

    fun convertFloatCriterionInput(it: Map<String, *>?): FloatCriterionInput? =
        if (it != null) {
            val values = it["value"] as Map<String, *>? // Might be an int or double
            FloatCriterionInput(
                value = values?.get("value")?.toString()?.toDouble() ?: 0.0,
                value2 = Optional.presentIfNotNull(values?.get("value2")?.toString()?.toDouble()),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }

    fun convertStringCriterionInput(it: Map<String, *>?): StringCriterionInput? =
        if (it != null) {
            StringCriterionInput(
                value = it["value"]?.toString() ?: "",
                modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
            )
        } else {
            null
        }

    private fun mapToIds(list: Any?): List<String>? =
        if (list != null && list is List<*>) {
            list.mapNotNull { (it as Map<*, *>)["id"]?.toString() }.toList()
        } else {
            null
        }

    fun convertHierarchicalMultiCriterionInput(it: Map<String, *>?): HierarchicalMultiCriterionInput? =
        if (it != null) {
            val values = it["value"]!! as Map<String, *>
            val items = mapToIds(values["items"])
            val excludes = mapToIds(values["excluded"])
            HierarchicalMultiCriterionInput(
                value = Optional.presentIfNotNull(items),
                modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
                depth = Optional.presentIfNotNull(values["depth"]?.toString()?.toIntOrNull()),
                excludes = Optional.presentIfNotNull(excludes),
            )
        } else {
            null
        }

    fun convertMultiCriterionInput(it: Map<String, *>?): MultiCriterionInput? =
        if (it != null) {
            if (it["value"] != null && it["value"] is Map<*, *>) {
                val values = it["value"] as Map<String, *>
                val items = mapToIds(values["items"])
                val excludes = mapToIds(values["excluded"])
                MultiCriterionInput(
                    value = Optional.presentIfNotNull(items),
                    modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
                    excludes = Optional.presentIfNotNull(excludes),
                )
            } else if (it["value"] != null && it["value"] is List<*>) {
                val items =
                    (it["value"] as List<*>).map {
                        if (it is Map<*, *>) {
                            it["id"].toString()
                        } else {
                            it.toString()
                        }
                    }
                MultiCriterionInput(
                    value = Optional.presentIfNotNull(items),
                    modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
                    excludes = Optional.absent(),
                )
            } else {
                MultiCriterionInput(
                    value = Optional.absent(),
                    modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
                    excludes = Optional.absent(),
                )
            }
        } else {
            null
        }

    fun convertBoolean(it: Map<String, *>?): Boolean? =
        if (it != null) {
            val value = (it["value"] as String?).toBoolean()
            when (CriterionModifier.valueOf(it["modifier"] as String)) {
                CriterionModifier.EQUALS -> value
                CriterionModifier.NOT_EQUALS -> !value
                else -> null
            }
        } else {
            null
        }

    fun convertDateCriterionInput(it: Map<String, *>?): DateCriterionInput? =
        if (it != null) {
            val values = it["value"] as Map<String, String?>?
            DateCriterionInput(
                value = values?.get("value") ?: "",
                value2 = Optional.presentIfNotNull(values?.get("value2")),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }

    fun convertTimestampCriterionInput(it: Map<String, *>?): TimestampCriterionInput? =
        if (it != null) {
            val values = it["value"] as Map<String, String?>?
            TimestampCriterionInput(
                value = values?.get("value") ?: "",
                value2 = Optional.presentIfNotNull(values?.get("value2")),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }

    fun convertCircumcisionCriterionInput(it: Map<String, *>?): CircumcisionCriterionInput? =
        if (it != null) {
            val valueList = (it["value"] as List<String>?)
            CircumcisionCriterionInput(
                value =
                    Optional.presentIfNotNull(
                        valueList
                            ?.map { CircumisedEnum.valueOf(it.uppercase()) }
                            ?.toList(),
                    ),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }

    fun convertGenderCriterionInput(it: Map<String, *>?): GenderCriterionInput? =
        if (it != null) {
            val value = it["value"]
            var values =
                if (value is List<*>) {
                    val v = value.filterNotNull().map { it.toString() }
                    v
                } else if (value != null) {
                    val v = listOf(value.toString())
                    v
                } else {
                    val v = emptyList<String>()
                    v
                }
            values =
                values.map {
                    it
                        .uppercase()
                        .replace(" ", "_")
                        .replace("-", "_")
                }

            GenderCriterionInput(
                value = Optional.absent(),
                value_list = Optional.present(values.map(GenderEnum::valueOf)),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }

    fun convertString(it: Map<String, *>?): String? {
        return if (it != null) {
            return it["value"]?.toString()
        } else {
            null
        }
    }

    fun convertStashIDCriterionInput(it: Map<String, *>?): StashIDCriterionInput? =
        if (it != null) {
            val values = it["value"] as Map<String, String?>?
            StashIDCriterionInput(
                endpoint = Optional.presentIfNotNull(values?.get("endpoint")),
                stash_id = Optional.presentIfNotNull(values?.get("stashID")),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }

    fun convertPhashDistanceCriterionInput(it: Map<String, *>?): PhashDistanceCriterionInput? =
        if (it != null) {
            val values = it["value"] as Map<String, *>?
            PhashDistanceCriterionInput(
                value = values?.get("value") as String? ?: "",
                modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
                distance = Optional.presentIfNotNull(values?.get("distance")?.toString()?.toInt()),
            )
        } else {
            null
        }

    fun convertPHashDuplicationCriterionInput(it: Map<String, *>?): PHashDuplicationCriterionInput? =
        if (it != null) {
            PHashDuplicationCriterionInput(
                duplicated = Optional.presentIfNotNull(it["value"]?.toString()?.toBoolean()),
                distance = Optional.presentIfNotNull(it["distance"]?.toString()?.toInt()),
            )
        } else {
            null
        }

    fun convertToResolutionEnum(str: String): ResolutionEnum =
        when (str) {
            "114p" -> ResolutionEnum.VERY_LOW
            "240p" -> ResolutionEnum.LOW
            "360p" -> ResolutionEnum.R360P
            "480p" -> ResolutionEnum.STANDARD
            "540p" -> ResolutionEnum.WEB_HD
            "720p" -> ResolutionEnum.STANDARD_HD
            "1080p" -> ResolutionEnum.FULL_HD
            "1440p" -> ResolutionEnum.QUAD_HD
            "4k" -> ResolutionEnum.FOUR_K
            "5k" -> ResolutionEnum.FIVE_K
            "6k" -> ResolutionEnum.SIX_K
            "7k" -> ResolutionEnum.SEVEN_K
            "8k" -> ResolutionEnum.EIGHT_K
            "Huge" -> ResolutionEnum.HUGE
            else -> ResolutionEnum.UNKNOWN__
        }

    fun convertResolutionCriterionInput(it: Map<String, *>?): ResolutionCriterionInput? =
        if (it != null) {
            ResolutionCriterionInput(
                value = convertToResolutionEnum(it["value"].toString()),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }

    fun convertOrientationCriterionInput(it: Map<String, *>?): OrientationCriterionInput? =
        if (it != null) {
            val value = it["value"]
            if (value is List<*>) {
                OrientationCriterionInput(
                    value.mapNotNull {
                        OrientationEnum.valueOf(
                            it.toString().uppercase(),
                        )
                    },
                )
            } else {
                OrientationCriterionInput(
                    listOf(
                        OrientationEnum.valueOf(
                            value.toString().uppercase(),
                        ),
                    ),
                )
            }
        } else {
            null
        }

    fun convertCustomFieldCriterionInput(it: List<*>?): List<CustomFieldCriterionInput>? =
        if (it != null) {
            it.map { item ->
                item as Map<String, *>
                CustomFieldCriterionInput(
                    field = item["field"].toString(),
                    value = Optional.presentIfNotNull(item["value"] as List<Any>?),
                    modifier = CriterionModifier.valueOf(item["modifier"]!!.toString()),
                )
            }
        } else {
            null
        }

    fun convertFilterMap(
        dataType: DataType,
        filterMap: Map<String, *>,
        useRandomSeed: Boolean = true,
    ): FilterArgs {
        val findFilterMap = filterMap.getCaseInsensitive("find_filter")
        val objectFilterMap = filterMap.getCaseInsensitive("object_filter")
        val findFilter = convertFindFilter(findFilterMap, useRandomSeed)
        val objectFilter = convertFilter(dataType, objectFilterMap)
        return FilterArgs(
            dataType,
            findFilter = findFilter,
            objectFilter = objectFilter,
        )
    }

    fun convertFindFilter(
        f: Any?,
        useRandomSeed: Boolean,
    ): StashFindFilter? =
        if (f is StashFindFilter) {
            f
        } else if (f == null) {
            null
        } else {
            val filter = f as Map<String, String>
            val sort = filter.getCaseInsensitive("sort")
            val direction =
                SortDirectionEnum.entries.find { it.name == (filter.getCaseInsensitive("direction")) }
                    ?: SortDirectionEnum.ASC
            val sortAndDirection =
                if (sort.isNotNullOrBlank()) {
                    SortAndDirection.create(sort, direction, useRandomSeed)
                } else {
                    null
                }
            StashFindFilter(filter.getCaseInsensitive("q"), sortAndDirection)
        }

    fun convertFilter(
        dataType: DataType,
        f: Any?,
    ): StashDataFilter? =
        when (dataType) {
            DataType.TAG -> convertTagFilterType(f)
            DataType.STUDIO -> convertStudioFilterType(f)
            DataType.GROUP -> convertGroupFilterType(f)
            DataType.SCENE -> convertSceneFilterType(f)
            DataType.IMAGE -> convertImageFilterType(f)
            DataType.GALLERY -> convertGalleryFilterType(f)
            DataType.MARKER -> convertSceneMarkerFilterType(f)
            DataType.PERFORMER -> convertPerformerFilterType(f)
        }
}
