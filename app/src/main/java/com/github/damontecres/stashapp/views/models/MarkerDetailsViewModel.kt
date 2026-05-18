package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.SceneMarkerUpdateInput
import com.github.damontecres.stashapp.di.server.MutationEngine
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.di.services.ServerLogger
import com.github.damontecres.stashapp.ui.showAddTag
import com.github.damontecres.stashapp.ui.showShort
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class MarkerDetailsViewModel(
    private val serverRepository: ServerRepository,
    private val serverLogger: ServerLogger,
    private val queryEngine: QueryEngine,
    val mutationEngine: MutationEngine,
    val navigationManager: NavigationManager,
    @InjectedParam private val id: String,
) : ViewModel() {
    val seconds = MutableLiveData<Double>()
    val endSeconds = MutableLiveData<Double?>(null)

    val start = EqualityMutableLiveData<Duration>()
    val end = EqualityMutableLiveData<Duration>()

    private val _item = EqualityMutableLiveData<FullMarkerData?>()
    val item: LiveData<FullMarkerData?> = _item

    private val _tags = MutableLiveData<List<TagData>>()
    val tags: LiveData<List<TagData>> = _tags

    private val _primaryTag = EqualityMutableLiveData<TagData>()
    val primaryTag: LiveData<TagData> = _primaryTag

    fun init() {
        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
            val marker = queryEngine.getMarker(id)
            _item.value = marker
            if (marker != null) {
                seconds.value = marker.seconds
                endSeconds.value = marker.end_seconds
                start.value = marker.seconds.seconds
                end.value = (marker.end_seconds ?: marker.seconds).seconds
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
                showShort("Set primary tag to '${result.primary_tag.tagData.name}'")
            }
        }
    }

    fun addTag(tagId: String) {
        viewModelScope.launch {
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
                result.tags.firstOrNull { it.tagData.id == tagId }?.let { showAddTag(it.tagData) }
            }
        }
    }

    fun removeTag(tagId: String) {
        viewModelScope.launch {
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
