package com.github.damontecres.stashapp.data

import android.content.Context
import android.os.Parcelable
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.util.toFind_filter
import kotlinx.parcelize.Parcelize

enum class FilterType {
    CUSTOM_FILTER,
    SAVED_FILTER,
    APP_FILTER,
    ;

    companion object {
        fun safeValueOf(value: String?): FilterType? {
            return entries.firstOrNull { it.name == value }
        }
    }
}

interface StashFilter : Parcelable {
    val filterType: FilterType

    val dataType: DataType
}

/**
 * A filter used internally by the app
 */
interface AppFilter : StashFilter {
    override val filterType: FilterType
        get() = FilterType.APP_FILTER

    /**
     * An optional name for the filter
     */
    val name: String?

    /**
     * The find filter to use, defaulting to the data type's default find filter
     */
    val findFilter: FindFilterType
        get() = dataType.asDefaultFindFilterType

    /**
     * The object filter such as a SceneFilterType/PerformerFilterType/etc
     */
    val objectFilter: Any?
        get() = null

    fun toSavedFilterData(context: Context): SavedFilterData {
        return SavedFilterData(
            id = "-1",
            mode = dataType.filterMode,
            name = name ?: context.getString(dataType.pluralStringId),
            find_filter = findFilter.toFind_filter(),
            object_filter = objectFilter,
            ui_options = null,
            __typename = javaClass.name,
        )
    }
}

@Parcelize
data class PerformTogetherAppFilter(override val name: String, val performerIds: List<String>) :
    AppFilter {
    override val dataType: DataType
        get() = DataType.SCENE

    override val objectFilter: Any
        get() =
            SceneFilterType(
                performers =
                    Optional.present(
                        MultiCriterionInput(
                            value = Optional.present(performerIds),
                            modifier = CriterionModifier.INCLUDES_ALL,
                        ),
                    ),
            )
}
