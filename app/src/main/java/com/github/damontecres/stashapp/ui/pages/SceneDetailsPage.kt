package com.github.damontecres.stashapp.ui.pages

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import coil3.compose.AsyncImage
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.ExtraImageData
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.GroupRelationshipData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MinimalSceneData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimPerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.SlimTagData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.playback.displayString
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.Material3MainTheme
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.DotSeparatedRow
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.StarRating
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.asMarkerData
import com.github.damontecres.stashapp.util.bitRateString
import com.github.damontecres.stashapp.util.fakeMarker
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.listOfNotNullOrBlank
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.resume_position
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.util.toLongMilliseconds
import com.github.damontecres.stashapp.util.toSeconds
import com.github.damontecres.stashapp.views.durationToString
import kotlinx.coroutines.launch

class SceneDetailsViewModel(
    server: StashServer,
    val sceneId: String,
) : ViewModel() {
    private val queryEngine = QueryEngine(server)
    private val mutationEngine = MutationEngine(server)

    private var scene: FullSceneData? = null

    val loadingState = MutableLiveData<SceneLoadingState>(SceneLoadingState.Loading)
    val tags = MutableLiveData<List<TagData>>(listOf())
    val performers = MutableLiveData<List<PerformerData>>(listOf())
    val galleries = MutableLiveData<List<GalleryData>>(listOf())
    val groups = MutableLiveData<List<GroupData>>(listOf())
    val markers = MutableLiveData<List<MarkerData>>(listOf())

    val rating100 = MutableLiveData(0)
    val oCount = MutableLiveData(0)

    fun init(): SceneDetailsViewModel {
        viewModelScope.launch {
            try {
                val scene = queryEngine.getScene(sceneId)
                if (scene != null) {
                    rating100.value = scene.rating100 ?: 0
                    oCount.value = scene.o_counter ?: 0
                    tags.value = scene.tags.map { it.tagData }
                    groups.value = scene.groups.map { it.group.groupData }
                    markers.value = scene.scene_markers.map { it.asMarkerData(scene) }
                    this@SceneDetailsViewModel.scene = scene

                    loadingState.value = SceneLoadingState.Success(scene)
                    if (scene.performers.isNotEmpty()) {
                        performers.value =
                            queryEngine.findPerformers(performerIds = scene.performers.map { it.id })
                    }
                    if (scene.galleries.isNotEmpty()) {
                        galleries.value = queryEngine.getGalleries(scene.galleries.map { it.id })
                    }
                } else {
                    loadingState.value = SceneLoadingState.Error
                }
            } catch (ex: Exception) {
                loadingState.value = SceneLoadingState.Error
            }
        }
        return this
    }

    fun addPerformer(performerId: String) = mutatePerformers { add(performerId) }

    fun removePerformer(performerId: String) = mutatePerformers { remove(performerId) }

    private fun mutatePerformers(mutator: MutableList<String>.() -> Unit) {
        val perfs = performers.value?.map { it.id }
        perfs?.let {
            val mutable = it.toMutableList()
            mutator.invoke(mutable)
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                performers.value =
                    mutationEngine
                        .setPerformersOnScene(sceneId, mutable)
                        ?.performers
                        ?.map { it.performerData }
                        .orEmpty()
            }
        }
    }

    fun addTag(id: String) = mutateTags { add(id) }

    fun removeTag(id: String) = mutateTags { remove(id) }

    private fun mutateTags(mutator: MutableList<String>.() -> Unit) {
        val ids = tags.value?.map { it.id }
        ids?.let {
            val mutable = it.toMutableList()
            mutator.invoke(mutable)
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                tags.value =
                    mutationEngine
                        .setTagsOnScene(sceneId, mutable)
                        ?.tags
                        ?.map { it.tagData }
                        .orEmpty()
            }
        }
    }

    fun addGroup(id: String) = mutateGroup { add(id) }

    fun removeGroup(id: String) = mutateGroup { remove(id) }

    private fun mutateGroup(mutator: MutableList<String>.() -> Unit) {
        val ids = groups.value?.map { it.id }
        ids?.let {
            val mutable = it.toMutableList()
            mutator.invoke(mutable)
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                groups.value =
                    mutationEngine
                        .setGroupsOnScene(sceneId, mutable)
                        ?.groups
                        ?.map { it.group.groupData }
                        .orEmpty()
            }
        }
    }

    fun addMarker(marker: MarkerData) {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
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
            }
        }
    }

    fun removeMarker(id: String) {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            if (mutationEngine.deleteMarker(id)) {
                markers.value = markers.value?.filter { it.id != id }.orEmpty()
            }
        }
    }

    fun updateOCount(action: suspend MutationEngine.(String) -> OCounter) {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val newOCount = action.invoke(mutationEngine, sceneId)
            oCount.value = newOCount.count
        }
    }

    fun updateRating(rating100: Int) {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val newRating =
                mutationEngine.setRating(sceneId, rating100)?.rating100 ?: 0
            this@SceneDetailsViewModel.rating100.value = newRating
            showSetRatingToast(StashApplication.getApplication(), newRating)
        }
    }

    companion object {
        val SERVER_KEY = object : CreationExtras.Key<StashServer> {}
        val SCENE_ID_KEY = object : CreationExtras.Key<String> {}
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val server = this[SERVER_KEY]!!
                    val sceneId = this[SCENE_ID_KEY]!!
                    SceneDetailsViewModel(server, sceneId).init()
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

@Composable
fun SceneDetailsPage(
    server: StashServer,
    sceneId: String,
    itemOnClick: ItemOnClicker<Any>,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel =
        ViewModelProvider.create(
            LocalViewModelStoreOwner.current!!,
            SceneDetailsViewModel.Factory,
            MutableCreationExtras().apply {
                set(SceneDetailsViewModel.SERVER_KEY, server)
                set(SceneDetailsViewModel.SCENE_ID_KEY, sceneId)
            },
        )[SceneDetailsViewModel::class]
    val loadingState by viewModel.loadingState.observeAsState()
    val tags by viewModel.tags.observeAsState(listOf())
    val performers by viewModel.performers.observeAsState(listOf())
    val galleries by viewModel.galleries.observeAsState(listOf())
    val groups by viewModel.groups.observeAsState(listOf())
    val markers by viewModel.markers.observeAsState(listOf())
    val rating100 by viewModel.rating100.observeAsState(0)
    val oCount by viewModel.oCount.observeAsState(0)

    when (val state = loadingState) {
        SceneLoadingState.Error ->
            Text(
                "Error",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        SceneLoadingState.Loading ->
            Text(
                "Loading...",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        is SceneLoadingState.Success ->
            SceneDetails(
                server = server,
                scene = state.scene,
                rating100 = rating100,
                oCount = oCount,
                tags = tags,
                performers = performers,
                galleries = galleries,
                groups = groups,
                markers = markers,
                uiConfig = ComposeUiConfig.fromStashServer(server),
                itemOnClick = itemOnClick,
                playOnClick = playOnClick,
                addItem = { item ->
                    when (item) {
                        is PerformerData, is SlimPerformerData -> viewModel.addPerformer(item.id)
                        is TagData, is SlimTagData -> viewModel.addTag(item.id)
                        is GroupData, is GroupRelationshipData -> viewModel.addGroup(item.id)
                        is GalleryData -> TODO()
                        is StudioData -> TODO()
                        is MarkerData -> viewModel.addMarker(item)

                        is FullMarkerData -> throw UnsupportedOperationException()
                        is ImageData, is ExtraImageData -> throw UnsupportedOperationException()
                        is SlimSceneData, is FullSceneData, is VideoSceneData, is MinimalSceneData -> throw UnsupportedOperationException()
                    }
                },
                removeItem = { item ->
                    when (item) {
                        is PerformerData, is SlimPerformerData -> viewModel.removePerformer(item.id)
                        is TagData, is SlimTagData -> viewModel.removeTag(item.id)
                        is GroupData, is GroupRelationshipData -> viewModel.removeGroup(item.id)
                        is GalleryData -> TODO()
                        is StudioData -> TODO()
                        is MarkerData, is FullMarkerData -> viewModel.removeMarker(item.id)

                        is ImageData, is ExtraImageData -> throw UnsupportedOperationException()
                        is SlimSceneData, is FullSceneData, is VideoSceneData, is MinimalSceneData -> throw UnsupportedOperationException()
                    }
                },
                oCountAction = viewModel::updateOCount,
                onRatingChange = {
                    viewModel.updateRating(it)
                },
                modifier = modifier.animateContentSize(),
            )

        null -> {}
    }
}

data class SearchForParams(
    val dataType: DataType,
    val id: Long = -1L,
)

data class DialogParams(
    val fromLongClick: Boolean,
    val title: String,
    val items: List<DialogItem>,
)

@Composable
fun SceneDetails(
    server: StashServer,
    scene: FullSceneData,
    rating100: Int,
    oCount: Int,
    tags: List<TagData>,
    performers: List<PerformerData>,
    galleries: List<GalleryData>,
    groups: List<GroupData>,
    markers: List<MarkerData>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    addItem: (item: StashData) -> Unit,
    removeItem: (item: StashData) -> Unit,
    oCountAction: (action: suspend MutationEngine.(String) -> OCounter) -> Unit,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showRatingBar: Boolean = true,
) {
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf<DialogParams?>(null) }
    var searchForDataType by remember { mutableStateOf<SearchForParams?>(null) }

    val removeLongClicker =
        LongClicker<Any> { item, filterAndPosition ->
            item as StashData
            showDialog =
                DialogParams(
                    title = extractTitle(item) ?: "",
                    fromLongClick = true,
                    items =
                        buildList {
                            add(
                                DialogItem("Go to", Icons.Default.PlayArrow) {
                                    itemOnClick.onClick(
                                        item,
                                        filterAndPosition,
                                    )
                                },
                            )
                            if (item !is GalleryData) {
                                add(
                                    DialogItem(
                                        onClick = { removeItem(item) },
                                        headlineContent = {
                                            Text(stringResource(R.string.stashapp_actions_remove))
                                        },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.stashapp_actions_remove),
                                                tint = Color.Red,
                                            )
                                        },
                                    ),
                                )
                            }
                        },
                )
        }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 135.dp),
        modifier = modifier,
    ) {
        item {
            SceneDetailsHeader(
                scene = scene,
                rating100 = rating100,
                oCount = oCount,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                playOnClick = playOnClick,
                showRatingBar = showRatingBar,
                onRatingChange = onRatingChange,
                moreOnClick = {
                    showDialog =
                        DialogParams(
                            title = context.getString(R.string.more) + "...",
                            fromLongClick = false,
                            items =
                                listOf(
                                    DialogItem(
                                        context.getString(R.string.play_direct),
                                        Icons.Default.PlayArrow,
                                    ) {
                                        playOnClick(
                                            scene.resume_position ?: 0,
                                            PlaybackMode.ForcedDirectPlay,
                                        )
                                    },
                                    DialogItem(
                                        context.getString(R.string.play_transcoding),
                                        Icons.Default.PlayArrow,
                                    ) {
                                        playOnClick(
                                            scene.resume_position ?: 0,
                                            PlaybackMode.ForcedTranscode("HLS"), // TODO
                                        )
                                    },
                                    DialogItem(
                                        headlineContent = {
                                            Text(
                                                text = stringResource(R.string.stashapp_actions_create_marker),
                                            )
                                        },
                                        supportingContent = {
                                            Text(text = durationToString(scene.resume_time ?: 0.0))
                                        },
                                        leadingContent = {
                                            Text(
                                                text = stringResource(DataType.MARKER.iconStringId),
                                                fontFamily = FontAwesome,
                                            )
                                        },
                                        onClick = {
                                            searchForDataType =
                                                SearchForParams(
                                                    DataType.TAG,
                                                    scene.resume_position ?: 0L,
                                                )
                                        },
                                    ),
                                    DialogItem(
                                        context.getString(R.string.add_group),
                                        DataType.GROUP.iconStringId,
                                    ) {
                                        searchForDataType = SearchForParams(DataType.GROUP)
                                    },
                                    DialogItem(
                                        context.getString(R.string.add_performer),
                                        DataType.PERFORMER.iconStringId,
                                    ) {
                                        searchForDataType = SearchForParams(DataType.PERFORMER)
                                    },
                                    DialogItem(
                                        context.getString(R.string.add_tag),
                                        DataType.TAG.iconStringId,
                                    ) {
                                        searchForDataType = SearchForParams(DataType.TAG)
                                    },
                                ),
                        )
                },
                oCounterOnClick = { oCountAction.invoke(MutationEngine::incrementOCounter) },
                oCounterOnLongClick = {
                    showDialog =
                        DialogParams(
                            title = context.getString(R.string.stashapp_o_counter),
                            fromLongClick = true,
                            items =
                                listOf(
                                    DialogItem(context.getString(R.string.increment)) {
                                        oCountAction.invoke(MutationEngine::incrementOCounter)
                                    },
                                    DialogItem(context.getString(R.string.decrement)) {
                                        oCountAction.invoke(MutationEngine::decrementOCounter)
                                    },
                                    DialogItem(context.getString(R.string.reset)) {
                                        oCountAction.invoke(MutationEngine::resetOCounter)
                                    },
                                ),
                        )
                },
            )
        }
        val startPadding = 24.dp
        val bottomPadding = 16.dp
        if (markers.isNotEmpty()) {
            item {
                ItemsRow(
                    title = R.string.stashapp_markers,
                    items = markers,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = removeLongClicker,
                    modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
                )
            }
        }
        if (groups.isNotEmpty()) {
            item {
                ItemsRow(
                    title = R.string.stashapp_groups,
                    items = groups,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = removeLongClicker,
                    modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
                )
            }
        }
        if (performers.isNotEmpty()) {
            item {
                ItemsRow(
                    title = R.string.stashapp_performers,
                    items = performers,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = removeLongClicker,
                    modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
                )
            }
        }
        if (tags.isNotEmpty()) {
            item {
                ItemsRow(
                    title = R.string.stashapp_tags,
                    items = tags,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = removeLongClicker,
                    modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
                )
            }
        }
        if (galleries.isNotEmpty()) {
            item {
                ItemsRow(
                    title = R.string.stashapp_galleries,
                    items = galleries,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = removeLongClicker,
                    modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
                )
            }
        }
        item {
            SceneDetailsFooter(
                scene,
                Modifier.padding(start = startPadding, bottom = bottomPadding, top = 24.dp),
            )
        }
    }
    showDialog?.let { params ->
        DialogPopup(
            showDialog = true,
            title = params.title,
            dialogItems = params.items,
            onDismissRequest = { showDialog = null },
            waitToLoad = params.fromLongClick,
        )
    }
    searchForDataType?.let { params ->
        Material3MainTheme {
            Dialog(
                onDismissRequest = { searchForDataType = null },
                properties =
                    DialogProperties(
                        usePlatformDefaultWidth = false,
                    ),
            ) {
                val elevatedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                val color = MaterialTheme.colorScheme.secondaryContainer
                Box(
                    Modifier
                        .fillMaxSize(.9f)
                        .graphicsLayer {
                            this.clip = true
                            this.shape = RoundedCornerShape(28.0.dp)
                        }.drawBehind { drawRect(color = color) }
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(PaddingValues(12.dp)),
                    propagateMinConstraints = true,
                ) {
                    SearchForPage(
                        server = server,
                        title = "Add " + stringResource(params.dataType.stringId),
                        searchId = params.id,
                        dataType = params.dataType,
                        itemOnClick = { id, item ->
                            // Close dialog
                            searchForDataType = null
                            if (item is TagData && id >= 0) {
                                // Marker primary tag
                                val marker = fakeMarker(item.id, id.toSeconds, scene)
                                addItem.invoke(marker)
                            } else {
                                addItem.invoke(item)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SceneDetailsHeader(
    scene: FullSceneData,
    rating100: Int,
    oCount: Int,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    moreOnClick: () -> Unit,
    oCounterOnClick: () -> Unit,
    oCounterOnLongClick: () -> Unit,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showRatingBar: Boolean = true,
) {
    val context = LocalContext.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Box(
        modifier =
            modifier
                .fillMaxWidth()
//                .fillMaxHeight(.33f)
                .height(460.dp)
                .bringIntoViewRequester(bringIntoViewRequester),
    ) {
        if (scene.paths.screenshot.isNotNullOrBlank()) {
            val gradientColor = MaterialTheme.colorScheme.background
            AsyncImage(
                model = scene.paths.screenshot,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, gradientColor),
                                    startY = 500f,
                                ),
                            )
                            drawRect(
                                Brush.horizontalGradient(
                                    colors = listOf(gradientColor, Color.Transparent),
                                    endX = 400f,
                                    startX = 100f,
                                ),
                            )
//                            drawRect(
//                                Brush.linearGradient(
//                                    colors = listOf(gradientColor, Color.Transparent),
//                                    start = Offset(x = 500f, y = 500f),
//                                    end = Offset(x = 1000f, y = 0f),
//                                ),
//                            )
                        },
            )
        }
        Column(modifier = Modifier.fillMaxWidth(0.8f)) {
            Spacer(modifier = Modifier.height(60.dp))
            Column(
                modifier = Modifier.padding(start = 16.dp),
            ) {
                // Title
                Text(
                    text = scene.titleOrFilename ?: "",
//                        color = MaterialTheme.colorScheme.onBackground,
                    color = Color.LightGray,
                    style =
                        MaterialTheme.typography.displayMedium.copy(
                            shadow =
                                Shadow(
                                    color = Color.DarkGray,
                                    offset = Offset(5f, 2f),
                                    blurRadius = 2f,
                                ),
                        ),
                )

                Column(
                    modifier = Modifier.alpha(0.75f),
                ) {
                    // Rating
                    if (showRatingBar) {
                        StarRating(
                            rating100 = rating100,
                            precision = uiConfig.starPrecision,
                            onRatingChange = onRatingChange,
                            enabled = true,
                            modifier =
                                Modifier
                                    .height(30.dp)
                                    .padding(start = 12.dp),
                        )
                    }
                    // Quick info
                    val file = scene.files.firstOrNull()?.videoFile
                    DotSeparatedRow(
                        modifier = Modifier.padding(top = 6.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        texts =
                            listOfNotNullOrBlank(
                                scene.date,
                                file?.let { durationToString(it.duration) },
                                file?.resolutionName(),
                                file?.bitRateString(),
                            ),
                    )
                    // Description
                    if (scene.details.isNotNullOrBlank()) {
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused = interactionSource.collectIsFocusedAsState().value
                        val bgColor =
                            if (isFocused) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .75f)
                            } else {
                                Color.Unspecified
                            }
                        var textOverflow by remember { mutableStateOf(false) }
                        var showDetailsDialog by remember { mutableStateOf(false) }
                        Box(
                            modifier =
                                Modifier
                                    .background(bgColor, shape = RoundedCornerShape(8.dp))
                                    .focusable(
                                        enabled = textOverflow,
                                        interactionSource = interactionSource,
                                    ).onFocusChanged {
                                        if (it.isFocused) {
                                            scope.launch { bringIntoViewRequester.bringIntoView() }
                                        }
                                    }.clickable(enabled = textOverflow) { showDetailsDialog = true },
                        ) {
                            Text(
                                text = scene.details,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { textLayoutResult ->
                                    textOverflow = textLayoutResult.hasVisualOverflow
                                },
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                        // TODO, need to focus so d-pad can scroll the dialog
                        if (showDetailsDialog) {
                            val focusRequester = remember { FocusRequester() }
                            val focusManager = LocalFocusManager.current

                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                                focusManager.moveFocus(FocusDirection.Enter)
                            }
                            Dialog(
                                onDismissRequest = { showDetailsDialog = false },
                                properties = DialogProperties(),
                            ) {
                                Column(
                                    modifier =
                                        Modifier
                                            .focusRequester(focusRequester)
                                            .focusProperties { exit = { FocusRequester.Cancel } }
                                            .focusGroup()
                                            .height(400.dp)
                                            .background(
                                                MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(8.dp),
                                            ),
                                ) {
                                    Text(
                                        text = scene.details,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier =
                                            Modifier
                                                .padding(8.dp)
                                                .verticalScroll(rememberScrollState()),
//                                                .focusable()
//                                                .focusRequester(focusRequester)
//                                                .onFocusChanged {
//                                                    Log.v("SceneDetails", "isFocused=${it.isFocused}")
//                                                },
                                    )
                                }
                            }
                        }
                    }
                    // Key-Values
                    Row(
                        modifier =
                            Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (scene.studio != null) {
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused = interactionSource.collectIsFocusedAsState().value
                            val bgColor =
                                if (isFocused) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .75f)
                                } else {
                                    Color.Unspecified
                                }
                            val navigationManager = LocalGlobalContext.current.navigationManager
                            TitleValueText(
                                stringResource(R.string.stashapp_studio),
                                scene.studio.studioData.name,
                                modifier =
                                    Modifier
                                        .background(bgColor, shape = RoundedCornerShape(8.dp))
                                        .clickable(enabled = true) {
                                            navigationManager.navigate(
                                                Destination.Item(
                                                    DataType.STUDIO,
                                                    scene.studio.studioData.id,
                                                ),
                                            )
                                        }.onFocusChanged {
                                            if (it.isFocused) {
                                                scope.launch { bringIntoViewRequester.bringIntoView() }
                                            }
                                        }.focusable(
                                            enabled = true,
                                            interactionSource = interactionSource,
                                        ),
                            )
                        }
                        if (scene.code.isNotNullOrBlank()) {
                            TitleValueText(
                                stringResource(R.string.stashapp_scene_code),
                                scene.code,
                            )
                        }
                        if (scene.director.isNotNullOrBlank()) {
                            TitleValueText(
                                stringResource(R.string.stashapp_director),
                                scene.director,
                            )
                        }
                        TitleValueText(
                            stringResource(R.string.stashapp_play_count),
                            (scene.play_count ?: 0).toString(),
                        )
                        TitleValueText(
                            stringResource(R.string.stashapp_play_duration),
                            durationToString(scene.play_duration ?: 0.0),
                        )
                    }
                    // Playback controls
                    PlayButtons(
                        scene = scene,
                        oCount = oCount,
                        playOnClick = playOnClick,
                        moreOnClick = moreOnClick,
                        oCounterOnClick = oCounterOnClick,
                        oCounterOnLongClick = oCounterOnLongClick,
                        buttonOnFocusChanged = {
                            if (it.isFocused) {
                                scope.launch { bringIntoViewRequester.bringIntoView() }
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayButtons(
    scene: FullSceneData,
    oCount: Int,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    moreOnClick: () -> Unit,
    oCounterOnClick: () -> Unit,
    oCounterOnLongClick: () -> Unit,
    buttonOnFocusChanged: (FocusState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstFocus = remember { FocusRequester() }
    val resume = scene.resume_position ?: 0
    LazyRow(
        modifier =
            modifier
                .padding(top = 24.dp, bottom = 24.dp)
                .focusGroup()
                .focusRestorer { firstFocus },
    ) {
        if (resume > 0) {
            item {
                LaunchedEffect(Unit) { firstFocus.requestFocus() }
                PlayButton(
                    R.string.resume,
                    resume,
                    Icons.Default.PlayArrow,
                    PlaybackMode.Choose,
                    playOnClick,
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged)
                        .focusRequester(firstFocus),
                )
            }
            item {
                PlayButton(
                    R.string.restart,
                    0L,
                    Icons.Default.Refresh,
                    PlaybackMode.Choose,
                    playOnClick,
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged),
                )
            }
        } else {
            item {
                LaunchedEffect(Unit) { firstFocus.requestFocus() }
                PlayButton(
                    R.string.play_scene,
                    0L,
                    Icons.Default.PlayArrow,
                    PlaybackMode.Choose,
                    playOnClick,
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged)
                        .focusRequester(firstFocus),
                )
            }
        }
        // O-Counter
        item {
            Button(
                onClick = oCounterOnClick,
                onLongClick = oCounterOnLongClick,
                modifier =
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    painter = painterResource(R.drawable.sweat_drops),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = oCount.toString(),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        // More button
        item {
            Button(
                onClick = moreOnClick,
                onLongClick = {},
                modifier =
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.more),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
fun PlayButton(
    @StringRes title: Int,
    resume: Long,
    icon: ImageVector,
    mode: PlaybackMode,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { playOnClick.invoke(resume, mode) },
        modifier = modifier,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
fun SceneDetailsFooter(
    scene: FullSceneData,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TitleValueText(stringResource(R.string.stashapp_scene_id), scene.id)
        if (scene.created_at.toString().length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_created_at),
                scene.created_at.toString().substring(0..<10),
            )
        }
        if (scene.updated_at.toString().length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_updated_at),
                scene.updated_at.toString().substring(0..<10),
            )
        }
        val file = scene.files.firstOrNull()?.videoFile
        if (file != null) {
            TitleValueText(
                stringResource(R.string.stashapp_video_codec),
                file.video_codec,
            )
            TitleValueText(
                stringResource(R.string.stashapp_audio_codec),
                file.audio_codec,
            )
            TitleValueText(
                stringResource(R.string.format),
                file.format,
            )
        }
        if (!scene.captions.isNullOrEmpty()) {
            val str =
                buildString {
                    append(
                        scene.captions
                            .first()
                            .caption
                            .displayString(LocalContext.current),
                    )
                    if (scene.captions.size > 1) {
                        append(", +${scene.captions.size - 1} more")
                    }
                }
            TitleValueText(
                stringResource(R.string.stashapp_captions),
                str,
            )
        }
        TitleValueText(
            stringResource(R.string.stashapp_organized),
            scene.organized.toString(),
        )
    }
}

@Composable
fun <T : StashData> ItemsRow(
    @StringRes title: Int,
    items: List<T>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) = ItemsRow(
    title = stringResource(title),
    items = items,
    uiConfig = uiConfig,
    itemOnClick = itemOnClick,
    longClicker = longClicker,
    modifier = modifier,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : StashData> ItemsRow(
    title: String,
    items: List<T>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    val firstFocus = remember { FocusRequester() }
    Column(
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .focusRestorer { firstFocus },
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val cardModifier =
                    if (index == 0) {
                        Modifier.focusRequester(firstFocus)
                    } else {
                        Modifier
                    }
                StashCard(
                    uiConfig = uiConfig,
                    item = item,
                    itemOnClick = { itemOnClick.onClick(item, null) },
                    longClicker = longClicker,
                    getFilterAndPosition = null,
                    modifier = cardModifier,
                )
            }
        }
    }
}
