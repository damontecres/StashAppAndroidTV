package com.github.damontecres.stashapp.ui.pages

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.ItemClicker
import com.github.damontecres.stashapp.di.services.ServerLogger
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.proto.TabType
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.components.BasicItemInfo
import com.github.damontecres.stashapp.ui.components.EditItem
import com.github.damontecres.stashapp.ui.components.ErrorMessage
import com.github.damontecres.stashapp.ui.components.ItemDetails
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LoadingPage
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.StashGridTab
import com.github.damontecres.stashapp.ui.components.TabPage
import com.github.damontecres.stashapp.ui.components.TabProvider
import com.github.damontecres.stashapp.ui.components.TableRow
import com.github.damontecres.stashapp.ui.components.scene.AddRemove
import com.github.damontecres.stashapp.ui.components.tabFindFilter
import com.github.damontecres.stashapp.ui.filterArgsSaver
import com.github.damontecres.stashapp.ui.showAddTag
import com.github.damontecres.stashapp.ui.showSetStudio
import com.github.damontecres.stashapp.ui.util.DataLoadingState
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.showSetRatingToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.parameter.parametersOf

private const val TAG = "StudioPage"

@KoinViewModel
class StudioDetailsViewModel(
    private val context: Application,
    private val serverRepository: ServerRepository,
    private val serverLogger: ServerLogger,
    val queryEngine: QueryEngine,
    val mutationEngine: com.github.damontecres.stashapp.di.server.MutationEngine,
    val itemClicker: ItemClicker,
    val navigationManager: com.github.damontecres.stashapp.di.services.NavigationManager,
    @InjectedParam private val id: String,
) : ViewModel() {
    val currentServer get() = serverRepository.currentServer

    private val _state = MutableStateFlow(StudioState())
    val state: StateFlow<StudioState> = _state

    init {
        viewModelScope.launchIO {
            _state.update { it.copy(studio = DataLoadingState.Loading) }
            val tagsFunc = { includeSubTags: Boolean ->
                Optional.present(
                    HierarchicalMultiCriterionInput(
                        value = Optional.present(listOf(id)),
                        modifier = CriterionModifier.INCLUDES_ALL,
                        depth = Optional.present(if (includeSubTags) -1 else 0),
                    ),
                )
            }
            try {
                val studio = queryEngine.getStudio(id)
                if (studio != null) {
                    val tags = queryEngine.getTags(studio.tags.map { it.slimTagData.id })
                    _state.update {
                        it.copy(
                            studio = DataLoadingState.Success(studio),
                            tags = tags,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            studio = DataLoadingState.Error("Not found: $id"),
                        )
                    }
                }
            } catch (ex: QueryEngine.QueryException) {
                Log.e(TAG, "No studio found with ID $id", ex)
                Toast.makeText(context, "No tag found with ID $id", Toast.LENGTH_LONG).show()
            }
        }
    }
}

data class StudioState(
    val studio: DataLoadingState<StudioData> = DataLoadingState.Pending,
    val tags: List<TagData> = emptyList(),
    val parentStudio: StudioData.Parent_studio? = null,
)

