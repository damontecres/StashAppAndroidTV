package com.github.damontecres.stashapp.filter

import android.content.Context
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
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
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StashIDCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.filter.output.FilterWriter
import com.github.damontecres.stashapp.filter.output.getAllIds
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.name
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.getRatingAsDecimalString
import com.github.damontecres.stashapp.views.getRatingString
import com.github.damontecres.stashapp.views.getString
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Get the default title for a [StashData] item usable as a sub-filter label
 */
fun extractTitle(item: StashData): String? =
    when (item) {
        is TagData -> item.name
        is PerformerData -> item.name
        is StudioData -> item.name
        is GalleryData -> item.name
        is ImageData -> item.title
        is MarkerData ->
            item.title.ifBlank {
                item.primary_tag.slimTagData.name
            }
        is GroupData -> item.name
        is SlimSceneData -> item.titleOrFilename
        is FullSceneData -> item.titleOrFilename
        else -> throw IllegalArgumentException("${item::class.qualifiedName} not supported")
    }

/**
 * Get the default description for a [StashData] item
 */
fun extractDescription(item: StashData): String? =
    when (item) {
        is TagData -> item.description?.ifBlank { null }
        is PerformerData -> item.disambiguation
        is StudioData -> null
        is GalleryData -> item.date
        is ImageData -> item.date
        is MarkerData -> "${item.scene.videoSceneData.titleOrFilename} (${durationToString(item.seconds)})"
        is GroupData -> item.date
        is SlimSceneData -> item.date
        is FullSceneData -> item.date
        else -> throw IllegalArgumentException("${item::class.qualifiedName} not supported")
    }

fun displayName(
    context: Context,
    gender: GenderEnum,
): String =
    when (gender) {
        GenderEnum.MALE -> context.getString(R.string.stashapp_gender_types_MALE)
        GenderEnum.FEMALE -> context.getString(R.string.stashapp_gender_types_FEMALE)
        GenderEnum.TRANSGENDER_MALE -> context.getString(R.string.stashapp_gender_types_TRANSGENDER_MALE)
        GenderEnum.TRANSGENDER_FEMALE -> context.getString(R.string.stashapp_gender_types_TRANSGENDER_FEMALE)
        GenderEnum.INTERSEX -> context.getString(R.string.stashapp_gender_types_INTERSEX)
        GenderEnum.NON_BINARY -> context.getString(R.string.stashapp_gender_types_NON_BINARY)
        GenderEnum.UNKNOWN__ -> ""
    }

fun displayName(orientation: OrientationEnum): String =
    orientation.rawValue
        .lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun findFilterSummary(
    context: Context,
    dataType: DataType,
    findFilter: StashFindFilter,
): String {
    val sortAndDirection = findFilter.sortAndDirection ?: dataType.defaultSort
    val sortName = sortAndDirection.sort.getString(context)
    val directionName =
        when (sortAndDirection.direction) {
            SortDirectionEnum.ASC -> context.getString(R.string.stashapp_ascending)
            SortDirectionEnum.DESC -> context.getString(R.string.stashapp_descending)
            SortDirectionEnum.UNKNOWN__ -> null
        }
    return "$sortName, $directionName"
}

fun filterSummary(
    f: MultiCriterionInput,
    itemMap: Map<String, CreateFilterViewModel.NameDescription?>,
): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val value = f.value.getOrNull()
    val resolvedTitles = value?.map { itemMap[it]?.name ?: it }.orEmpty()
    val toStr =
        when (f.modifier) {
            CriterionModifier.EQUALS -> resolvedTitles.firstOrNull() ?: ""
            CriterionModifier.INCLUDES_ALL -> resolvedTitles.toString()
            CriterionModifier.INCLUDES -> resolvedTitles.toString()

            // Short circuit and return
            CriterionModifier.IS_NULL, CriterionModifier.NOT_NULL -> return modStr
            else -> throw IllegalArgumentException("${f.modifier}")
        }.ifBlank { null }

    val resolvedExcludes =
        f.excludes
            .getOrNull()
            ?.map { itemMap[it]?.name ?: it }
            .orEmpty()
    val excludeStr =
        if (resolvedExcludes.isNotEmpty()) {
            val str =
                StashApplication
                    .getApplication()
                    .getString(R.string.stashapp_criterion_modifier_excludes)
            "$str $resolvedExcludes"
        } else {
            null
        }

    val strings = listOf(modStr, toStr, excludeStr)
    return strings.joinNotNullOrBlank(" ")
}

