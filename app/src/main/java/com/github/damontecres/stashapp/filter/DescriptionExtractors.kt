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
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationCriterionInput
import com.github.damontecres.stashapp.api.type.PHashDuplicationCriterionInput
import com.github.damontecres.stashapp.api.type.PhashDistanceCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionCriterionInput
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StashIDCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.getString
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

fun findFilterSummary(
    context: Context,
    dataType: DataType,
    findFilter: StashFindFilter,
): String {
    val sortAndDirection = findFilter.sortAndDirection ?: dataType.defaultSort
    val sortOption = dataType.sortOptions.firstOrNull { it.key == sortAndDirection.sort }
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
    itemMap: Map<String, StashData>,
): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val value = f.value.getOrNull()
    // TODO
    // val resolvedTitles = value?.map { extractTitle(itemMap[it]!!) }.orEmpty()
    val resolvedTitles = value.orEmpty()
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
    itemMap: Map<String, StashData>,
): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    val value = f.value.getOrNull()
    // TODO
    // val resolvedTitles = value?.map { extractTitle(itemMap[it]!!) }.orEmpty()
    val resolvedTitles = value.orEmpty()
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

fun filterSummary(f: ResolutionCriterionInput): String {
    val modStr = f.modifier.getString(StashApplication.getApplication())
    // TODO map strings
    return "$modStr ${f.value.name}"
}

fun filterSummary(f: OrientationCriterionInput): String {
    // TODO map strings
    return f.value.map { it.name }.toString()
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

fun filterSummary(value: Any): String {
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

        is Boolean, String -> value.toString()

        // TODO look up IDs
        is MultiCriterionInput -> {
            filterSummary(value, mapOf())
        }

        is HierarchicalMultiCriterionInput -> {
            filterSummary(value, mapOf())
        }

        // TODO
        else -> value.toString()
    }
}

fun filterSummary(
    type: KClass<in StashDataFilter>,
    f: StashDataFilter,
): String {
    val params =
        type.declaredMemberProperties.mapNotNull { param ->
            val obj = param.get(f) as Optional<*>
            val value = obj.getOrNull()
            if (value != null) {
                val valueStr = filterSummary(value)
                param.name to valueStr
            } else {
                null
            }
        }.sortedBy { it.first }
    val text =
        params.joinToString("\n") {
            "${it.first}: ${it.second}"
        }
    return text
}
