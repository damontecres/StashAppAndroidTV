package com.github.damontecres.stashapp.views.models

import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.util.QueryEngine

class GroupViewModel : ItemViewModel<GroupData>() {
    override suspend fun fetch(
        queryEngine: QueryEngine,
        id: String,
    ): GroupData? = queryEngine.getGroup(id)
}
