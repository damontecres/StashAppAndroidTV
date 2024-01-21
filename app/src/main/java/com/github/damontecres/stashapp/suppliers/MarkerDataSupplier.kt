package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindMarkersQuery
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.StashPagingSource

class MarkerDataSupplier(
    private val findFilter: FindFilterType?,
    private val markerFilter: SceneMarkerFilterType? = null,
) :
    StashPagingSource.DataSupplier<FindMarkersQuery.Data, MarkerData> {
    override val dataType: DataType get() = DataType.MARKER

    override fun createQuery(filter: FindFilterType?): Query<FindMarkersQuery.Data> {
        return FindMarkersQuery(
            filter = filter,
            markerFilter,
        )
    }

    override fun parseQuery(data: FindMarkersQuery.Data?): CountAndList<MarkerData> {
        val markers = data?.findSceneMarkers?.scene_markers?.map { it.markerData }.orEmpty()
        val count = data?.findSceneMarkers?.count ?: -1
        return CountAndList(count, markers)
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: FindFilterType()
    }
}
