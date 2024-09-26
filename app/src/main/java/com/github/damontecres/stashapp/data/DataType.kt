package com.github.damontecres.stashapp.data

import androidx.annotation.StringRes
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.presenters.ImagePresenter
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.presenters.MoviePresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import kotlin.reflect.KClass

/**
 * One of the eight core types of data that the server uses
 */
enum class DataType(
    /**
     * The equivalent [FilterMode] for this data type
     */
    val filterMode: FilterMode,
    /**
     * The string resource for the singular form of the data type
     */
    @StringRes val stringId: Int,
    /**
     * The string resource for the plural form of the data type
     */
    @StringRes val pluralStringId: Int,
    /**
     * The string resource for the icon form of the data type
     */
    @StringRes val iconStringId: Int,
    /**
     * The default sort for items of this data type
     */
    val defaultSort: SortAndDirection,
    /**
     * The available options for sorting this data type
     */
    val sortOptions: List<SortOption>,
) {
    SCENE(
        FilterMode.SCENES,
        R.string.stashapp_scene,
        R.string.stashapp_scenes,
        R.string.fa_circle_play,
        SortAndDirection("date", SortDirectionEnum.DESC),
        SCENE_SORT_OPTIONS,
    ),
    MOVIE(
        FilterMode.MOVIES,
        R.string.stashapp_movie,
        R.string.stashapp_movies,
        R.string.fa_film,
        SortAndDirection.NAME_ASC,
        MOVIE_SORT_OPTIONS,
    ),
    MARKER(
        FilterMode.SCENE_MARKERS,
        R.string.stashapp_markers,
        R.string.stashapp_markers,
        R.string.fa_location_dot,
        SortAndDirection("created_at", SortDirectionEnum.DESC),
        MARKER_SORT_OPTIONS,
    ),
    PERFORMER(
        FilterMode.PERFORMERS,
        R.string.stashapp_performer,
        R.string.stashapp_performers,
        R.string.fa_user,
        SortAndDirection.NAME_ASC,
        PERFORMER_SORT_OPTIONS,
    ),
    STUDIO(
        FilterMode.STUDIOS,
        R.string.stashapp_studio,
        R.string.stashapp_studios,
        R.string.fa_video,
        SortAndDirection.NAME_ASC,
        STUDIO_SORT_OPTIONS,
    ),
    TAG(
        FilterMode.TAGS,
        R.string.stashapp_tag,
        R.string.stashapp_tags,
        R.string.fa_tag,
        SortAndDirection.NAME_ASC,
        TAG_SORT_OPTIONS,
    ),
    IMAGE(
        FilterMode.IMAGES,
        R.string.stashapp_image,
        R.string.stashapp_images,
        R.string.fa_image,
        SortAndDirection.PATH_ASC,
        IMAGE_SORT_OPTIONS,
    ),
    GALLERY(
        FilterMode.GALLERIES,
        R.string.stashapp_gallery,
        R.string.stashapp_galleries,
        R.string.fa_images,
        SortAndDirection.PATH_ASC,
        GALLERY_SORT_OPTIONS,
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

    val filterType
        get() =
            when (this) {
                SCENE -> SceneFilterType::class
                MOVIE -> MovieFilterType::class
                MARKER -> SceneMarkerFilterType::class
                PERFORMER -> PerformerFilterType::class
                STUDIO -> StudioFilterType::class
                TAG -> TagFilterType::class
                IMAGE -> ImageFilterType::class
                GALLERY -> GalleryFilterType::class
            } as KClass<StashDataFilter>

    val defaultCardRatio
        get() =
            ScenePresenter.CARD_WIDTH.toDouble() / defaultCardWidth

    val supportsPlaylists get() = this == SCENE || this == MARKER

    companion object {
        fun fromFilterMode(mode: FilterMode): DataType? {
            return DataType.entries.firstOrNull {
                it.filterMode == mode
            }
        }
    }
}
