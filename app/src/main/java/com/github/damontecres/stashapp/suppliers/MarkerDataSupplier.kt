package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountMarkersQuery
import com.github.damontecres.stashapp.api.FindMarkersQuery
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.data.DataType

class MarkerDataSupplier(
    private val findFilter: FindFilterType?,
    private val markerFilter: SceneMarkerFilterType? = null,
) :
    StashPagingSource.DataSupplier<FindMarkersQuery.Data, MarkerData, CountMarkersQuery.Data> {
    constructor(markerFilter: SceneMarkerFilterType? = null) : this(
        DataType.MARKER.asDefaultFindFilterType,
        markerFilter,
    )

    override val dataType: DataType get() = DataType.MARKER

    override fun createQuery(filter: FindFilterType?): Query<FindMarkersQuery.Data> {
        return FindMarkersQuery(
            filter = filter,
            markerFilter,
        )
    }

    override fun parseQuery(data: FindMarkersQuery.Data): List<MarkerData> {
        return data.findSceneMarkers.scene_markers.map { it.markerData }
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: DataType.MARKER.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<CountMarkersQuery.Data> {
        return CountMarkersQuery(filter, markerFilter)
    }

    override fun parseCountQuery(data: CountMarkersQuery.Data): Int {
        return data.findSceneMarkers.count
    }
}