fun filterSummary(
    f: HierarchicalMultiCriterionInput,
    itemMap: Map<String, CreateFilterViewModel.NameDescription?>,
): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val value = f.value.getOrNull()
    val resolvedTitles = value?.map { itemMap[it]?.name ?: it }.orEmpty()
    val toStr =
        when (f.modifier) {
            CriterionModifier.EQUALS -> resolvedTitles.firstOrNull() ?: ""
            CriterionModifier.INCLUDES_ALL -> resolvedTitles.toString()
            CriterionModifier.INCLUDES -> resolvedTitles.toString()

            // Short circuit and return
            CriterionModifier.IS_NULL, CriterionModifier.NOT_NULL -> return modStr
            else -> throw IllegalArgumentException("${f.modifier}")
        }.ifBlank { null }

    val depth = f.depth.getOrNull()
    val depthStr =
        if (depth == -1) {
            val allStr = StashApplication.getApplication().getString(R.string.stashapp_all)
            "(+$allStr)"
        } else if (depth != null && depth > 0) {
            "(+$depth)"
        } else {
            null
        }
    val resolvedExcludes =
        f.excludes
            .getOrNull()
            ?.map { itemMap[it]?.name ?: it }
            .orEmpty()
    val excludeStr =
        if (resolvedExcludes.isNotEmpty()) {
            val str =
                StashApplication
                    .getApplication()
                    .getString(R.string.stashapp_criterion_modifier_excludes)
            "$str $resolvedExcludes"
        } else {
            null
        }

    val strings = listOf(modStr, toStr, depthStr, excludeStr)
    return strings.joinNotNullOrBlank(" ")
}

fun filterSummary(f: StringCriterionInput): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val value = f.value.ifBlank { null }
    return if (value != null) {
        "$modStr $value"
    } else {
        modStr
    }
}

fun filterSummaryRating(f: IntCriterionInput): String {
    val context = StashApplication.getApplication()
    val ratingsAsStars = StashServer.requireCurrentServer().serverPreferences.ratingsAsStars
    val modStr = f.modifier.getString(context)
    val value = f.value
    val value2 = f.value2.getOrNull()
    val toStr =
        when (f.modifier) {
            CriterionModifier.EQUALS,
            CriterionModifier.NOT_EQUALS,
            CriterionModifier.GREATER_THAN,
            CriterionModifier.LESS_THAN,
            -> getRatingString(value, ratingsAsStars)

            CriterionModifier.IS_NULL, CriterionModifier.NOT_NULL -> null

            CriterionModifier.BETWEEN, CriterionModifier.NOT_BETWEEN -> {
                val valueStr = getRatingAsDecimalString(value, ratingsAsStars)
                val value2Str = getRatingAsDecimalString(value2!!, ratingsAsStars)
                if (ratingsAsStars) {
                    val starsStr =
                        StashApplication
                            .getApplication()
                            .getString(R.string.stashapp_config_ui_editing_rating_system_type_options_stars)
                    "$valueStr & $value2Str $starsStr"
                } else {
                    "$valueStr & $value2Str"
                }
            }

            else -> throw IllegalArgumentException("${f.modifier}")
        }

    return if (toStr != null) {
        "$modStr $toStr"
    } else {
        modStr
    }
}

fun filterSummaryDuration(f: IntCriterionInput): String = filterSummary(f, { it.toDuration(DurationUnit.SECONDS).toString() })

fun filterSummary(
    f: IntCriterionInput,
    valueMapper: ((Int) -> String) = { it.toString() },
): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val value = valueMapper(f.value)
    val value2 = f.value2.getOrNull()?.let { valueMapper(it) }
    val toStr =
        when (f.modifier) {
            CriterionModifier.EQUALS,
            CriterionModifier.NOT_EQUALS,
            CriterionModifier.GREATER_THAN,
            CriterionModifier.LESS_THAN,
            -> value.toString()

            CriterionModifier.IS_NULL, CriterionModifier.NOT_NULL -> null

            CriterionModifier.BETWEEN, CriterionModifier.NOT_BETWEEN -> "$value & $value2"

            else -> throw IllegalArgumentException("${f.modifier}")
        }

    return if (toStr != null) {
        "$modStr $toStr"
    } else {
        modStr
    }
}

fun filterSummary(f: FloatCriterionInput): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val value = f.value
    val value2 = f.value2.getOrNull()
    val toStr =
        when (f.modifier) {
            CriterionModifier.EQUALS,
            CriterionModifier.NOT_EQUALS,
            CriterionModifier.GREATER_THAN,
            CriterionModifier.LESS_THAN,
            -> value.toString()

            CriterionModifier.IS_NULL, CriterionModifier.NOT_NULL -> null

            CriterionModifier.BETWEEN, CriterionModifier.NOT_BETWEEN -> "$value & $value2"

            else -> throw IllegalArgumentException("${f.modifier}")
        }

    return if (toStr != null) {
        "$modStr $toStr"
    } else {
        modStr
    }
}

