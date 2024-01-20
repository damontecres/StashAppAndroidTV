package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.FilterMode

enum class DataType(val filterMode: FilterMode, val stringId: Int, val pluralStringId: Int) {
    PERFORMER(FilterMode.PERFORMERS, R.string.stashapp_performer, R.string.stashapp_performers),
    SCENE(FilterMode.SCENES, R.string.stashapp_scene, R.string.stashapp_scenes),
    STUDIO(FilterMode.STUDIOS, R.string.stashapp_studio, R.string.stashapp_studios),
    TAG(FilterMode.TAGS, R.string.stashapp_tag, R.string.stashapp_tags),
    MOVIE(FilterMode.MOVIES, R.string.stashapp_movie, R.string.stashapp_movies),
}
