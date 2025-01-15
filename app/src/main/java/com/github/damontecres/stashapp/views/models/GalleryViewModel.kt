package com.github.damontecres.stashapp.views.models

import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.util.QueryEngine

class GalleryViewModel : ItemViewModel<GalleryData>() {
    override suspend fun fetch(
        queryEngine: QueryEngine,
        id: String,
    ): GalleryData? = queryEngine.getGallery(id)
}
