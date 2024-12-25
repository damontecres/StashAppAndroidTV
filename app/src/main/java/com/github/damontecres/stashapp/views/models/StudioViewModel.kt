package com.github.damontecres.stashapp.views.models

import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.util.QueryEngine

class StudioViewModel : ItemViewModel<StudioData>() {
    override suspend fun fetch(
        queryEngine: QueryEngine,
        id: String,
    ): StudioData? = queryEngine.findStudios(studioIds = listOf(id)).firstOrNull()
}
