package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.CountScenesQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType

class SceneDataSupplier(
    private val findFilter: FindFilterType?,
    private val sceneFilter: SceneFilterType? = null,
) :
    StashPagingSource.DataCountSupplier<FindScenesQuery.Data, SlimSceneData, CountScenesQuery.Data> {
    constructor(sceneFilter: SceneFilterType? = null) : this(
        DataType.SCENE.asDefaultFindFilterType,
        sceneFilter,
    )

    override val dataType: DataType get() = DataType.SCENE

    override fun createQuery(filter: FindFilterType?): Query<FindScenesQuery.Data> {
        return FindScenesQuery(
            filter = filter,
            scene_filter = sceneFilter,
            scene_ids = null,
        )
    }

    override fun parseQuery(data: FindScenesQuery.Data?): CountAndList<SlimSceneData> {
        val scenes = data?.findScenes?.scenes?.map { it.slimSceneData }.orEmpty()
        val count = StashPagingSource.UNSUPPORTED_COUNT
        return CountAndList(count, scenes)
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: DataType.SCENE.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<CountScenesQuery.Data> {
        return CountScenesQuery(filter, sceneFilter, null)
    }

    override fun parseCountQuery(data: CountScenesQuery.Data?): Int {
        return data?.findScenes?.count ?: StashPagingSource.INVALID_COUNT
    }
}
