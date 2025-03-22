package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountMarkersQuery
import com.github.damontecres.stashapp.api.FindFullMarkersQuery
import com.github.damontecres.stashapp.api.FindMarkersQuery
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.data.DataType

class MarkerDataSupplier(
    private val findFilter: FindFilterType?,
    private val markerFilter: SceneMarkerFilterType? = null,
) : StashPagingSource.DataSupplier<FindMarkersQuery.Data, MarkerData, CountMarkersQuery.Data> {
    constructor(markerFilter: SceneMarkerFilterType? = null) : this(
        DataType.MARKER.asDefaultFindFilterType,
        markerFilter,
    )

    override val dataType: DataType get() = DataType.MARKER

    override fun createQuery(filter: FindFilterType?): Query<FindMarkersQuery.Data> =
        FindMarkersQuery(
            filter = filter,
            scene_marker_filter = markerFilter,
            ids = null,
        )

    override fun parseQuery(data: FindMarkersQuery.Data): List<MarkerData> = data.findSceneMarkers.scene_markers.map { it.markerData }

    override fun getDefaultFilter(): FindFilterType = findFilter ?: DataType.MARKER.asDefaultFindFilterType

    override fun createCountQuery(filter: FindFilterType?): Query<CountMarkersQuery.Data> = CountMarkersQuery(filter, markerFilter)

    override fun parseCountQuery(data: CountMarkersQuery.Data): Int = data.findSceneMarkers.count
}

class FullMarkerDataSupplier(
    private val findFilter: FindFilterType?,
    private val markerFilter: SceneMarkerFilterType? = null,
) : StashPagingSource.DataSupplier<FindFullMarkersQuery.Data, FullMarkerData, CountMarkersQuery.Data> {
    constructor(markerFilter: SceneMarkerFilterType? = null) : this(
        DataType.MARKER.asDefaultFindFilterType,
        markerFilter,
    )

    override val dataType: DataType get() = DataType.MARKER

    override fun createQuery(filter: FindFilterType?): Query<FindFullMarkersQuery.Data> =
        FindFullMarkersQuery(
            filter = filter,
            scene_marker_filter = markerFilter,
            ids = null,
        )

    override fun parseQuery(data: FindFullMarkersQuery.Data): List<FullMarkerData> =
        data.findSceneMarkers.scene_markers.map { it.fullMarkerData }

    override fun getDefaultFilter(): FindFilterType = findFilter ?: DataType.MARKER.asDefaultFindFilterType

    override fun createCountQuery(filter: FindFilterType?): Query<CountMarkersQuery.Data> = CountMarkersQuery(filter, markerFilter)

    override fun parseCountQuery(data: CountMarkersQuery.Data): Int = data.findSceneMarkers.count
}
