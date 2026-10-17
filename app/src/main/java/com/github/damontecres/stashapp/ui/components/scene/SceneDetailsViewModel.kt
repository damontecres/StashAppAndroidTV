package com.github.damontecres.stashapp.ui.components.scene

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.di.server.MutationEngine
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.InterfaceService
import com.github.damontecres.stashapp.di.services.ItemClicker
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.di.services.ServerLogger
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.ui.showAddGallery
import com.github.damontecres.stashapp.ui.showAddGroup
import com.github.damontecres.stashapp.ui.showAddMarker
import com.github.damontecres.stashapp.ui.showAddPerf
import com.github.damontecres.stashapp.ui.showAddTag
import com.github.damontecres.stashapp.ui.showSetStudio
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.asMarkerData
import com.github.damontecres.stashapp.util.createSceneSuggestionFilter
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.util.toLongMilliseconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.coroutines.CoroutineContext

@KoinViewModel
class SceneDetailsViewModel(
    private val context: Application,
    private val serverLogger: ServerLogger,
    private val queryEngine: QueryEngine,
    private val mutationEngine: MutationEngine,
    private val serverRepository: ServerRepository,
    private val preferences: DataStore<StashPreferences>,
    val navigationManager: NavigationManager,
    val itemClicker: ItemClicker,
    private val interfaceService: InterfaceService,
    @InjectedParam val sceneId: String,
) : ViewModel() {
    private val exceptionHandler =
        object : CoroutineExceptionHandler {
            override val key: CoroutineContext.Key<*>
                get() = CoroutineExceptionHandler

            override fun handleException(
                context: CoroutineContext,
                exception: Throwable,
            ) {
                Logger.e(exception) { "Exception" }
                viewModelScope.launchIO {
                    serverLogger.logException(exception, null)
                }
            }
        }

    val currentServer get() = serverRepository.currentServer

    private var scene: FullSceneData? = null

    private val _state = MutableStateFlow(SceneDetailsState())
    val state: StateFlow<SceneDetailsState> = _state

    fun init(): SceneDetailsViewModel {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            try {
                val scene = queryEngine.getScene(sceneId)
                if (scene != null) {
                    _state.update {
                        it.copy(
                            rating100 = scene.rating100 ?: 0,
                            oCount = scene.o_counter ?: 0,
                            tags = scene.tags.map { it.tagData },
                            groups = scene.groups.map { it.group.groupData },
                            markers = scene.scene_markers.map { it.asMarkerData(scene) },
                            studio = scene.studio?.studioData,
                        )
                    }
                    this@SceneDetailsViewModel.scene = scene

                    interfaceService.setTitle(scene.titleOrFilename)

                    _state.update {
                        it.copy(loadingState = SceneLoadingState.Success(scene))
                    }
                    if (scene.performers.isNotEmpty()) {
                        val performers =
                            queryEngine.findPerformers(performerIds = scene.performers.map { it.id })
                        _state.update { it.copy(performers = performers) }
                    }
                    if (scene.galleries.isNotEmpty()) {
                        val galleries = queryEngine.getGalleries(scene.galleries.map { it.id })
                        _state.update { it.copy(galleries = galleries) }
                    }
                    if (state.value.suggestions.isEmpty()) {
                        refreshSuggestions()
                    }
                } else {
                    _state.update { it.copy(loadingState = SceneLoadingState.Error) }
                }
            } catch (ex: Exception) {
                _state.update { it.copy(loadingState = SceneLoadingState.Error) }
                serverLogger.logException(ex)
            }
        }
        return this
    }

    private fun refreshSuggestions() {
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            scene?.let {
                _state.update { it.copy(suggestions = emptyList()) }
                val filterArgs = createSceneSuggestionFilter(it)
                if (filterArgs != null) {
                    val supplier =
                        DataSupplierFactory(serverRepository.currentServerVersion)
                            .create<Query.Data, SlimSceneData, Query.Data>(filterArgs)
                    val suggestions =
                        StashPagingSource<Query.Data, SlimSceneData, SlimSceneData, Query.Data>(
                            queryEngine,
                            supplier,
                        ).fetchPage(
                            1,
                            preferences.data
                                .first()
                                .searchPreferences.maxResults,
                        )
                    _state.update { it.copy(suggestions = suggestions) }
                }
            }
        }
    }

    fun addPerformer(performerId: String) = mutatePerformers(performerId, AddRemove.ADD)

    fun removePerformer(performerId: String) = mutatePerformers(performerId, AddRemove.REMOVE)

    private fun mutatePerformers(
        id: String,
        op: AddRemove,
    ) {
        val perfs = state.value.performers.map { it.id }
        perfs?.let {
            val mutable = it.toMutableList()
            when (op) {
                AddRemove.ADD -> mutable.add(id)
                AddRemove.REMOVE -> mutable.remove(id)
            }
            viewModelScope.launch(exceptionHandler) {
                val results =
                    mutationEngine
                        .setPerformersOnScene(sceneId, mutable)
                        ?.performers
                        ?.map { it.performerData }
                        .orEmpty()
                _state.update { it.copy(performers = results) }
                if (op == AddRemove.ADD) {
                    results.firstOrNull { it.id == id }?.let { showAddPerf(it) }
                }
                refreshSuggestions()
            }
        }
    }

    fun addTag(id: String) = mutateTags(id, AddRemove.ADD)

    fun removeTag(id: String) = mutateTags(id, AddRemove.REMOVE)

    private fun mutateTags(
        id: String,
        op: AddRemove,
    ) {
        val ids = state.value.tags.map { it.id }
        ids?.let {
            val mutable = it.toMutableList()
            when (op) {
                AddRemove.ADD -> mutable.add(id)
                AddRemove.REMOVE -> mutable.remove(id)
            }
            viewModelScope.launch(exceptionHandler) {
                val results =
                    mutationEngine
                        .setTagsOnScene(sceneId, mutable)
                        ?.tags
                        ?.map { it.tagData }
                        .orEmpty()
                _state.update { it.copy(tags = results) }
                if (op == AddRemove.ADD) {
                    results.firstOrNull { it.id == id }?.let { showAddTag(it) }
                }
                refreshSuggestions()
            }
        }
    }

    fun addGroup(id: String) = mutateGroup(id, AddRemove.ADD)

    fun removeGroup(id: String) = mutateGroup(id, AddRemove.REMOVE)

    private fun mutateGroup(
        id: String,
        op: AddRemove,
    ) {
        val ids = state.value.groups.map { it.id }
        ids?.let {
            val mutable = it.toMutableList()
            when (op) {
                AddRemove.ADD -> mutable.add(id)
                AddRemove.REMOVE -> mutable.remove(id)
            }
            viewModelScope.launch(exceptionHandler) {
                val results =
                    mutationEngine
                        .setGroupsOnScene(sceneId, mutable)
                        ?.groups
                        ?.map { it.group.groupData }
                        .orEmpty()
                _state.update { it.copy(groups = results) }
                if (op == AddRemove.ADD) {
                    results.firstOrNull { it.id == id }?.let { showAddGroup(it) }
                }
                refreshSuggestions()
            }
        }
    }

    fun setStudio(id: String) = mutateStudio(id)

    fun removeStudio() = mutateStudio(null)

    private fun mutateStudio(id: String?) {
        viewModelScope.launch(exceptionHandler) {
            val result = mutationEngine.setStudioOnScene(sceneId, id)?.studio?.studioData
            _state.update { it.copy(studio = result) }
            if (result != null) {
                showSetStudio(result)
            }
            refreshSuggestions()
        }
    }

    fun addMarker(marker: MarkerData) {
        viewModelScope.launch(exceptionHandler) {
            val newMarker =
                mutationEngine.createMarker(
                    sceneId,
                    marker.seconds.toLongMilliseconds,
                    marker.primary_tag.slimTagData.id,
                )
            newMarker?.let {
                val m = newMarker.asMarkerData(scene!!)
                val markers =
                    state.value.markers
                        .toMutableList()
                        .apply { add(m) }
                        .sortedBy { it.seconds }
                _state.update { it.copy(markers = markers) }
                showAddMarker(m)
            }
        }
    }

    fun removeMarker(id: String) {
        viewModelScope.launch(exceptionHandler) {
            if (mutationEngine.deleteMarker(id)) {
                _state.update { it.copy(markers = it.markers.filter { it.id != id }) }
            }
        }
    }

    fun addGallery(id: String) = mutateGallery(id, AddRemove.ADD)

    fun removeGallery(id: String) = mutateGallery(id, AddRemove.REMOVE)

    private fun mutateGallery(
        id: String,
        op: AddRemove,
    ) {
        val ids = state.value.galleries.map { it.id }
        ids?.let {
            val mutable = it.toMutableList()
            when (op) {
                AddRemove.ADD -> mutable.add(id)
                AddRemove.REMOVE -> mutable.remove(id)
            }
            viewModelScope.launch(exceptionHandler) {
                val results =
                    mutationEngine
                        .setGalleriesOnScene(sceneId, mutable)
                        ?.galleries
                        ?.map { it.galleryData }
                        .orEmpty()
                _state.update { it.copy(galleries = results) }
                if (op == AddRemove.ADD) {
                    results.firstOrNull { it.id == id }?.let { showAddGallery(it) }
                }
            }
        }
    }

    fun updateOCount(action: suspend MutationEngine.(String) -> OCounter) {
        viewModelScope.launch(exceptionHandler) {
            val newOCount = action.invoke(mutationEngine, sceneId)
            _state.update { it.copy(oCount = newOCount.count) }
        }
    }

    fun updateRating(rating100: Int) {
        viewModelScope.launch(exceptionHandler) {
            val newRating =
                mutationEngine.setRating(sceneId, rating100)?.rating100 ?: 0
            _state.update { it.copy(rating100 = newRating) }
            showSetRatingToast(StashApplication.getApplication(), newRating)
        }
    }

    fun deleteScene(
        deleteFiles: Boolean,
        deleteGenerated: Boolean,
        onDeleted: (Boolean) -> Unit,
    ) {
        _state.update { it.copy(loadingState = SceneLoadingState.Loading) }
        viewModelScope.launch(exceptionHandler) {
            val success = mutationEngine.deleteScene(sceneId, deleteFiles, deleteGenerated)
            onDeleted(success)
            if (!success) {
                scene?.let { scene ->
                    _state.update { it.copy(loadingState = SceneLoadingState.Success(scene)) }
                }
            }
        }
    }
}

sealed class SceneLoadingState {
    data object Loading : SceneLoadingState()

    data object Error : SceneLoadingState()

    data class Success(
        val scene: FullSceneData,
    ) : SceneLoadingState()
}

enum class AddRemove {
    ADD,
    REMOVE,
    ;

    fun exec(
        id: String,
        list: MutableList<String>,
    ) {
        if (this == ADD) {
            list.add(id)
        } else {
            list.remove(id)
        }
    }
}

data class SceneDetailsState(
    val loadingState: SceneLoadingState = SceneLoadingState.Loading,
    val tags: List<TagData> = emptyList(),
    val performers: List<PerformerData> = emptyList(),
    val galleries: List<GalleryData> = emptyList(),
    val groups: List<GroupData> = emptyList(),
    val markers: List<MarkerData> = emptyList(),
    val studio: StudioData? = null,
    val suggestions: List<SlimSceneData> = emptyList(),
    val rating100: Int = 0,
    val oCount: Int = 0,
)
