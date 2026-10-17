package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.SceneMarkerUpdateInput
import com.github.damontecres.stashapp.di.server.MutationEngine
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.InterfaceService
import com.github.damontecres.stashapp.di.services.ItemClicker
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.di.services.PlayerFactory
import com.github.damontecres.stashapp.di.services.ServerLogger
import com.github.damontecres.stashapp.ui.showAddTag
import com.github.damontecres.stashapp.ui.showShort
import com.github.damontecres.stashapp.ui.util.DataLoadingState
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    val itemClicker: ItemClicker,
    val mutationEngine: MutationEngine,
    val navigationManager: NavigationManager,
    val playerFactory: PlayerFactory,
    private val interfaceService: InterfaceService,
    @InjectedParam private val id: String,
) : ViewModel() {
    private val _state = MutableStateFlow(MarkerDetailsState())
    val state: StateFlow<MarkerDetailsState> = _state

    fun init() {
        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
            val marker = queryEngine.getMarker(id)
            if (marker != null) {
                val title =
                    if (marker.title.isNotNullOrBlank()) {
                        marker.title
                    } else {
                        marker.primary_tag.tagData.name
                    }
                interfaceService.setTitle(title)

                _state.update {
                    it.copy(
                        item = DataLoadingState.Success(marker),
                        seconds = marker.seconds,
                        endSeconds = marker.end_seconds,
                        start = marker.seconds.seconds,
                        end = (marker.end_seconds ?: marker.seconds).seconds,
                        tags = marker.tags.map { it.tagData },
                    )
                }
            }
        }
    }

    fun setPrimaryTag(tagId: String) {
        viewModelScope.launch {
            val result =
                mutationEngine.updateMarker(
                    SceneMarkerUpdateInput(
                        id = id,
                        primary_tag_id = Optional.present(tagId),
                    ),
                )
            if (result != null) {
                update(result)
                showShort("Set primary tag to '${result.primary_tag.tagData.name}'")
            }
        }
    }

    fun addTag(tagId: String) {
        viewModelScope.launch {
            val tagIds =
                state.value.tags
                    .map { it.id }
                    .toMutableList()
            tagIds.add(tagId)
            val result =
                mutationEngine.updateMarker(
                    SceneMarkerUpdateInput(
                        id = id,
                        tag_ids = Optional.present(tagIds),
                    ),
                )
            if (result != null) {
                update(result)
                result.tags.firstOrNull { it.tagData.id == tagId }?.let { showAddTag(it.tagData) }
            }
        }
    }

    fun removeTag(tagId: String) {
        viewModelScope.launch {
            val tagIds =
                state.value.tags
                    .map { it.id }
                    .toMutableList()
            if (tagIds.remove(tagId)) {
                val result =
                    mutationEngine.updateMarker(
                        SceneMarkerUpdateInput(
                            id = id,
                            tag_ids = Optional.present(tagIds),
                        ),
                    )
                if (result != null) {
                    update(result)
                }
            }
        }
    }

    private fun update(result: FullMarkerData) {
        _state.update {
            it.copy(
                item = DataLoadingState.Success(result),
                tags = result.tags.map { it.tagData },
            )
        }
    }

    fun updateStart(start: Duration) {
        _state.update { it.copy(start = start) }
    }

    fun updateEnd(end: Duration) {
        _state.update { it.copy(end = end) }
    }

    companion object {
        private const val TAG = "MarkerDetailsViewModel"
    }
}

data class MarkerDetailsState(
    val item: DataLoadingState<FullMarkerData> = DataLoadingState.Pending,
    val seconds: Double = -1.0,
    val endSeconds: Double? = null,
    val start: Duration = Duration.ZERO,
    val end: Duration = Duration.ZERO,
    val tags: List<TagData> = emptyList(),
)
