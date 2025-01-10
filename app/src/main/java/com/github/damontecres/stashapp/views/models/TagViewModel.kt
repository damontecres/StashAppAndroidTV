package com.github.damontecres.stashapp.views.models

import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.util.QueryEngine

class TagViewModel : ItemViewModel<TagData>() {
    override suspend fun fetch(
        queryEngine: QueryEngine,
        id: String,
    ): TagData? = queryEngine.getTag(id)
}
