package com.github.damontecres.stashapp.filter

import android.content.Context
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
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
import com.github.damontecres.stashapp.data.RANDOM_SORT_OPTION
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.filter.output.FilterWriter
import com.github.damontecres.stashapp.filter.output.getAllIds
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.getString
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

fun extractTitle(item: StashData): String? {
    return when (item) {
        is TagData -> item.name
        is PerformerData -> item.name
        is StudioData -> item.name
        is GalleryData -> item.title
        is ImageData -> item.title
        is MarkerData -> item.title
        is MovieData -> item.name
        is SlimSceneData -> item.titleOrFilename
        is FullSceneData -> item.titleOrFilename
        else -> throw IllegalArgumentException("${item::class.qualifiedName} not supported")
    }
}

fun extractDescription(item: StashData): String? {
    return when (item) {
        is TagData -> item.description?.ifBlank { null }
        is PerformerData -> item.disambiguation
        is StudioData -> null
        is GalleryData -> item.date
        is ImageData -> item.date
        is MarkerData -> "${item.scene.videoSceneData.titleOrFilename} (${durationToString(item.seconds)})"
        is MovieData -> item.date
        is SlimSceneData -> item.date
        is FullSceneData -> item.date
        else -> throw IllegalArgumentException("${item::class.qualifiedName} not supported")
    }
}

fun displayName(
    context: Context,
    gender: GenderEnum,
): String {
    return when (gender) {
        GenderEnum.MALE -> context.getString(R.string.stashapp_gender_types_MALE)
        GenderEnum.FEMALE -> context.getString(R.string.stashapp_gender_types_FEMALE)
        GenderEnum.TRANSGENDER_MALE -> context.getString(R.string.stashapp_gender_types_TRANSGENDER_MALE)
        GenderEnum.TRANSGENDER_FEMALE -> context.getString(R.string.stashapp_gender_types_TRANSGENDER_FEMALE)
        GenderEnum.INTERSEX -> context.getString(R.string.stashapp_gender_types_INTERSEX)
        GenderEnum.NON_BINARY -> context.getString(R.string.stashapp_gender_types_NON_BINARY)
        GenderEnum.UNKNOWN__ -> ""
    }
}

fun displayName(orientation: OrientationEnum): String {
    return orientation.rawValue.lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

fun findFilterSummary(
    context: Context,
    dataType: DataType,
    findFilter: StashFindFilter,
): String {
    val sortAndDirection = findFilter.sortAndDirection ?: dataType.defaultSort
    val sortOption =
        if (sortAndDirection.isRandom) {
            RANDOM_SORT_OPTION
        } else {
            dataType.sortOptions.firstOrNull { it.key == sortAndDirection.sort }
        }
    val sortName =
        if (sortOption != null) {
            context.getString(sortOption.nameStringId)
        } else {
            sortAndDirection.sort
        }
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
            CriterionModifier.IS_NULL -> ""
            CriterionModifier.NOT_NULL -> ""
            CriterionModifier.INCLUDES_ALL -> resolvedTitles.toString()
            CriterionModifier.INCLUDES -> resolvedTitles.toString()
            else -> throw IllegalArgumentException("${f.modifier}")
        }.ifBlank { null }
    // TODO excludes
    return if (toStr != null) {
        "$modStr $toStr"
    } else {
        modStr
    }
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
            CriterionModifier.IS_NULL -> ""
            CriterionModifier.NOT_NULL -> ""
            CriterionModifier.INCLUDES_ALL -> resolvedTitles.toString()
            CriterionModifier.INCLUDES -> resolvedTitles.toString()
            else -> throw IllegalArgumentException("${f.modifier}")
        }.ifBlank { null }
    // TODO excludes
    return if (toStr != null) {
        "$modStr $toStr"
    } else {
        modStr
    }
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

fun filterSummary(f: IntCriterionInput): String {
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
    if (distance != null) {
        return "$duplicated ($distance)"
    } else {
        return "$duplicated"
    }
}

fun resolutionName(res: ResolutionEnum): String {
    return when (res) {
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
}

fun filterSummary(f: ResolutionCriterionInput): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val name = resolutionName(f.value)
    return "$modStr $name"
}

fun filterSummary(f: OrientationCriterionInput): String {
    return f.value.map { v ->
        v.name.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }.toString()
}

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
    val resolvedTitles = values.map { displayName(StashApplication.getApplication(), it) }.orEmpty()
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

fun filterSummary(
    name: String,
    value: Any,
    idLookup: (DataType, List<String>) -> Map<String, CreateFilterViewModel.NameDescription?>,
): String {
    return when (value) {
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

        is Boolean, String -> value.toString()

        is MultiCriterionInput -> {
            val dataType = FilterWriter.TYPE_MAPPING[name]!!
            filterSummary(value, idLookup(dataType, value.getAllIds()))
        }

        is HierarchicalMultiCriterionInput -> {
            val dataType = FilterWriter.TYPE_MAPPING[name]!!
            filterSummary(value, idLookup(dataType, value.getAllIds()))
        }

        // TODO
        else -> value.toString()
    }
}

fun filterSummary(
    dataType: DataType,
    type: KClass<in StashDataFilter>,
    f: StashDataFilter,
    idLookup: (DataType, List<String>) -> Map<String, CreateFilterViewModel.NameDescription?>,
): String {
    val filterOptionNames = FilterOptions[dataType]!!.associateBy { it.name }
    val params =
        type.declaredMemberProperties.mapNotNull { param ->
            val obj = param.get(f) as Optional<*>
            val value = obj.getOrNull()
            if (value != null) {
                val valueStr = filterSummary(param.name, value, idLookup)
                val nameStringId = filterOptionNames[param.name]?.nameStringId
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

fun getIdsByDataType(
    type: KClass<in StashDataFilter>,
    f: StashDataFilter,
): Map<DataType, List<String>> {
    val result =
        buildMap {
            type.declaredMemberProperties.forEach { param ->
                val obj = param.get(f) as Optional<*>
                val value = obj.getOrNull()
                if (value != null) {
                    when (value) {
                        is MultiCriterionInput -> {
                            val dataType = FilterWriter.TYPE_MAPPING[param.name]!!
                            put(dataType, value.getAllIds())
                        }

                        is HierarchicalMultiCriterionInput -> {
                            val dataType = FilterWriter.TYPE_MAPPING[param.name]!!
                            put(dataType, value.getAllIds())
                        }
                    }
                }
            }
        }
    return result
}
