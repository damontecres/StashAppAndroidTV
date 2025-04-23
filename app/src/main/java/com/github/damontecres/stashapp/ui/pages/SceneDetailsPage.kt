package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
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
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.FocusPair
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.ItemsRow
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.RowColumn
import com.github.damontecres.stashapp.ui.components.scene.SceneDetailsFooter
import com.github.damontecres.stashapp.ui.components.scene.SceneDetailsHeader
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.asMarkerData
import com.github.damontecres.stashapp.util.fakeMarker
import com.github.damontecres.stashapp.util.resume_position
import com.github.damontecres.stashapp.util.showSetRatingToast
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
                    SceneDetailsViewModel(server, sceneId)
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
    uiConfig: ComposeUiConfig,
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

    LaunchedEffect(Unit) {
        viewModel.init()
    }

    when (val state = loadingState) {
        SceneLoadingState.Error ->
            Text(
                "Error",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

        SceneLoadingState.Loading -> CircularProgress()

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
                uiConfig = uiConfig,
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

@OptIn(ExperimentalFoundationApi::class)
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
    val navigationManager = LocalGlobalContext.current.navigationManager

    var showDialog by remember { mutableStateOf<DialogParams?>(null) }
    var searchForDataType by remember { mutableStateOf<SearchForParams?>(null) }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var focusPosition by rememberSaveable { mutableStateOf<RowColumn?>(null) }
    var savedFocusPosition by rememberSaveable { mutableStateOf<RowColumn?>(null) }
    val focusPositionRequester = remember { FocusRequester() }
    val headerFocusRequester = remember { FocusRequester() }
    /* How focus restoring works (such as entering or coming back to this page):
       Focus will be either on the SceneDetailsHeader or an ItemRow
       Each card gets an onFocus modifier to callback its row & column, which is saved in focusPosition
       When restoring focus, if focusPosition is set, then pass the focusPositionRequester into that ItemRow
       That ItemRow must assign that FocusRequester to the right card
       When this page leaves composition, save the currently focused position
       Finally, on recomposing, the FocusRequester is called. If focusPosition is null, instead
       SceneHeaderDetails will be brought into view and headerFocusRequester will be called
     */

    val createFocusPair = { row: Int ->
        focusPosition?.let {
            if (it.row == row) {
                FocusPair(
                    it.row,
                    it.column,
                    focusPositionRequester,
                )
            } else {
                null
            }
        }
    }
    LifecycleStartEffect(Unit) {
        onStopOrDispose {
            savedFocusPosition = focusPosition
        }
    }

    val cardOnFocus = { isFocused: Boolean, row: Int, column: Int ->
        if (isFocused) {
            focusPosition = RowColumn(row, column)
        } else if (focusPosition?.let { it.row == row && it.column == column } == true) {
            focusPosition = null
        }
    }

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
                            if (Destination.getDataType(item) == DataType.MARKER) {
                                add(
                                    DialogItem(
                                        context.getString(R.string.stashapp_details),
                                        Icons.Default.Info,
                                    ) {
                                        navigationManager.navigate(Destination.MarkerDetails(item.id))
                                    },
                                )
                            }
                            if (item !is GalleryData && uiConfig.readOnlyModeDisabled) {
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

    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 135.dp),
        modifier = modifier,
    ) {
        item {
            SceneDetailsHeader(
                bringIntoViewRequester = bringIntoViewRequester,
                focusRequester = headerFocusRequester,
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
                    cardOnFocus = { isFocused, index ->
                        cardOnFocus.invoke(isFocused, 0, index)
                    },
                    focusPair = createFocusPair(0),
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
                    cardOnFocus = { isFocused, index ->
                        cardOnFocus.invoke(isFocused, 1, index)
                    },
                    focusPair = createFocusPair(1),
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
                    cardOnFocus = { isFocused, index ->
                        cardOnFocus.invoke(isFocused, 2, index)
                    },
                    focusPair = createFocusPair(2),
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
                    cardOnFocus = { isFocused, index ->
                        cardOnFocus.invoke(isFocused, 3, index)
                    },
                    focusPair = createFocusPair(3),
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
                    cardOnFocus = { isFocused, index ->
                        cardOnFocus.invoke(isFocused, 4, index)
                    },
                    focusPair = createFocusPair(4),
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
    SearchForDialog(
        show = searchForDataType != null,
        dataType = searchForDataType?.dataType ?: DataType.TAG,
        onItemClick = { item ->
            val id = searchForDataType!!.id
            searchForDataType = null
            if (item is TagData && id >= 0) {
                // Marker primary tag
                val marker = fakeMarker(item.id, id.toSeconds, scene)
                addItem.invoke(marker)
            } else {
                addItem.invoke(item)
            }
        },
        onDismissRequest = { searchForDataType = null },
        dialogTitle = null,
        dismissOnClick = false,
        uiConfig = uiConfig,
    )
    LaunchedEffect(Unit) {
        if (savedFocusPosition != null) {
            Log.v("SceneDetails", "Focusing on $focusPosition")
            focusPositionRequester.tryRequestFocus()
        } else {
            bringIntoViewRequester.bringIntoView()
            listState.animateScrollToItem(0)
            headerFocusRequester.tryRequestFocus()
        }
    }
}
