package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.SortDirectionEnum

enum class DataType(
    val filterMode: FilterMode,
    val stringId: Int,
    val pluralStringId: Int,
    val defaultSort: SortAndDirection,
) {
    SCENE(
        FilterMode.SCENES,
        R.string.stashapp_scene,
        R.string.stashapp_scenes,
        SortAndDirection("date", SortDirectionEnum.DESC),
    ),
    MOVIE(
        FilterMode.MOVIES,
        R.string.stashapp_movie,
        R.string.stashapp_movies,
        SortAndDirection.NAME_ASC,
    ),
    MARKER(
        FilterMode.SCENE_MARKERS,
        R.string.stashapp_markers,
        R.string.stashapp_markers,
        SortAndDirection("created_at", SortDirectionEnum.DESC),
    ),
    PERFORMER(
        FilterMode.PERFORMERS,
        R.string.stashapp_performer,
        R.string.stashapp_performers,
        SortAndDirection.NAME_ASC,
    ),
    STUDIO(
        FilterMode.STUDIOS,
        R.string.stashapp_studio,
        R.string.stashapp_studios,
        SortAndDirection.NAME_ASC,
    ),
    TAG(FilterMode.TAGS, R.string.stashapp_tag, R.string.stashapp_tags, SortAndDirection.NAME_ASC),
    ;

    val asDefaultFindFilterType get() = defaultSort.asFindFilterType

    companion object {
        fun fromFilterMode(mode: FilterMode): DataType? {
            return DataType.entries.firstOrNull {
                it.filterMode == mode
            }
        }
    }
}
