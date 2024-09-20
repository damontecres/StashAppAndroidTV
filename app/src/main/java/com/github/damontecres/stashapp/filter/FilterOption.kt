package com.github.damontecres.stashapp.filter

import androidx.annotation.StringRes
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import kotlin.reflect.KClass

/**
 * A way to filter a particular [DataType]
 *
 * @param name the key for the filter
 * @param nameStringId ID for human readable name
 * @param dataType the type of this sub-filter, not the overarching filter
 * @param type the type of this sub-filter
 * @param getter how to get the value of this from the filter
 * @param setter how to set the value of this on the filter (returning a copy)
 */
data class FilterOption<FilterType : StashDataFilter, ValueType : Any>(
    val name: String,
    @StringRes val nameStringId: Int,
    val dataType: DataType?,
    val type: KClass<ValueType>,
    val getter: (FilterType) -> Optional<ValueType?>,
    val setter: (FilterType, Optional<ValueType?>) -> FilterType,
)

private val SceneFilterOptions =
    listOf(
        FilterOption<SceneFilterType, DateCriterionInput>(
            "date",
            R.string.stashapp_date,
            DataType.TAG,
            DateCriterionInput::class,
            { filter -> filter.date },
            { filter, value -> filter.copy(date = value) },
        ),
        FilterOption<SceneFilterType, StringCriterionInput>(
            "director",
            R.string.stashapp_director,
            null,
            StringCriterionInput::class,
            { it.director },
            { filter, value -> filter.copy(director = value) },
        ),
        FilterOption<SceneFilterType, MultiCriterionInput>(
            "movies",
            R.string.stashapp_movies,
            DataType.MOVIE,
            MultiCriterionInput::class,
            { it.movies },
            { filter, value -> filter.copy(movies = value) },
        ),
        FilterOption<SceneFilterType, IntCriterionInput>(
            "performer_age",
            R.string.stashapp_performer_age,
            null,
            IntCriterionInput::class,
            { it.performer_age },
            { filter, value -> filter.copy(performer_age = value) },
        ),
        FilterOption<SceneFilterType, IntCriterionInput>(
            "performer_count",
            R.string.stashapp_performer_count,
            null,
            IntCriterionInput::class,
            { it.performer_count },
            { filter, value -> filter.copy(performer_count = value) },
        ),
        FilterOption<SceneFilterType, Boolean>(
            "performer_favorite",
            R.string.stashapp_performer_favorite,
            null,
            Boolean::class,
            { it.performer_favorite },
            { filter, value -> filter.copy(performer_favorite = value) },
        ),
        FilterOption<SceneFilterType, HierarchicalMultiCriterionInput>(
            "performer_tags",
            R.string.stashapp_performer_tags,
            DataType.TAG,
            HierarchicalMultiCriterionInput::class,
            { filter -> filter.performer_tags },
            { filter, value -> filter.copy(performer_tags = value) },
        ),
        FilterOption<SceneFilterType, MultiCriterionInput>(
            "performers",
            R.string.stashapp_performers,
            DataType.PERFORMER,
            MultiCriterionInput::class,
            { it.performers },
            { filter, value -> filter.copy(performers = value) },
        ),
        FilterOption<SceneFilterType, IntCriterionInput>(
            "play_count",
            R.string.stashapp_play_count,
            null,
            IntCriterionInput::class,
            { it.play_count },
            { filter, value -> filter.copy(play_count = value) },
        ),
        FilterOption<SceneFilterType, IntCriterionInput>(
            "o_counter",
            R.string.stashapp_o_counter,
            null,
            IntCriterionInput::class,
            { it.o_counter },
            { filter, value -> filter.copy(o_counter = value) },
        ),
        FilterOption<SceneFilterType, IntCriterionInput>(
            "rating100",
            R.string.stashapp_rating,
            null,
            IntCriterionInput::class,
            { it.rating100 },
            { filter, value -> filter.copy(rating100 = value) },
        ),
        FilterOption<SceneFilterType, HierarchicalMultiCriterionInput>(
            "studios",
            R.string.stashapp_studios,
            DataType.STUDIO,
            HierarchicalMultiCriterionInput::class,
            { filter -> filter.studios },
            { filter, value -> filter.copy(studios = value) },
        ),
        FilterOption<SceneFilterType, HierarchicalMultiCriterionInput>(
            "tags",
            R.string.stashapp_tags,
            DataType.TAG,
            HierarchicalMultiCriterionInput::class,
            { filter -> filter.tags },
            { filter, value -> filter.copy(tags = value) },
        ),
        FilterOption<SceneFilterType, StringCriterionInput>(
            "title",
            R.string.stashapp_title,
            null,
            StringCriterionInput::class,
            { it.title },
            { filter, value -> filter.copy(title = value) },
        ),
    )

private val PerformerFilterOptions =
    listOf(
        FilterOption<PerformerFilterType, StringCriterionInput>(
            "name",
            R.string.stashapp_name,
            null,
            StringCriterionInput::class,
            { it.name },
            { filter, value -> filter.copy(name = value) },
        ),
        FilterOption<PerformerFilterType, HierarchicalMultiCriterionInput>(
            "tags",
            R.string.stashapp_tags,
            DataType.TAG,
            HierarchicalMultiCriterionInput::class,
            { filter -> filter.tags },
            { filter, value -> filter.copy(tags = value) },
        ),
    )

val FilterOptions =
    mapOf(
        DataType.SCENE to SceneFilterOptions,
        DataType.PERFORMER to PerformerFilterOptions,
    )

fun getFilterOptions(dataType: DataType): List<FilterOption<out StashDataFilter, out Any>> {
    return FilterOptions[dataType] ?: throw UnsupportedOperationException("$dataType")
}
