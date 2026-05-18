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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.ItemClicker
import com.github.damontecres.stashapp.di.services.ServerLogger
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.proto.TabType
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
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
import com.github.damontecres.stashapp.ui.components.TabPage
import com.github.damontecres.stashapp.ui.components.TabProvider
import com.github.damontecres.stashapp.ui.components.TableRow
import com.github.damontecres.stashapp.ui.components.createTabFunc
import com.github.damontecres.stashapp.ui.components.scene.AddRemove
import com.github.damontecres.stashapp.ui.components.tabFindFilter
import com.github.damontecres.stashapp.ui.showAddPerf
import com.github.damontecres.stashapp.ui.showAddTag
import com.github.damontecres.stashapp.ui.showSetStudio
import com.github.damontecres.stashapp.ui.util.DataLoadingState
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.name
import com.github.damontecres.stashapp.util.showSetRatingToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.parameter.parametersOf

private const val TAG = "GalleryPage"

@KoinViewModel
class GalleryDetailsViewModel(
    private val context: Application,
    private val serverRepository: ServerRepository,
    private val serverLogger: ServerLogger,
    val queryEngine: com.github.damontecres.stashapp.di.server.QueryEngine,
    val mutationEngine: com.github.damontecres.stashapp.di.server.MutationEngine,
    val itemClicker: ItemClicker,
    val navigationManager: com.github.damontecres.stashapp.di.services.NavigationManager,
    @InjectedParam private val id: String,
) : ViewModel() {
    val currentServer get() = serverRepository.currentServer

    private val _state = MutableStateFlow(GalleryState())
    val state: StateFlow<GalleryState> = _state

    init {
        viewModelScope.launchIO {
            _state.update { it.copy(gallery = DataLoadingState.Loading) }
            try {
                val gallery = queryEngine.getGallery(id)
                if (gallery != null) {
                    val tags = queryEngine.getTags(gallery.tags.map { it.slimTagData.id })
                    _state.update {
                        it.copy(
                            gallery = DataLoadingState.Success(gallery),
                            tags = tags,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            gallery = DataLoadingState.Error("Not found: $id"),
                        )
                    }
                }
            } catch (ex: QueryEngine.QueryException) {
                Log.e(TAG, "No gallery found with ID $id", ex)
                Toast.makeText(context, "No tag found with ID $id", Toast.LENGTH_LONG).show()
            }
        }
    }
}

data class GalleryState(
    val gallery: DataLoadingState<GalleryData> = DataLoadingState.Pending,
    val tags: List<TagData> = emptyList(),
)