@Composable
fun StudioPage(
    id: String,
    includeSubStudios: Boolean,
    longClicker: LongClicker<Any>,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
    onUpdateTitle: ((AnnotatedString) -> Unit)? = null,
    viewModel: StudioDetailsViewModel =
        koinViewModel {
            parametersOf(id)
        },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val currentServer by viewModel.currentServer.collectAsState()

    when (val st = state.studio) {
        is DataLoadingState.Error -> {
            ErrorMessage(null, st.exception)
        }

        DataLoadingState.Loading,
        DataLoadingState.Pending,
        -> {
            LoadingPage(modifier)
        }

        is DataLoadingState.Success<StudioData> -> {
            val serverPreferences = currentServer.serverPreferences
            val studio = st.data
            // Remember separately so we don't have refresh the whole page
            var favorite by remember { mutableStateOf(studio.favorite) }
            var rating100 by remember { mutableIntStateOf(studio.rating100 ?: 0) }
            var tags by remember { mutableStateOf<List<TagData>>(state.tags) }
            var parentStudio by remember { mutableStateOf<StudioData.Parent_studio?>(studio.parent_studio) }
            val subToggleLabel = stringResource(R.string.stashapp_include_sub_studio_content)
            val subToggleEnabled =
                remember(studio.child_studios) { studio.child_studios.isNotEmpty() }

            val studiosFunc = { includeSubStudios: Boolean ->
                Optional.present(
                    HierarchicalMultiCriterionInput(
                        value = Optional.present(listOf(studio.id)),
                        modifier = CriterionModifier.INCLUDES,
                        depth = Optional.present(if (includeSubStudios) -1 else 0),
                    ),
                )
            }

            val detailsTab =
                TabProvider(stringResource(R.string.stashapp_details), TabType.DETAILS) {
                    StudioDetails(
                        studio = studio,
                        parentStudio = parentStudio,
                        uiConfig = uiConfig,
                        favorite = favorite,
                        favoriteClick = {
                            scope.launch(LoggingCoroutineExceptionHandler(currentServer, scope)) {
                                val newStudio =
                                    viewModel.mutationEngine.updateStudio(
                                        studioId = studio.id,
                                        favorite = !favorite,
                                    )
                                if (newStudio != null) {
                                    favorite = newStudio.favorite
                                    if (newStudio.favorite) {
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(R.string.studio_favorited),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }
                                }
                            }
                        },
                        rating100 = rating100,
                        tags = tags,
                        rating100Click = { newRating100 ->
                            scope.launch(LoggingCoroutineExceptionHandler(currentServer, scope)) {
                                val newStudio =
                                    viewModel.mutationEngine.updateStudio(
                                        studioId = studio.id,
                                        rating100 = newRating100,
                                    )
                                if (newStudio != null) {
                                    rating100 = newStudio.rating100 ?: 0
                                    showSetRatingToast(
                                        context,
                                        newStudio.rating100 ?: 0,
                                        serverPreferences.ratingsAsStars,
                                    )
                                }
                            }
                        },
                        onEdit = { edit ->
                            scope.launch(StashCoroutineExceptionHandler()) {
                                if (edit.dataType == DataType.TAG) {
                                    val ids = tags.map { it.id }.toMutableList()
                                    edit.action.exec(edit.id, ids)
                                    val newItem =
                                        viewModel.mutationEngine.updateStudio(
                                            studioId = studio.id,
                                            tagIds = ids,
                                        )
                                    val newTagIds =
                                        newItem?.let { it.tags.map { it.slimTagData.id } }
                                    if (newTagIds != null) {
                                        tags = viewModel.queryEngine.getTags(newTagIds)
                                        if (edit.action == AddRemove.ADD) {
                                            tags
                                                .firstOrNull { it.id == edit.id }
                                                ?.let { showAddTag(it) }
                                        }
                                    }
                                } else if (edit.dataType == DataType.STUDIO) {
                                    // TODO the wording in the dialogs & toasts don't specify its the parent studio
                                    val newItem =
                                        viewModel.mutationEngine.updateStudio(
                                            studioId = studio.id,
                                            parentStudioId = edit.id,
                                        )
                                    if (newItem?.parent_studio != null) {
                                        parentStudio = newItem.parent_studio
                                        showSetStudio(newItem.parent_studio.name)
                                    }
                                }
                            }
                        },
                        itemOnClick = viewModel.itemClicker,
                        longClicker = longClicker,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

            // Scenes
            var scenesSubTags by rememberSaveable { mutableStateOf(false) }
            var scenesFilter by rememberSaveable(scenesSubTags, saver = filterArgsSaver) {
                mutableStateOf(
                    FilterArgs(
                        DataType.SCENE,
                        findFilter = tabFindFilter(serverPreferences, PageFilterKey.STUDIO_SCENES),
                        objectFilter = SceneFilterType(studios = studiosFunc(scenesSubTags)),
                    ),
                )
            }
            val scenesTab =
                remember(scenesFilter, scenesSubTags) {
                    TabProvider(
                        context.getString(R.string.stashapp_scenes),
                        TabType.SCENES,
                    ) { positionCallback ->
                        StashGridTab(
                            name = context.getString(R.string.stashapp_scenes),
                            initialFilter = scenesFilter,
                            itemOnClick = viewModel.itemClicker,
                            longClicker = longClicker,
                            modifier = Modifier,
                            positionCallback = positionCallback,
                            subToggleLabel = subToggleLabel,
                            onSubToggleCheck = { scenesSubTags = it },
                            subToggleChecked = scenesSubTags,
                            subToggleEnabled = subToggleEnabled,
                            composeUiConfig = uiConfig,
                            onFilterChange = { scenesFilter = it },
                        )
                    }
                }

            // Galleries
            var galleriesSubTags by rememberSaveable { mutableStateOf(false) }
            var galleriesFilter by rememberSaveable(galleriesSubTags, saver = filterArgsSaver) {
                mutableStateOf(
                    FilterArgs(
                        DataType.GALLERY,
                        findFilter =
                            tabFindFilter(
                                serverPreferences,
                                PageFilterKey.STUDIO_GALLERIES,
                            ),
                        objectFilter = GalleryFilterType(studios = studiosFunc(galleriesSubTags)),
                    ),
                )
            }
            val galleriesTab =
                remember(galleriesSubTags, galleriesFilter) {
                    TabProvider(
                        context.getString(R.string.stashapp_galleries),
                        TabType.GALLERIES,
                    ) { positionCallback ->
                        StashGridTab(
                            name = context.getString(R.string.stashapp_galleries),
                            initialFilter = galleriesFilter,
                            itemOnClick = viewModel.itemClicker,
                            longClicker = longClicker,
                            modifier = Modifier,
                            positionCallback = positionCallback,
                            subToggleLabel = subToggleLabel,
                            onSubToggleCheck = { galleriesSubTags = it },
                            subToggleChecked = galleriesSubTags,
                            subToggleEnabled = subToggleEnabled,
                            composeUiConfig = uiConfig,
                            onFilterChange = { galleriesFilter = it },
                        )
                    }
                }
            // images
            var imagesSubTags by rememberSaveable { mutableStateOf(false) }
            var imagesFilter by rememberSaveable(imagesSubTags, saver = filterArgsSaver) {
                mutableStateOf(
                    FilterArgs(
                        DataType.IMAGE,
                        findFilter = tabFindFilter(serverPreferences, PageFilterKey.STUDIO_IMAGES),
                        objectFilter = ImageFilterType(studios = studiosFunc(imagesSubTags)),
                    ),
                )
            }
            val imagesTab =
                remember(imagesSubTags, imagesFilter) {
                    TabProvider(
                        context.getString(R.string.stashapp_images),
                        TabType.IMAGES,
                    ) { positionCallback ->
                        StashGridTab(
                            name = context.getString(R.string.stashapp_images),
                            initialFilter = imagesFilter,
                            itemOnClick = viewModel.itemClicker,
                            longClicker = longClicker,
                            modifier = Modifier,
                            positionCallback = positionCallback,
                            subToggleLabel = subToggleLabel,
                            onSubToggleCheck = { imagesSubTags = it },
                            subToggleChecked = imagesSubTags,
                            subToggleEnabled = subToggleEnabled,
                            composeUiConfig = uiConfig,
                            onFilterChange = { imagesFilter = it },
                        )
                    }
                }
            // markers
            var markersSubTags by rememberSaveable { mutableStateOf(false) }
            var markersFilter by rememberSaveable(markersSubTags, saver = filterArgsSaver) {
                mutableStateOf(
                    FilterArgs(
                        DataType.MARKER,
                        findFilter = null,
                        objectFilter =
                            SceneMarkerFilterType(
                                scene_filter =
                                    Optional.present(
                                        SceneFilterType(studios = studiosFunc(markersSubTags)),
                                    ),
                            ),
                    ),
                )
            }
            val markersTab =
                remember(markersSubTags, markersFilter) {
                    TabProvider(
                        context.getString(R.string.stashapp_markers),
                        TabType.MARKERS,
                    ) { positionCallback ->
                        StashGridTab(
                            name = context.getString(R.string.stashapp_markers),
                            initialFilter = markersFilter,
                            itemOnClick = viewModel.itemClicker,
                            longClicker = longClicker,
                            modifier = Modifier,
                            positionCallback = positionCallback,
                            subToggleLabel = subToggleLabel,
                            onSubToggleCheck = { markersSubTags = it },
                            subToggleChecked = markersSubTags,
                            subToggleEnabled = subToggleEnabled,
                            composeUiConfig = uiConfig,
                            onFilterChange = { markersFilter = it },
                        )
                    }
                }

            // performers
            var performersSubTags by rememberSaveable { mutableStateOf(false) }
            var performersFilter by rememberSaveable(performersSubTags, saver = filterArgsSaver) {
                mutableStateOf(
                    FilterArgs(
                        DataType.PERFORMER,
                        findFilter =
                            tabFindFilter(
                                serverPreferences,
                                PageFilterKey.STUDIO_PERFORMERS,
                            ),
                        objectFilter = PerformerFilterType(studios = studiosFunc(performersSubTags)),
                    ),
                )
            }
            val performersTab =
                remember(performersSubTags, performersFilter) {
                    TabProvider(
                        context.getString(R.string.stashapp_performers),
                        TabType.PERFORMERS,
                    ) { positionCallback ->
                        StashGridTab(
                            name = context.getString(R.string.stashapp_performers),
                            initialFilter = performersFilter,
                            itemOnClick = viewModel.itemClicker,
                            longClicker = longClicker,
                            modifier = Modifier,
                            positionCallback = positionCallback,
                            subToggleLabel = subToggleLabel,
                            onSubToggleCheck = { performersSubTags = it },
                            subToggleChecked = performersSubTags,
                            subToggleEnabled = subToggleEnabled,
                            composeUiConfig = uiConfig,
                            onFilterChange = { performersFilter = it },
                        )
                    }
                }

            // groups
            var groupsSubTags by rememberSaveable { mutableStateOf(false) }
            var groupsFilter by rememberSaveable(groupsSubTags, saver = filterArgsSaver) {
                mutableStateOf(
                    FilterArgs(
                        DataType.GROUP,
                        findFilter = tabFindFilter(serverPreferences, PageFilterKey.STUDIO_GROUPS),
                        objectFilter = GroupFilterType(studios = studiosFunc(groupsSubTags)),
                    ),
                )
            }
            val groupsTab =
                remember(groupsSubTags, groupsFilter) {
                    TabProvider(
                        context.getString(R.string.stashapp_groups),
                        TabType.GROUPS,
                    ) { positionCallback ->
                        StashGridTab(
                            name = context.getString(R.string.stashapp_groups),
                            initialFilter = groupsFilter,
                            itemOnClick = viewModel.itemClicker,
                            longClicker = longClicker,
                            modifier = Modifier,
                            positionCallback = positionCallback,
                            subToggleLabel = subToggleLabel,
                            onSubToggleCheck = { groupsSubTags = it },
                            subToggleChecked = groupsSubTags,
                            subToggleEnabled = subToggleEnabled,
                            composeUiConfig = uiConfig,
                            onFilterChange = { groupsFilter = it },
                        )
                    }
                }

            var subStudioFilter by rememberSaveable(saver = filterArgsSaver) {
                mutableStateOf(
                    FilterArgs(
                        dataType = DataType.STUDIO,
                        findFilter =
                            tabFindFilter(
                                serverPreferences,
                                PageFilterKey.STUDIO_CHILDREN,
                            ),
                        objectFilter =
                            StudioFilterType(
                                parents =
                                    Optional.present(
                                        MultiCriterionInput(
                                            value = Optional.present(listOf(studio.id)),
                                            modifier = CriterionModifier.INCLUDES,
                                        ),
                                    ),
                            ),
                    ),
                )
            }
            val subStudiosTab =
                TabProvider(
                    stringResource(R.string.stashapp_subsidiary_studios),
                    TabType.SUBSIDIARY_STUDIOS,
                ) { positionCallback ->
                    StashGridTab(
                        name = stringResource(R.string.stashapp_subsidiary_studios),
                        initialFilter = subStudioFilter,
                        itemOnClick = viewModel.itemClicker,
                        longClicker = longClicker,
                        modifier = Modifier,
                        positionCallback = positionCallback,
                        composeUiConfig = uiConfig,
                        subToggleLabel = null, // TODO
                        onFilterChange = { subStudioFilter = it },
                    )
                }

            val uiTabs =
                getUiTabs(uiConfig.preferences.interfacePreferences.tabPreferences, DataType.STUDIO)
            val tabs =
                listOf(
                    detailsTab,
                    scenesTab,
                    galleriesTab,
                    imagesTab,
                    performersTab,
                    groupsTab,
                    markersTab,
                    subStudiosTab,
                ).filter { it.type in uiTabs }
            val title = AnnotatedString(studio.name)
            LaunchedEffect(title) { onUpdateTitle?.invoke(title) }
            TabPage(
                title,
                uiConfig.preferences.interfacePreferences.rememberSelectedTab,
                tabs,
                DataType.STUDIO,
                modifier,
                onUpdateTitle == null,
            )
        }
    }
}

@Composable
fun StudioDetails(
    studio: StudioData,
    parentStudio: StudioData.Parent_studio?,
    uiConfig: ComposeUiConfig,
    favorite: Boolean,
    favoriteClick: () -> Unit,
    rating100: Int,
    tags: List<TagData>,
    rating100Click: (rating100: Int) -> Unit,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    onEdit: (EditItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationManager = LocalGlobalContext.current.navigationManager
    val rows =
        buildList {
            add(TableRow.from(R.string.stashapp_description, studio.details))
            add(
                TableRow.from(R.string.stashapp_parent_studio, parentStudio?.name) {
                    navigationManager.navigate(
                        Destination.Item(
                            DataType.STUDIO,
                            parentStudio!!.id,
                        ),
                    )
                },
            )
            if (studio.aliases.isNotEmpty()) {
                add(
                    TableRow.from(
                        R.string.stashapp_aliases,
                        studio.aliases.joinToString(", "),
                    ),
                )
            }
        }.filterNotNull()
    ItemDetails(
        modifier = modifier,
        uiConfig = uiConfig,
        imageUrl = studio.image_path,
        tableRows = rows,
        favorite = favorite,
        favoriteClick = favoriteClick,
        rating100 = rating100,
        rating100Click = rating100Click,
        basicItemInfo = BasicItemInfo(studio.id, studio.created_at, studio.updated_at),
        itemOnClick = itemOnClick,
        longClicker = longClicker,
        tags = tags,
        onEdit = onEdit,
        editableTypes = setOf(DataType.TAG, DataType.STUDIO),
    )
}
