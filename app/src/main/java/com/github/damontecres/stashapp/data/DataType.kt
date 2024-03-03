package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.FilterMode

enum class DataType(val filterMode: FilterMode, val stringId: Int, val pluralStringId: Int) {
    SCENE(FilterMode.SCENES, R.string.stashapp_scene, R.string.stashapp_scenes),
    MOVIE(FilterMode.MOVIES, R.string.stashapp_movie, R.string.stashapp_movies),
    MARKER(FilterMode.SCENE_MARKERS, R.string.stashapp_markers, R.string.stashapp_markers),
    PERFORMER(FilterMode.PERFORMERS, R.string.stashapp_performer, R.string.stashapp_performers),
    STUDIO(FilterMode.STUDIOS, R.string.stashapp_studio, R.string.stashapp_studios),
    TAG(FilterMode.TAGS, R.string.stashapp_tag, R.string.stashapp_tags),
    IMAGE(FilterMode.IMAGES, R.string.stashapp_image, R.string.stashapp_images),
//    GALLERY(FilterMode.GALLERIES, R.string.stashapp_gallery, R.string.stashapp_galleries),
    ;

    companion object {
        fun fromFilterMode(mode: FilterMode): DataType? {
            return DataType.entries.firstOrNull {
                it.filterMode == mode
            }
        }
    }
}
