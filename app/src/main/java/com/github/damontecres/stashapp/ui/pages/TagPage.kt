package com.github.damontecres.stashapp.ui.pages

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.ItemClicker
import com.github.damontecres.stashapp.di.services.ServerLogger
import com.github.damontecres.stashapp.proto.TabType
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.BasicItemInfo
import com.github.damontecres.stashapp.ui.components.ErrorMessage
import com.github.damontecres.stashapp.ui.components.ItemDetails
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.ItemsRow
import com.github.damontecres.stashapp.ui.components.LoadingPage
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.StashGridTab
import com.github.damontecres.stashapp.ui.components.TabPage
import com.github.damontecres.stashapp.ui.components.TabProvider
import com.github.damontecres.stashapp.ui.components.TableRow
import com.github.damontecres.stashapp.ui.components.tabFindFilter
import com.github.damontecres.stashapp.ui.filterArgsSaver
import com.github.damontecres.stashapp.ui.util.DataLoadingState
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.util.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.parameter.parametersOf

private const val TAG = "TagPage"

@KoinViewModel
class TagDetailsViewModel(
    private val context: Application,
    private val serverRepository: ServerRepository,
    private val serverLogger: ServerLogger,
    private val queryEngine: com.github.damontecres.stashapp.di.server.QueryEngine,
    val mutationEngine: com.github.damontecres.stashapp.di.server.MutationEngine,
    val itemClicker: ItemClicker,
    val navigationManager: com.github.damontecres.stashapp.di.services.NavigationManager,
    @InjectedParam private val id: String,
) : ViewModel() {
    val currentServer get() = serverRepository.currentServer

    private val _state = MutableStateFlow(TagState())
    val state: StateFlow<TagState> = _state

    init {
        viewModelScope.launchIO {
            _state.update { it.copy(tag = DataLoadingState.Loading) }
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
                val tag = queryEngine.getTag(id)
                if (tag != null) {
                    val childTags =
                        queryEngine.findTags(tagFilter = TagFilterType(parents = tagsFunc(false)))
                    val parentTags =
                        queryEngine.findTags(tagFilter = TagFilterType(children = tagsFunc(false)))
                    _state.update {
                        it.copy(
                            tag = DataLoadingState.Success(tag),
                            childTags = childTags,
                            parentTags = parentTags,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            tag = DataLoadingState.Error("Not found: $id"),
                        )
                    }
                }
            } catch (ex: QueryEngine.QueryException) {
                Log.e(TAG, "No tag found with ID $id", ex)
                Toast.makeText(context, "No tag found with ID $id", Toast.LENGTH_LONG).show()
            }
        }
    }
}

data class TagState(
    val tag: DataLoadingState<TagData> = DataLoadingState.Pending,
    val parentTags: List<TagData> = emptyList(),
    val childTags: List<TagData> = emptyList(),
)