@Composable
fun GalleryPage(
    id: String,
    longClicker: LongClicker<Any>,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
    onUpdateTitle: ((AnnotatedString) -> Unit)? = null,
    viewModel: GalleryDetailsViewModel =
        koinViewModel {
            parametersOf(id)
        },
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val currentServer by viewModel.currentServer.collectAsState()
    val serverPreferences = currentServer.serverPreferences

    val scope = rememberCoroutineScope()

    val createTab =
        createTabFunc(
            viewModel.itemClicker,
            longClicker,
            uiConfig,
        )

    when (val st = state.gallery) {
        is DataLoadingState.Error -> {
            ErrorMessage(null, st.exception)
        }

        DataLoadingState.Loading,
        DataLoadingState.Pending,
        -> {
            LoadingPage(modifier)
        }

        is DataLoadingState.Success<GalleryData> -> {
            val gallery = st.data
            var studio by remember { mutableStateOf<GalleryData.Studio?>(gallery.studio) }
            var tags by remember { mutableStateOf<List<TagData>>(state.tags) }
            // Remember separately so we don't have refresh the whole page
            var rating100 by remember { mutableIntStateOf(gallery.rating100 ?: 0) }

            val galleries =
                remember(gallery) {
                    Optional.present(
                        MultiCriterionInput(
                            value = Optional.present(listOf(gallery.id)),
                            modifier = CriterionModifier.INCLUDES_ALL,
                        ),
                    )
                }
            val uiTabs =
                getUiTabs(
                    uiConfig.preferences.interfacePreferences.tabPreferences,
                    DataType.GALLERY,
                )
            val tabs =
                listOf(
                    TabProvider(stringResource(R.string.stashapp_details), TabType.DETAILS) {
                        GalleryDetails(
                            modifier = Modifier.fillMaxSize(),
                            uiConfig = uiConfig,
                            gallery = gallery,
                            studio = studio,
                            tags = tags,
                            rating100 = rating100,
                            rating100Click = { newRating100 ->
                                scope.launch(
                                    LoggingCoroutineExceptionHandler(
                                        currentServer,
                                        scope,
                                    ),
                                ) {
                                    val newGallery =
                                        viewModel.mutationEngine.updateGallery(
                                            galleryId = gallery.id,
                                            rating100 = newRating100,
                                        )
                                    if (newGallery != null) {
                                        rating100 = newGallery.rating100 ?: 0
                                        showSetRatingToast(
                                            context,
                                            newGallery.rating100 ?: 0,
                                            serverPreferences.ratingsAsStars,
                                        )
                                    }
                                }
                            },
                            itemOnClick = viewModel.itemClicker,
                            longClicker = longClicker,
                            onEdit = { edit ->
                                scope.launch(StashCoroutineExceptionHandler()) {
                                    if (edit.dataType == DataType.TAG) {
                                        val ids = tags.map { it.id }.toMutableList()
                                        edit.action.exec(edit.id, ids)
                                        val newGallery =
                                            viewModel.mutationEngine.updateGallery(
                                                galleryId = gallery.id,
                                                tagIds = ids,
                                            )
                                        val newTagIds =
                                            newGallery?.let { it.tags.map { it.slimTagData.id } }
                                        if (newTagIds != null) {
                                            tags = viewModel.queryEngine.getTags(newTagIds)
                                            if (edit.action == AddRemove.ADD) {
                                                tags
                                                    .firstOrNull { it.id == edit.id }
                                                    ?.let { showAddTag(it) }
                                            }
                                        }
                                    } else if (edit.dataType == DataType.PERFORMER) {
                                        val ids =
                                            gallery.performers
                                                .map { it.slimPerformerData.id }
                                                .toMutableList()
                                        edit.action.exec(edit.id, ids)
                                        val newGallery =
                                            viewModel.mutationEngine.updateGallery(
                                                galleryId = gallery.id,
                                                performerIds = ids,
                                            )
                                        if (newGallery != null) {
                                            val perf = viewModel.queryEngine.getPerformer(edit.id)
                                            if (edit.action == AddRemove.ADD && perf != null) {
                                                showAddPerf(perf)
                                            }
                                        }
                                    } else if (edit.dataType == DataType.STUDIO) {
                                        val newGallery =
                                            viewModel.mutationEngine.updateGallery(
                                                galleryId = gallery.id,
                                                studioId = edit.id,
                                            )
                                        if (newGallery != null) {
                                            studio = newGallery.studio
                                            if (edit.action == AddRemove.ADD && newGallery.studio != null) {
                                                showSetStudio(newGallery.studio.name)
                                            }
                                        }
                                    }
                                }
                            },
                        )
                    },
                    createTab(
                        FilterArgs(
                            dataType = DataType.IMAGE,
                            findFilter =
                                tabFindFilter(
                                    serverPreferences,
                                    PageFilterKey.GALLERY_IMAGES,
                                ),
                            objectFilter = ImageFilterType(galleries = galleries),
                        ),
                    ),
                    createTab(
                        FilterArgs(
                            dataType = DataType.SCENE,
                            objectFilter = SceneFilterType(galleries = galleries),
                        ),
                    ),
                    createTab(
                        FilterArgs(
                            dataType = DataType.PERFORMER,
                            override = DataSupplierOverride.GalleryPerformer(gallery.id),
                        ),
                    ),
                ).filter { it.type in uiTabs }
            val title = AnnotatedString(gallery.name ?: "")
            LaunchedEffect(title) { onUpdateTitle?.invoke(title) }
            TabPage(
                title,
                uiConfig.preferences.interfacePreferences.rememberSelectedTab,
                tabs,
                DataType.GALLERY,
                modifier,
                onUpdateTitle == null,
            )
        }
    }
}

@Composable
fun GalleryDetails(
    gallery: GalleryData,
    tags: List<TagData>,
    studio: GalleryData.Studio?,
    uiConfig: ComposeUiConfig,
    rating100: Int,
    rating100Click: (rating100: Int) -> Unit,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    onEdit: (EditItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationManager = LocalGlobalContext.current.navigationManager
    val context = LocalContext.current
    val rows =
        buildList {
            add(TableRow.from(R.string.stashapp_date, gallery.date))
            add(
                TableRow.from(R.string.stashapp_studio, studio?.name) {
                    navigationManager.navigate(
                        Destination.Item(
                            DataType.STUDIO,
                            studio!!.id,
                        ),
                    )
                },
            )
            add(TableRow.from(R.string.stashapp_scene_code, gallery.code))
            add(
                TableRow.from(R.string.stashapp_photographer, gallery.photographer) {
                    navigationManager.navigate(
                        Destination.Filter(
                            filterArgs =
                                FilterArgs(
                                    dataType = DataType.GALLERY,
                                    name = context.getString(R.string.stashapp_photographer) + ": " + gallery.photographer,
                                    objectFilter =
                                        GalleryFilterType(
                                            photographer = stringCriterion(gallery.photographer!!),
                                        ),
                                ),
                        ),
                    )
                },
            )
            add(TableRow.from(R.string.stashapp_description, gallery.details))
        }.filterNotNull()
    ItemDetails(
        modifier = modifier,
        uiConfig = uiConfig,
        imageUrl = gallery.paths.cover,
        tableRows = rows,
        favorite = null,
        favoriteClick = null,
        rating100 = rating100,
        rating100Click = rating100Click,
        tags = tags,
        basicItemInfo = BasicItemInfo(gallery.id, gallery.created_at, gallery.updated_at),
        itemOnClick = itemOnClick,
        longClicker = longClicker,
        onEdit = onEdit,
        editableTypes = setOf(DataType.TAG, DataType.PERFORMER, DataType.STUDIO),
    )
}
