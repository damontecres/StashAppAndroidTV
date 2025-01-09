package com.github.damontecres.stashapp.views.models

import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.util.QueryEngine

class PerformerViewModel : ItemViewModel<PerformerData>() {
    override suspend fun fetch(
        queryEngine: QueryEngine,
        id: String,
    ): PerformerData? = queryEngine.getPerformer(id)
}
