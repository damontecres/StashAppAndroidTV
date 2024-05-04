package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.presenters.ImagePresenter
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.presenters.MoviePresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter

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
    IMAGE(
        FilterMode.IMAGES,
        R.string.stashapp_image,
        R.string.stashapp_images,
        SortAndDirection.PATH_ASC,
    ),
    GALLERY(
        FilterMode.GALLERIES,
        R.string.stashapp_gallery,
        R.string.stashapp_galleries,
        SortAndDirection.PATH_ASC,
    ),
    ;

    val asDefaultFindFilterType get() = defaultSort.asFindFilterType

    val defaultCardWidth
        get() =
            when (this) {
                SCENE -> ScenePresenter.CARD_WIDTH
                MOVIE -> MoviePresenter.CARD_WIDTH
                MARKER -> MarkerPresenter.CARD_WIDTH
                PERFORMER -> PerformerPresenter.CARD_WIDTH
                STUDIO -> StudioPresenter.CARD_WIDTH
                TAG -> TagPresenter.CARD_WIDTH
                IMAGE -> ImagePresenter.CARD_WIDTH
                GALLERY -> GalleryPresenter.CARD_WIDTH
            }

    companion object {
        fun fromFilterMode(mode: FilterMode): DataType? {
            return DataType.entries.firstOrNull {
                it.filterMode == mode
            }
        }
    }
}