fun filterSummary(f: PhashDistanceCriterionInput): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val distance = f.distance.getOrNull()
    if (distance != null) {
        return "$modStr ${f.value} ($distance)"
    } else {
        return "$modStr ${f.value}"
    }
}

fun filterSummary(f: PHashDuplicationCriterionInput): String {
    val duplicated = f.duplicated.getOrNull()
    val distance = f.distance.getOrNull()
    return if (distance != null) {
        "$duplicated ($distance)"
    } else {
        "$duplicated"
    }
}

fun resolutionName(res: ResolutionEnum): String =
    when (res) {
        ResolutionEnum.VERY_LOW -> "144p"
        ResolutionEnum.LOW -> "240p"
        ResolutionEnum.R360P -> "360p"
        ResolutionEnum.STANDARD -> "480p"
        ResolutionEnum.WEB_HD -> "540p"
        ResolutionEnum.STANDARD_HD -> "720p"
        ResolutionEnum.FULL_HD -> "1080p"
        ResolutionEnum.QUAD_HD -> "1440p"
        ResolutionEnum.VR_HD -> "1920p"
        ResolutionEnum.FOUR_K -> "4K"
        ResolutionEnum.FIVE_K -> "5K"
        ResolutionEnum.SIX_K -> "6K"
        ResolutionEnum.SEVEN_K -> "7K"
        ResolutionEnum.EIGHT_K -> "8K"
        ResolutionEnum.HUGE -> "8K+"
        ResolutionEnum.UNKNOWN__ -> "Unknown"
    }

fun filterSummary(f: ResolutionCriterionInput): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val name = resolutionName(f.value)
    return "$modStr $name"
}

fun filterSummary(f: OrientationCriterionInput): String =
    f.value
        .map { v ->
            v.name
                .lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }.toString()

fun filterSummary(f: StashIDCriterionInput): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val stashId = f.stash_id.getOrNull()
    val endpoint = f.endpoint.getOrNull()
    return if (endpoint != null) {
        "$modStr $stashId ($endpoint)"
    } else {
        "$modStr $stashId"
    }
}

fun filterSummary(f: CircumcisionCriterionInput): String {
    val context = StashApplication.getApplication()
    val modStr = f.modifier.getString(context)
    val strings =
        f.value.getOrNull().orEmpty().map {
            when (it) {
                CircumisedEnum.CUT -> context.getString(R.string.stashapp_circumcised_types_CUT)
                CircumisedEnum.UNCUT -> context.getString(R.string.stashapp_circumcised_types_UNCUT)
                CircumisedEnum.UNKNOWN__ -> "Unknown"
            }
        }
    return when (f.modifier) {
        CriterionModifier.IS_NULL, CriterionModifier.NOT_NULL -> modStr
        else -> "$modStr $strings"
    }
}

fun filterSummary(f: TimestampCriterionInput): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val value = f.value
    val value2 = f.value2.getOrNull()
    val toStr =
        when (f.modifier) {
            CriterionModifier.EQUALS,
            CriterionModifier.NOT_EQUALS,
            CriterionModifier.GREATER_THAN,
            CriterionModifier.LESS_THAN,
            -> value

            CriterionModifier.IS_NULL, CriterionModifier.NOT_NULL -> null

            CriterionModifier.BETWEEN, CriterionModifier.NOT_BETWEEN -> "$value & $value2"

            else -> throw IllegalArgumentException("${f.modifier}")
        }

    return if (toStr != null) {
        "$modStr $toStr"
    } else {
        modStr
    }
}

fun filterSummary(f: DateCriterionInput): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val value = f.value
    val value2 = f.value2.getOrNull()
    val toStr =
        when (f.modifier) {
            CriterionModifier.EQUALS,
            CriterionModifier.NOT_EQUALS,
            CriterionModifier.GREATER_THAN,
            CriterionModifier.LESS_THAN,
            -> value

            CriterionModifier.IS_NULL, CriterionModifier.NOT_NULL -> null

            CriterionModifier.BETWEEN, CriterionModifier.NOT_BETWEEN -> "$value & $value2"

            else -> throw IllegalArgumentException("${f.modifier}")
        }

    return if (toStr != null) {
        "$modStr $toStr"
    } else {
        modStr
    }
}