@Composable
fun TagPage(
    id: String,
    includeSubTags: Boolean,
    longClicker: LongClicker<Any>,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
    onUpdateTitle: ((AnnotatedString) -> Unit)? = null,
    viewModel: TagDetailsViewModel =
        koinViewModel {
            parametersOf(id)
        },
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val currentServer by viewModel.currentServer.collectAsState()
    val serverPreferences = currentServer.serverPreferences

    val tagsFunc = { includeSubTags: Boolean ->
        Optional.present(
            HierarchicalMultiCriterionInput(
                value = Optional.present(listOf(id)),
                modifier = CriterionModifier.INCLUDES_ALL,
                depth = Optional.present(if (includeSubTags) -1 else 0),
            ),
        )
    }

    when (val st = state.tag) {
        is DataLoadingState.Error -> {
            ErrorMessage(null, st.exception)
        }

        DataLoadingState.Loading,
        DataLoadingState.Pending,
        -> {
            LoadingPage(modifier)
        }

        is DataLoadingState.Success<TagData> -> {
            val tag = st.data
            var favorite by remember { mutableStateOf(tag.favorite) }
            val scope = rememberCoroutineScope()
            val subToggleLabel = stringResource(R.string.stashapp_include_sub_tag_content)
            val subToggleEnabled = tag.child_count > 0

            val uiTabs =
                getUiTabs(uiConfig.preferences.interfacePreferences.tabPreferences, DataType.TAG)

            val detailsTab =
                remember {
                    TabProvider(
                        context.getString(R.string.stashapp_details),
                        TabType.DETAILS,
                    ) {
                        TagDetails(
                            modifier = Modifier.fillMaxSize(),
                            uiConfig = uiConfig,
                            tag = tag,
                            parentTags = state.parentTags,
                            childTags = state.childTags,
                            favorite = favorite,
                            favoriteClick = {
                                scope.launch(
                                    LoggingCoroutineExceptionHandler(
                                        currentServer,
                                        scope,
                                    ),
                                ) {
                                    val newTag =
                                        viewModel.mutationEngine.setTagFavorite(
                                            tagId = tag.id,
                                            favorite = !favorite,
                                        )
                                    if (newTag != null) {
                                        favorite = newTag.favorite
                                        if (newTag.favorite) {
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Tag favorited!",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        }
                                    }
                                }
                            },
                            itemOnClick = viewModel.itemClicker,
                            longClicker = longClicker,
                        )
                    }
                }

            // Scenes
            var scenesSubTags by rememberSaveable { mutableStateOf(false) }
            var scenesFilter by rememberSaveable(scenesSubTags, saver = filterArgsSaver) {
                mutableStateOf(
                    FilterArgs(
                        DataType.SCENE,
                        findFilter =
                            tabFindFilter(
                                currentServer.serverPreferences,
                                PageFilterKey.TAG_SCENES,
                            ),
                        objectFilter = SceneFilterType(tags = tagsFunc(scenesSubTags)),
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
                        findFilter = tabFindFilter(serverPreferences, PageFilterKey.TAG_GALLERIES),
                        objectFilter = GalleryFilterType(tags = tagsFunc(galleriesSubTags)),
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
                        findFilter = tabFindFilter(serverPreferences, PageFilterKey.TAG_IMAGES),
                        objectFilter = ImageFilterType(tags = tagsFunc(imagesSubTags)),
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
                        findFilter = tabFindFilter(serverPreferences, PageFilterKey.TAG_MARKERS),
                        objectFilter = SceneMarkerFilterType(tags = tagsFunc(markersSubTags)),
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
                        findFilter = tabFindFilter(serverPreferences, PageFilterKey.TAG_PERFORMERS),
                        objectFilter = PerformerFilterType(tags = tagsFunc(performersSubTags)),
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

            // studios
            var studiosSubTags by rememberSaveable { mutableStateOf(false) }
            var studiosFilter by rememberSaveable(studiosSubTags, saver = filterArgsSaver) {
                mutableStateOf(
                    FilterArgs(
                        DataType.STUDIO,
                        findFilter = null,
                        objectFilter = StudioFilterType(tags = tagsFunc(studiosSubTags)),
                    ),
                )
            }
            val studiosTab =
                remember(studiosSubTags, studiosFilter) {
                    TabProvider(
                        context.getString(R.string.stashapp_studios),
                        TabType.STUDIOS,
                    ) { positionCallback ->
                        StashGridTab(
                            name = context.getString(R.string.stashapp_studios),
                            initialFilter = studiosFilter,
                            itemOnClick = viewModel.itemClicker,
                            longClicker = longClicker,
                            modifier = Modifier,
                            positionCallback = positionCallback,
                            subToggleLabel = subToggleLabel,
                            onSubToggleCheck = { studiosSubTags = it },
                            subToggleChecked = studiosSubTags,
                            subToggleEnabled = subToggleEnabled,
                            composeUiConfig = uiConfig,
                            onFilterChange = { studiosFilter = it },
                        )
                    }
                }

            val tabs =
                listOf(
                    detailsTab,
                    scenesTab,
                    galleriesTab,
                    imagesTab,
                    markersTab,
                    performersTab,
                    studiosTab,
                ).filter { it.type in uiTabs }
            val title = AnnotatedString(tag.name)
            LaunchedEffect(title) { onUpdateTitle?.invoke(title) }
            TabPage(
                title,
                uiConfig.preferences.interfacePreferences.rememberSelectedTab,
                tabs,
                DataType.TAG,
                modifier,
                onUpdateTitle == null,
            )
        }
    }
}

@Composable
fun TagDetails(
    uiConfig: ComposeUiConfig,
    tag: TagData,
    parentTags: List<TagData>,
    childTags: List<TagData>,
    favorite: Boolean,
    favoriteClick: () -> Unit,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val rows =
        remember {
            buildList {
                add(TableRow.from(context.getString(R.string.stashapp_sort_name), tag.sort_name))
                add(
                    TableRow.from(
                        context.getString(R.string.stashapp_description),
                        tag.description,
                    ),
                )
                if (tag.aliases.isNotEmpty()) {
                    add(
                        TableRow.from(
                            context.getString(R.string.stashapp_aliases),
                            tag.aliases.joinToString(", "),
                        ),
                    )
                }
            }.filterNotNull()
        }
    ItemDetails(
        modifier = modifier,
        uiConfig = uiConfig,
        imageUrl = tag.image_path,
        tableRows = rows,
        favorite = favorite,
        favoriteClick = favoriteClick,
        basicItemInfo = BasicItemInfo(tag.id, tag.created_at, tag.updated_at),
        itemOnClick = itemOnClick,
        longClicker = longClicker,
        onEdit = {},
        editableTypes = setOf(),
    ) {
        if (parentTags.isNotEmpty()) {
            item {
                ItemsRow(
                    title = stringResource(R.string.stashapp_parent_tags),
                    items = parentTags,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        if (childTags.isNotEmpty()) {
            item {
                ItemsRow(
                    title = stringResource(R.string.stashapp_sub_tags),
                    items = childTags,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
