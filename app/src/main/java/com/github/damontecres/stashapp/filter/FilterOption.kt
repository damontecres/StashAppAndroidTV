package com.github.damontecres.stashapp.filter

import androidx.annotation.StringRes
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import kotlin.reflect.KClass

data class FilterOption<FilterType, ValueType : Any>(
    @StringRes val nameStringId: Int,
    val type: KClass<ValueType>,
    val getter: (FilterType) -> Optional<ValueType?>,
    val setter: (FilterType, ValueType) -> FilterType,
)

val SceneFilterOptions =
    listOf(
        FilterOption<SceneFilterType, HierarchicalMultiCriterionInput>(
            R.string.stashapp_tags,
            HierarchicalMultiCriterionInput::class,
            { filter -> filter.tags },
            { filter, value -> filter.copy(tags = Optional.presentIfNotNull(value)) },
        ),
//        FilterOption(R.string.stashapp_performers, MultiCriterionInput::class),
        FilterOption<SceneFilterType, IntCriterionInput>(
            R.string.stashapp_performer_count,
            IntCriterionInput::class,
            { it.performer_count },
            { filter, value -> filter.copy(performer_count = Optional.presentIfNotNull(value)) },
        ),
    )

val SceneFilterOptionsMap = SceneFilterOptions.associateBy { it.nameStringId }