fun filterSummary(f: GenderCriterionInput): String {
    val values = f.value_list.getOrNull() ?: listOfNotNull(f.value.getOrNull())
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val resolvedTitles = values.map { displayName(StashApplication.getApplication(), it) }
    val toStr =
        when (f.modifier) {
            CriterionModifier.EQUALS,
            CriterionModifier.NOT_EQUALS,
            CriterionModifier.INCLUDES,
            CriterionModifier.EXCLUDES,
            -> resolvedTitles.toString()

            CriterionModifier.IS_NULL,
            CriterionModifier.NOT_NULL,
            -> ""

            else -> throw IllegalArgumentException("${f.modifier}")
        }.ifBlank { null }
    return if (toStr != null) {
        "$modStr $toStr"
    } else {
        modStr
    }
}

/**
 * Summarize a "sub-filter"
 *
 * @param name the sub-filter name
 * @param filterDataType the sub-filter's [DataType]
 * @param value the sub-filter value
 * @param idLookup a function associate IDs to a [CreateFilterViewModel.NameDescription]
 */
fun filterSummary(
    name: String,
    filterDataType: DataType,
    value: Any,
    idLookup: (DataType, List<String>) -> Map<String, CreateFilterViewModel.NameDescription?>,
): String =
    if (name == "rating100") {
        filterSummaryRating(value as IntCriterionInput)
    } else if (name == "duration") {
        filterSummaryDuration(value as IntCriterionInput)
    } else {
        when (value) {
            is IntCriterionInput -> filterSummary(value)
            is FloatCriterionInput -> filterSummary(value)
            is StringCriterionInput -> filterSummary(value)
            is PhashDistanceCriterionInput -> filterSummary(value)
            is PHashDuplicationCriterionInput -> filterSummary(value)
            is ResolutionCriterionInput -> filterSummary(value)
            is OrientationCriterionInput -> filterSummary(value)
            is StashIDCriterionInput -> filterSummary(value)
            is TimestampCriterionInput -> filterSummary(value)
            is DateCriterionInput -> filterSummary(value)
            is GenderCriterionInput -> filterSummary(value)
            is CircumcisionCriterionInput -> filterSummary(value)

            is Boolean, String -> value.toString()

            is MultiCriterionInput -> {
                val dataType = FilterWriter.getType(filterDataType, name)!!
                filterSummary(value, idLookup(dataType, value.getAllIds()))
            }

            is HierarchicalMultiCriterionInput -> {
                val dataType = FilterWriter.getType(filterDataType, name)!!
                filterSummary(value, idLookup(dataType, value.getAllIds()))
            }

            else -> value.toString()
        }
    }

/**
 * Summarize a filter
 *
 * @param dataType the filter [DataType]
 * @param type the filter's class
 * @param f the filter
 * @param idLookup a function associate IDs to a [CreateFilterViewModel.NameDescription]
 */
fun filterSummary(
    dataType: DataType,
    type: KClass<in StashDataFilter>,
    f: StashDataFilter,
    idLookup: (DataType, List<String>) -> Map<String, CreateFilterViewModel.NameDescription?>,
): String {
    val filterOptionNames = FilterOptions[dataType]!!.associateBy { it.name }
    val params =
        type.declaredMemberProperties
            .mapNotNull { param ->
                val obj = param.get(f) as Optional<*>
                val value = obj.getOrNull()
                if (value != null) {
                    val nameStringId = filterOptionNames[param.name]?.nameStringId
                    val valueStr = filterSummary(param.name, dataType, value, idLookup)
                    val key =
                        if (nameStringId != null) {
                            StashApplication.getApplication().getString(nameStringId)
                        } else {
                            param.name
                        }
                    key to valueStr
                } else {
                    null
                }
            }.sortedBy { it.first }
    val text =
        params.joinToString("\n") {
            "${it.first} ${it.second}"
        }
    return text
}

/**
 * Collect all of the IDs in the filter's [MultiCriterionInput] or [HierarchicalMultiCriterionInput] sub-filters and associate them by their [DataType]
 *
 * For example, a SceneFilterType.performers will associated [DataType.PERFORMER] to the list of performers IDs
 */
fun getIdsByDataType(
    filterDataType: DataType,
    f: StashDataFilter,
): Map<DataType, List<String>> {
    val result =
        buildMap {
            filterDataType.filterType.declaredMemberProperties.forEach { param ->
                val obj = param.get(f) as Optional<*>
                val value = obj.getOrNull()
                if (value != null) {
                    when (value) {
                        is MultiCriterionInput -> {
                            val dataType = FilterWriter.getType(filterDataType, param.name)!!
                            put(dataType, value.getAllIds())
                        }

                        is HierarchicalMultiCriterionInput -> {
                            val dataType = FilterWriter.getType(filterDataType, param.name)!!
                            put(dataType, value.getAllIds())
                        }
                    }
                }
            }
        }
    return result
}
