package com.github.damontecres.stashapp.ui.components.scene

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.lifecycle.MutableLiveData
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
import com.github.damontecres.stashapp.util.toLongMilliseconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.first
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

    val loadingState = MutableLiveData<SceneLoadingState>(SceneLoadingState.Loading)
    val tags = MutableLiveData<List<TagData>>(listOf())
    val performers = MutableLiveData<List<PerformerData>>(listOf())
    val galleries = MutableLiveData<List<GalleryData>>(listOf())
    val groups = MutableLiveData<List<GroupData>>(listOf())
    val markers = MutableLiveData<List<MarkerData>>(listOf())
    val studio = MutableLiveData<StudioData?>(null)
    val suggestions = MutableLiveData<List<SlimSceneData>>()

    val rating100 = MutableLiveData(0)
    val oCount = MutableLiveData(0)

    fun init(): SceneDetailsViewModel {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            try {
                val scene = queryEngine.getScene(sceneId)
                if (scene != null) {
                    rating100.value = scene.rating100 ?: 0
                    oCount.value = scene.o_counter ?: 0
                    tags.value = scene.tags.map { it.tagData }
                    groups.value = scene.groups.map { it.group.groupData }
                    markers.value = scene.scene_markers.map { it.asMarkerData(scene) }
                    studio.value = scene.studio?.studioData
                    this@SceneDetailsViewModel.scene = scene

                    loadingState.value = SceneLoadingState.Success(scene)
                    if (scene.performers.isNotEmpty()) {
                        performers.value =
                            queryEngine.findPerformers(performerIds = scene.performers.map { it.id })
                    }
                    if (scene.galleries.isNotEmpty()) {
                        galleries.value = queryEngine.getGalleries(scene.galleries.map { it.id })
                    }
                    if (!suggestions.isInitialized || suggestions.value?.isEmpty() == true) {
                        refreshSuggestions()
                    }
                } else {
                    loadingState.value = SceneLoadingState.Error
                }
            } catch (ex: Exception) {
                loadingState.value = SceneLoadingState.Error
                serverLogger.logException(ex)
            }
        }
        return this
    }

    private fun refreshSuggestions() {
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            scene?.let {
                suggestions.value = listOf()
                val filterArgs = createSceneSuggestionFilter(it)
                if (filterArgs != null) {
                    val supplier =
                        DataSupplierFactory(serverRepository.currentServerVersion)
                            .create<Query.Data, SlimSceneData, Query.Data>(filterArgs)
                    suggestions.value =
                        StashPagingSource<Query.Data, SlimSceneData, SlimSceneData, Query.Data>(
                            queryEngine.asUtilQueryEngine(),
                            supplier,
                        ).fetchPage(
                            1,
                            preferences.data
                                .first()
                                .searchPreferences.maxResults,
                        )
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
        val perfs = performers.value?.map { it.id }
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
                performers.value = results
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
        val ids = tags.value?.map { it.id }
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
                tags.value = results
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
        val ids = groups.value?.map { it.id }
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
                groups.value = results
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
            studio.value = result
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
                markers.value =
                    markers.value
                        ?.toMutableList()
                        ?.apply { add(m) }
                        ?.sortedBy { it.seconds }
                        ?: listOf(m)
                showAddMarker(m)
            }
        }
    }

    fun removeMarker(id: String) {
        viewModelScope.launch(exceptionHandler) {
            if (mutationEngine.deleteMarker(id)) {
                markers.value = markers.value?.filter { it.id != id }.orEmpty()
            }
        }
    }

    fun addGallery(id: String) = mutateGallery(id, AddRemove.ADD)

    fun removeGallery(id: String) = mutateGallery(id, AddRemove.REMOVE)

    private fun mutateGallery(
        id: String,
        op: AddRemove,
    ) {
        val ids = galleries.value?.map { it.id }
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
                galleries.value = results
                if (op == AddRemove.ADD) {
                    results.firstOrNull { it.id == id }?.let { showAddGallery(it) }
                }
            }
        }
    }

    fun updateOCount(action: suspend MutationEngine.(String) -> OCounter) {
        viewModelScope.launch(exceptionHandler) {
            val newOCount = action.invoke(mutationEngine, sceneId)
            oCount.value = newOCount.count
        }
    }

    fun updateRating(rating100: Int) {
        viewModelScope.launch(exceptionHandler) {
            val newRating =
                mutationEngine.setRating(sceneId, rating100)?.rating100 ?: 0
            this@SceneDetailsViewModel.rating100.value = newRating
            showSetRatingToast(StashApplication.getApplication(), newRating)
        }
    }

    fun deleteScene(
        deleteFiles: Boolean,
        deleteGenerated: Boolean,
        onDeleted: (Boolean) -> Unit,
    ) {
        loadingState.value = SceneLoadingState.Loading
        viewModelScope.launch(exceptionHandler) {
            val success = mutationEngine.deleteScene(sceneId, deleteFiles, deleteGenerated)
            onDeleted(success)
            if (!success) {
                scene?.let { loadingState.value = SceneLoadingState.Success(it) }
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
