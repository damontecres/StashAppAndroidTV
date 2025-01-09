package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

class MarkerDetailsViewModel : ViewModel() {
    val seconds = MutableLiveData<Double>()

    private val _item = EqualityMutableLiveData<FullSceneData.Scene_marker?>()
    val item: LiveData<FullSceneData.Scene_marker?> = _item

    private val _scene = EqualityMutableLiveData<FullSceneData?>()
    val scene: LiveData<FullSceneData?> = _scene

    fun init(
        id: String,
        sceneId: String,
    ) {
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            val queryEngine = QueryEngine(StashServer.requireCurrentServer())
            // TODO fetch by marker ID if supported in the future
            val sceneData = queryEngine.getScene(sceneId)!!
            _scene.value = sceneData
            val marker = sceneData.scene_markers.firstOrNull { it.id == id }
            _item.value = marker
            seconds.value = marker?.seconds
        }
    }

    fun setMarker(marker: FullMarkerData) {
        _item.value =
            FullSceneData.Scene_marker(
                id = marker.id,
                title = marker.title,
                seconds = marker.seconds,
                created_at = marker.created_at,
                updated_at = marker.updated_at,
                stream = marker.stream,
                preview = marker.preview,
                primary_tag =
                    FullSceneData.Primary_tag(
                        __typename = marker.primary_tag.__typename,
                        tagData = marker.primary_tag.tagData,
                    ),
                tags =
                    marker.tags.map {
                        FullSceneData.Tag(
                            __typename = it.__typename,
                            tagData = it.tagData,
                        )
                    },
                screenshot = marker.screenshot,
                __typename = marker.__typename,
            )
    }
}
