package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.api.type.FilterMode

enum class DataType(val filterMode: FilterMode) {
    PERFORMER(FilterMode.PERFORMERS),
    SCENE(FilterMode.SCENES),
    STUDIO(FilterMode.STUDIOS),
    TAG(FilterMode.TAGS),
    MOVIE(FilterMode.MOVIES),
}
