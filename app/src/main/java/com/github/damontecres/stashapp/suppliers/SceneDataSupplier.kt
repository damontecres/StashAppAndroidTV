package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountScenesQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType

class SceneDataSupplier(
    private val findFilter: FindFilterType?,
    private val sceneFilter: SceneFilterType? = null,
) : StashPagingSource.DataSupplier<FindScenesQuery.Data, SlimSceneData, CountScenesQuery.Data> {
    override val dataType: DataType get() = DataType.SCENE

    override fun createQuery(filter: FindFilterType?): Query<FindScenesQuery.Data> =
        FindScenesQuery(
            filter = filter,
            scene_filter = sceneFilter,
            ids = null,
        )

    override fun parseQuery(data: FindScenesQuery.Data): List<SlimSceneData> = data.findScenes.scenes.map { it.slimSceneData }

    override fun getDefaultFilter(): FindFilterType = findFilter ?: DataType.SCENE.asDefaultFindFilterType

    override fun createCountQuery(filter: FindFilterType?): Query<CountScenesQuery.Data> = CountScenesQuery(filter, sceneFilter, null)

    override fun parseCountQuery(data: CountScenesQuery.Data): Int = data.findScenes.count
}
