package com.github.damontecres.stashapp.filter

import androidx.annotation.StringRes
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import kotlin.reflect.KClass

data class FilterOption<FilterType : StashDataFilter, ValueType : Any>(
    @StringRes val nameStringId: Int,
    val dataType: DataType?,
    val type: KClass<ValueType>,
    val getter: (FilterType) -> Optional<ValueType?>,
    val setter: (FilterType, Optional<ValueType?>) -> FilterType,
)

private val SceneFilterOptions =
    listOf(
        FilterOption<SceneFilterType, HierarchicalMultiCriterionInput>(
            R.string.stashapp_tags,
            DataType.TAG,
            HierarchicalMultiCriterionInput::class,
            { filter -> filter.tags },
            { filter, value -> filter.copy(tags = value) },
        ),
        FilterOption<SceneFilterType, MultiCriterionInput>(
            R.string.stashapp_performers,
            DataType.PERFORMER,
            MultiCriterionInput::class,
            { it.performers },
            { filter, value -> filter.copy(performers = value) },
        ),
        FilterOption<SceneFilterType, IntCriterionInput>(
            R.string.stashapp_performer_count,
            null,
            IntCriterionInput::class,
            { it.performer_count },
            { filter, value -> filter.copy(performer_count = value) },
        ),
        FilterOption<SceneFilterType, IntCriterionInput>(
            R.string.stashapp_performer_age,
            null,
            IntCriterionInput::class,
            { it.performer_age },
            { filter, value -> filter.copy(performer_age = value) },
        ),
        FilterOption<SceneFilterType, IntCriterionInput>(
            R.string.stashapp_o_counter,
            null,
            IntCriterionInput::class,
            { it.o_counter },
            { filter, value -> filter.copy(o_counter = value) },
        ),
        FilterOption<SceneFilterType, HierarchicalMultiCriterionInput>(
            R.string.stashapp_studios,
            DataType.STUDIO,
            HierarchicalMultiCriterionInput::class,
            { filter -> filter.studios },
            { filter, value -> filter.copy(studios = value) },
        ),
        FilterOption<SceneFilterType, MultiCriterionInput>(
            R.string.stashapp_movies,
            DataType.MOVIE,
            MultiCriterionInput::class,
            { it.movies },
            { filter, value -> filter.copy(movies = value) },
        ),
        FilterOption<SceneFilterType, Boolean>(
            R.string.stashapp_performer_favorite,
            null,
            Boolean::class,
            { it.performer_favorite },
            { filter, value -> filter.copy(performer_favorite = value) },
        ),
        FilterOption<SceneFilterType, StringCriterionInput>(
            R.string.stashapp_title,
            null,
            StringCriterionInput::class,
            { it.title },
            { filter, value -> filter.copy(title = value) },
        ),
        FilterOption<SceneFilterType, StringCriterionInput>(
            R.string.stashapp_director,
            null,
            StringCriterionInput::class,
            { it.director },
            { filter, value -> filter.copy(director = value) },
        ),
        FilterOption<SceneFilterType, IntCriterionInput>(
            R.string.stashapp_rating,
            null,
            IntCriterionInput::class,
            { it.rating100 },
            { filter, value -> filter.copy(rating100 = value) },
        ),
        FilterOption<SceneFilterType, IntCriterionInput>(
            R.string.stashapp_play_count,
            null,
            IntCriterionInput::class,
            { it.play_count },
            { filter, value -> filter.copy(play_count = value) },
        ),
    )

private val PerformerFilterOptions =
    listOf(
        FilterOption<PerformerFilterType, StringCriterionInput>(
            R.string.stashapp_name,
            null,
            StringCriterionInput::class,
            { it.name },
            { filter, value -> filter.copy(name = value) },
        ),
        FilterOption<PerformerFilterType, HierarchicalMultiCriterionInput>(
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
