package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.presenters.StashPagingSource

class SceneDataSupplier(private val sceneFilter: SceneFilterType? = null) :
    StashPagingSource.DataSupplier<FindScenesQuery.Data, SlimSceneData> {
    override fun createQuery(filter: FindFilterType?): Query<FindScenesQuery.Data> {
        return FindScenesQuery(
            filter = Optional.presentIfNotNull(filter),
            scene_filter = Optional.presentIfNotNull(sceneFilter)
        )
    }

    override fun parseQuery(data: FindScenesQuery.Data?): CountAndList<SlimSceneData> {
        val scenes = data?.findScenes?.scenes?.map { it.slimSceneData }.orEmpty()
        val count = data?.findScenes?.count ?: -1
        return CountAndList(count, scenes)
    }

    override fun getSortKey(): String? {
        return "date"
    }
}
