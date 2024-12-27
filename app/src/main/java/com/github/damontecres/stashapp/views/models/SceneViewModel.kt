package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

class SceneViewModel : ViewModel() {
    private val _scene = EqualityMutableLiveData<FullSceneData?>()
    val scene: LiveData<FullSceneData?> = _scene

    val currentPosition = MutableLiveData<Long>()

    private val _performers = MutableLiveData<List<PerformerData>>()
    val performers: LiveData<List<PerformerData>> = _performers

    private val _galleries = MutableLiveData<List<GalleryData>>()
    val galleries: LiveData<List<GalleryData>> = _galleries

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
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            val queryEngine = QueryEngine(StashServer.requireCurrentServer())
            val newScene = queryEngine.getScene(id)
            _scene.value = newScene
            if (newScene != null) {
                val currPos = currentPosition.value
                if (currPos == null || currPos <= 0L) {
                    currentPosition.value = (newScene.resume_time ?: 0.0).times(1000L).toLong()
                }

                if (fetchAll) {
                    val performerIds = newScene.performers.map { it.id }
                    if (performerIds.isNotEmpty()) {
                        viewModelScope.launch(StashCoroutineExceptionHandler()) {
                            _performers.value =
                                queryEngine.findPerformers(performerIds = performerIds)
                        }
                    }
                    viewModelScope.launch(StashCoroutineExceptionHandler()) {
                        _galleries.value =
                            queryEngine.getGalleries(newScene.galleries.map { it.id })
                    }

                    // Suggestions
                    viewModelScope.launch(StashCoroutineExceptionHandler()) {
                        val idFilter =
                            Optional.present(
                                IntCriterionInput(
                                    value = newScene.id.toInt(),
                                    modifier = CriterionModifier.NOT_EQUALS,
                                ),
                            )
                        val filters =
                            buildList {
                                if (newScene.tags.isNotEmpty()) {
                                    add(
                                        SceneFilterType(
                                            id = idFilter,
                                            tags =
                                                Optional.present(
                                                    HierarchicalMultiCriterionInput(
                                                        value = Optional.present(newScene.tags.map { it.tagData.id }),
                                                        modifier = CriterionModifier.INCLUDES,
                                                    ),
                                                ),
                                        ),
                                    )
                                }
                                if (newScene.performers.isNotEmpty()) {
                                    add(
                                        SceneFilterType(
                                            id = idFilter,
                                            performers =
                                                Optional.presentIfNotNull(
                                                    MultiCriterionInput(
                                                        value = Optional.present(newScene.performers.map { it.id }),
                                                        modifier = CriterionModifier.INCLUDES,
                                                    ),
                                                ),
                                        ),
                                    )
                                }
                                if (newScene.studio?.studioData != null) {
                                    add(
                                        SceneFilterType(
                                            id = idFilter,
                                            studios =
                                                Optional.present(
                                                    HierarchicalMultiCriterionInput(
                                                        value = Optional.present(listOf(newScene.studio.studioData.id)),
                                                        modifier = CriterionModifier.EQUALS,
                                                    ),
                                                ),
                                        ),
                                    )
                                }
                                if (newScene.groups.isNotEmpty()) {
                                    add(
                                        SceneFilterType(
                                            id = idFilter,
                                            groups =
                                                Optional.present(
                                                    HierarchicalMultiCriterionInput(
                                                        value = Optional.present(newScene.groups.map { it.group.groupData.id }),
                                                        modifier = CriterionModifier.INCLUDES,
                                                    ),
                                                ),
                                        ),
                                    )
                                }
                            }.toMutableList()
                        if (filters.isNotEmpty()) {
                            for (i in (1..<filters.size).reversed()) {
                                filters[i - 1] =
                                    filters[i - 1].copy(OR = Optional.present(filters[i]))
                            }
                            val objectFilter = filters.first()
                            val filterArgs =
                                FilterArgs(
                                    DataType.SCENE,
                                    objectFilter = objectFilter,
                                    findFilter = StashFindFilter(sortAndDirection = SortAndDirection.random()),
                                ).withResolvedRandom()
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
