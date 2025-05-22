package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.asMarkerData
import com.github.damontecres.stashapp.util.createSceneSuggestionFilter
import kotlinx.coroutines.launch

class SceneViewModel : ViewModel() {
    private val _scene = EqualityMutableLiveData<FullSceneData?>()
    val scene: LiveData<FullSceneData?> = _scene

    val currentPosition = MutableLiveData<Long>()

    private val _performers = EqualityMutableLiveData<List<PerformerData>>()
    val performers: LiveData<List<PerformerData>> = _performers

    private val _galleries = EqualityMutableLiveData<List<GalleryData>>()
    val galleries: LiveData<List<GalleryData>> = _galleries

    private val _tags = EqualityMutableLiveData<List<TagData>>()
    val tags: LiveData<List<TagData>> = _tags

    private val _markers = EqualityMutableLiveData<List<MarkerData>>()
    val markers: LiveData<List<MarkerData>> = _markers

    private val _groups = EqualityMutableLiveData<List<GroupData>>()
    val groups: LiveData<List<GroupData>> = _groups

    private val _suggestedScenes = MutableLiveData<List<SlimSceneData>>()
    val suggestedScenes: LiveData<List<SlimSceneData>> = _suggestedScenes

    /**
     * Saves the current position for the current scene
     */
    fun saveCurrentPosition() {
        val currentScene = scene.value
        val position = currentPosition.value
        if (currentScene != null && position != null) {
            viewModelScope.launch(StashCoroutineExceptionHandler()) {
                val mutationEngine = MutationEngine(StashServer.requireCurrentServer())
                mutationEngine.saveSceneActivity(currentScene.id, position)
            }
        }
    }

    /**
     * Initialize the [FullSceneData] for the given id. Optionally also fetch extra data such as galleries and performers.
     */
    fun init(
        id: String,
        fetchAll: Boolean,
    ) {
        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
            val queryEngine = QueryEngine(StashServer.requireCurrentServer())
            val newScene = queryEngine.getScene(id)
            if (newScene == null) {
                _scene.setValueNoCheck(null)
            } else {
                _scene.value = newScene
                val currPos = currentPosition.value
                if (currPos == null || currPos <= 0L) {
                    currentPosition.value = (newScene.resume_time ?: 0.0).times(1000L).toLong()
                }

                _tags.value = newScene.tags.map { it.tagData }
                _markers.value = newScene.scene_markers.map { it.asMarkerData(newScene) }
                _groups.value = newScene.groups.map { it.group.groupData }

                if (fetchAll) {
                    val performerIds = newScene.performers.map { it.id }
                    if (performerIds.isNotEmpty()) {
                        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
                            _performers.value =
                                queryEngine.findPerformers(performerIds = performerIds)
                        }
                    }
                    if (newScene.galleries.isNotEmpty()) {
                        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
                            _galleries.value =
                                queryEngine.getGalleries(newScene.galleries.map { it.id })
                        }
                    }

                    // Suggestions
                    if (!_suggestedScenes.isInitialized || _suggestedScenes.value!!.isEmpty()) {
                        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
                            val filterArgs = createSceneSuggestionFilter(newScene)
                            if (filterArgs != null) {
                                val pageSize =
                                    PreferenceManager
                                        .getDefaultSharedPreferences(StashApplication.getApplication())
                                        .getInt(
                                            StashApplication
                                                .getApplication()
                                                .getString(R.string.pref_key_max_search_results),
                                            25,
                                        )
                                val supplier =
                                    DataSupplierFactory(
                                        StashServer.requireCurrentServer().version,
                                    ).create<Query.Data, SlimSceneData, Query.Data>(
                                        filterArgs,
                                    )
                                _suggestedScenes.value =
                                    StashPagingSource<Query.Data, SlimSceneData, SlimSceneData, Query.Data>(
                                        QueryEngine(StashServer.requireCurrentServer()),
                                        supplier,
                                    ).fetchPage(1, pageSize)
                            }
                        }
                    }
                }
            }
        }
    }
}
