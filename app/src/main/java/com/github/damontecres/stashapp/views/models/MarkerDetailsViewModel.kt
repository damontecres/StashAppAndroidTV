package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.SceneMarkerUpdateInput
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

class MarkerDetailsViewModel : ViewModel() {
    private lateinit var server: StashServer

    val seconds = MutableLiveData<Double>()
    val endSeconds = MutableLiveData<Double?>(null)

    private val _item = EqualityMutableLiveData<FullMarkerData?>()
    val item: LiveData<FullMarkerData?> = _item

    private val _tags = MutableLiveData<List<TagData>>()
    val tags: LiveData<List<TagData>> = _tags

    private val _primaryTag = EqualityMutableLiveData<TagData>()
    val primaryTag: LiveData<TagData> = _primaryTag

    fun init(
        server: StashServer,
        id: String,
    ) {
        this.server = server
        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
            val queryEngine = QueryEngine(server)
            val marker = queryEngine.getMarker(id)
            _item.value = marker
            if (marker != null) {
                seconds.value = marker.seconds
                endSeconds.value = marker.end_seconds
                _primaryTag.value = marker.primary_tag.tagData
                _tags.value = marker.tags.map { it.tagData }
            }
        }
    }

    fun setMarker(marker: FullMarkerData) {
        _item.value = marker
    }

    fun setPrimaryTag(tagId: String) {
        viewModelScope.launch {
            val mutationEngine = MutationEngine(server)
            val result =
                mutationEngine.updateMarker(
                    SceneMarkerUpdateInput(
                        id = item.value!!.id,
                        primary_tag_id = Optional.present(tagId),
                    ),
                )
            if (result != null) {
                _item.value = result
                _primaryTag.value = result.primary_tag.tagData
                _tags.value = result.tags.map { it.tagData }
            }
        }
    }

    fun addTag(tagId: String) {
        viewModelScope.launch {
            val mutationEngine = MutationEngine(server)
            val tagIds = tags.value!!.map { it.id }.toMutableList()
            tagIds.add(tagId)
            val result =
                mutationEngine.updateMarker(
                    SceneMarkerUpdateInput(
                        id = item.value!!.id,
                        tag_ids = Optional.present(tagIds),
                    ),
                )
            if (result != null) {
                _item.value = result
                _primaryTag.value = result.primary_tag.tagData
                _tags.value = result.tags.map { it.tagData }
            }
        }
    }

    fun removeTag(tagId: String) {
        viewModelScope.launch {
            val mutationEngine = MutationEngine(server)
            val tagIds = tags.value!!.map { it.id }.toMutableList()
            if (tagIds.remove(tagId)) {
                val result =
                    mutationEngine.updateMarker(
                        SceneMarkerUpdateInput(
                            id = item.value!!.id,
                            tag_ids = Optional.present(tagIds),
                        ),
                    )
                if (result != null) {
                    _item.value = result
                    _primaryTag.value = result.primary_tag.tagData
                    _tags.value = result.tags.map { it.tagData }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MarkerDetailsViewModel"
    }
}
