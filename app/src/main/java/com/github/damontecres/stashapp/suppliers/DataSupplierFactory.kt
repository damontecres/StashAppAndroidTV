package com.github.damontecres.stashapp.suppliers

import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.Version

class DataSupplierFactory(val serverVersion: Version) {
    fun create(
        dataType: DataType,
        findFilter: StashFindFilter? = StashFindFilter(null, dataType.defaultSort),
        objectFilter: Any? = null,
        override: DataSupplierOverride? = null,
    ): StashPagingSource.DataSupplier<*, *, *> {
        if (override != null) {
            TODO()
        } else {
            val filterParser = FilterParser(serverVersion)
            return when (dataType) {
                DataType.SCENE ->
                    SceneDataSupplier(
                        findFilter?.toFindFilterType(),
                        filterParser.convertSceneObjectFilter(objectFilter),
                    )
                else -> TODO()
            }
        }
    }
}

enum class DataSupplierOverride {
    PERFORMER_TAGS,
}
