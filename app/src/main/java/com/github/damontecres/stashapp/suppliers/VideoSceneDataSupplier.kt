package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountScenesQuery
import com.github.damontecres.stashapp.api.FindVideoScenesQuery
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType

class VideoSceneDataSupplier(
    private val findFilter: FindFilterType?,
    private val sceneFilter: SceneFilterType? = null,
) : StashPagingSource.DataSupplier<FindVideoScenesQuery.Data, VideoSceneData, CountScenesQuery.Data> {
    override val dataType: DataType get() = DataType.SCENE

    override fun createQuery(filter: FindFilterType?): Query<FindVideoScenesQuery.Data> =
        FindVideoScenesQuery(
            filter = filter,
            scene_filter = sceneFilter,
            ids = null,
        )

    override fun parseQuery(data: FindVideoScenesQuery.Data): List<VideoSceneData> = data.findScenes.scenes.map { it.videoSceneData }

    override fun getDefaultFilter(): FindFilterType = findFilter ?: DataType.SCENE.asDefaultFindFilterType

    override fun createCountQuery(filter: FindFilterType?): Query<CountScenesQuery.Data> = CountScenesQuery(filter, sceneFilter, null)

    override fun parseCountQuery(data: CountScenesQuery.Data): Int = data.findScenes.count
}
