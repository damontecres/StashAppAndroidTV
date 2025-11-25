package com.github.damontecres.stashapp.ui.pages

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
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
import com.github.damontecres.stashapp.api.fragment.SlimImageData
import com.github.damontecres.stashapp.api.fragment.SlimPerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.SlimTagData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.proto.StreamChoice
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.DefaultLongClicker
import com.github.damontecres.stashapp.ui.components.DeleteDialog
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.FocusPair
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.MarkerDurationDialog
import com.github.damontecres.stashapp.ui.components.RowColumn
import com.github.damontecres.stashapp.ui.components.scene.SceneDescriptionDialog
import com.github.damontecres.stashapp.ui.components.scene.SceneDetailsFooter
import com.github.damontecres.stashapp.ui.components.scene.SceneDetailsHeader
import com.github.damontecres.stashapp.ui.components.scene.SceneDetailsViewModel
import com.github.damontecres.stashapp.ui.components.scene.SceneLoadingState
import com.github.damontecres.stashapp.ui.components.scene.sceneDetailsBody
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.asString
import com.github.damontecres.stashapp.util.fakeMarker
import com.github.damontecres.stashapp.util.resume_position
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.util.toSeconds
import com.github.damontecres.stashapp.views.durationToString

@Composable
fun SceneDetailsPage(
    server: StashServer,
    navigationManager: NavigationManager,
    sceneId: String,
    itemOnClick: ItemOnClicker<Any>,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
    onUpdateTitle: ((AnnotatedString) -> Unit)? = null,
) {
    val viewModel =
        ViewModelProvider.create(
            LocalViewModelStoreOwner.current!!,
            SceneDetailsViewModel.Factory,
            MutableCreationExtras().apply {
                set(SceneDetailsViewModel.SERVER_KEY, server)
                set(SceneDetailsViewModel.SCENE_ID_KEY, sceneId)
                set(
                    SceneDetailsViewModel.PAGE_SIZE_KEY,
                    uiConfig.preferences.searchPreferences.maxResults,
                )
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
    val studio by viewModel.studio.observeAsState(null)
    val suggestions by viewModel.suggestions.observeAsState(listOf())
    val context = LocalContext.current

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

        is SceneLoadingState.Success -> {
            LaunchedEffect(Unit) {
                state.scene.titleOrFilename?.let {
                    onUpdateTitle?.invoke(AnnotatedString(it))
                }
            }
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
                studio = studio,
                suggestions = suggestions,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                playOnClick = playOnClick,
                addItem = { item ->
                    when (item) {
                        is PerformerData, is SlimPerformerData -> viewModel.addPerformer(item.id)
                        is TagData, is SlimTagData -> viewModel.addTag(item.id)
                        is GroupData, is GroupRelationshipData -> viewModel.addGroup(item.id)
                        is GalleryData -> viewModel.addGallery(item.id)
                        is StudioData -> viewModel.setStudio(item.id)
                        is MarkerData -> viewModel.addMarker(item)

                        is FullMarkerData -> throw UnsupportedOperationException()
                        is ImageData, is ExtraImageData, is SlimImageData -> throw UnsupportedOperationException()
                        is SlimSceneData, is FullSceneData, is VideoSceneData, is MinimalSceneData -> throw UnsupportedOperationException()
                    }
                },
                removeItem = { item ->
                    when (item) {
                        is PerformerData, is SlimPerformerData -> viewModel.removePerformer(item.id)
                        is TagData, is SlimTagData -> viewModel.removeTag(item.id)
                        is GroupData, is GroupRelationshipData -> viewModel.removeGroup(item.id)
                        is GalleryData -> viewModel.removeGallery(item.id)
                        is StudioData -> viewModel.removeStudio()
                        is MarkerData, is FullMarkerData -> viewModel.removeMarker(item.id)

                        is ImageData, is ExtraImageData, is SlimImageData -> throw UnsupportedOperationException()
                        is SlimSceneData, is FullSceneData, is VideoSceneData, is MinimalSceneData -> throw UnsupportedOperationException()
                    }
                },
                oCountAction = viewModel::updateOCount,
                onRatingChange = {
                    viewModel.updateRating(it)
                },
                onSceneDelete = { deleteFiles, deleteGenerated ->
                    viewModel.deleteScene(deleteFiles, deleteGenerated) {
                        if (it) {
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                            navigationManager.goBack()
                        } else {
                            Toast
                                .makeText(context, "Error deleting scene", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                },
                modifier = modifier.animateContentSize(),
            )
        }

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
    studio: StudioData?,
    suggestions: List<SlimSceneData>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    addItem: (item: StashData) -> Unit,
    removeItem: (item: StashData) -> Unit,
    onSceneDelete: (deleteFiles: Boolean, deleteGenerated: Boolean) -> Unit,
    oCountAction: (action: suspend MutationEngine.(String) -> OCounter) -> Unit,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showRatingBar: Boolean = true,
) {
    val context = LocalContext.current
    val navigationManager = LocalGlobalContext.current.navigationManager

    var showDialog by remember { mutableStateOf<DialogParams?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var searchForDataType by remember { mutableStateOf<SearchForParams?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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

    var showMarkerDurationDialog by remember { mutableStateOf(false) }
    if (showMarkerDurationDialog) {
        MarkerDurationDialog(
            onDismissRequest = { showMarkerDurationDialog = false },
            onClick = { playAllMarkers(navigationManager, scene.id, it) },
        )
    }

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
//    LifecycleStartEffect(Unit) {
//        onStopOrDispose {
//            savedFocusPosition = focusPosition
//        }
//    }

    val cardOnFocus = { isFocused: Boolean, row: Int, column: Int ->
        if (isFocused) {
            focusPosition = RowColumn(row, column)
        } else if (focusPosition?.let { it.row == row && it.column == column } == true) {
            savedFocusPosition = focusPosition
//            focusPosition = null
        }
//        Log.v(
//            "SceneDetails",
//            "cardOnFocus: isFocused=$isFocused, row=$row, column=$column, savedFocusPosition=$savedFocusPosition, focusPosition=$focusPosition",
//        )
    }
    val focusManager = LocalFocusManager.current
    val defaultLongClicker =
        remember {
            DefaultLongClicker(
                navigationManager,
                itemOnClick,
                server.serverPreferences.alwaysStartFromBeginning,
                markerPlayAllOnClick = { },
            ) { showDialog = it }
        }
    val removeLongClicker =
        remember {
            LongClicker<Any> { item, filterAndPosition ->
                item as StashData
                showDialog =
                    DialogParams(
                        title = extractTitle(item) ?: "",
                        fromLongClick = true,
                        items =
                            buildList {
                                add(
                                    DialogItem(
                                        context.getString(R.string.go_to),
                                        Icons.Default.PlayArrow,
                                    ) {
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
                                            navigationManager.navigate(
                                                Destination.MarkerDetails(
                                                    item.id,
                                                ),
                                            )
                                        },
                                    )
                                    if (uiConfig.readOnlyModeDisabled) {
                                        add(
                                            DialogItem(
                                                context.getString(R.string.timestamps),
                                                Icons.Default.Edit,
                                            ) {
                                                navigationManager.navigate(
                                                    Destination.UpdateMarker(
                                                        item.id,
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                }
                                if (uiConfig.readOnlyModeDisabled && item !is SlimSceneData) {
                                    if (item is StudioData) {
                                        add(
                                            DialogItem(
                                                context.getString(R.string.replace),
                                                Icons.Default.Edit,
                                            ) {
                                                searchForDataType = SearchForParams(DataType.STUDIO)
                                            },
                                        )
                                    }
                                    add(
                                        DialogItem(
                                            onClick = {
                                                when (item) {
                                                    is StudioData -> headerFocusRequester.tryRequestFocus()
                                                    else -> focusManager.moveFocus(FocusDirection.Previous)
                                                }
                                                removeItem(item)
                                            },
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
        }
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 135.dp),
        modifier = modifier,
    ) {
        item {
            SceneDetailsHeader(
                modifier =
                    Modifier.focusProperties {
                        onEnter = {
                            savedFocusPosition = null
                            focusPosition = null
                        }
                    },
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
                removeLongClicker = removeLongClicker,
                studio = studio,
                alwaysStartFromBeginning = server.serverPreferences.alwaysStartFromBeginning,
                showEditButton = uiConfig.readOnlyModeDisabled,
                editOnClick = {
                    showDialog =
                        DialogParams(
                            title = context.getString(R.string.stashapp_actions_edit) + "...",
                            fromLongClick = false,
                            items =
                                listOf(
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
                                        context.getString(R.string.add_tag),
                                        DataType.TAG.iconStringId,
                                    ) {
                                        searchForDataType = SearchForParams(DataType.TAG)
                                    },
                                    DialogItem(
                                        context.getString(R.string.add_performer),
                                        DataType.PERFORMER.iconStringId,
                                    ) {
                                        searchForDataType = SearchForParams(DataType.PERFORMER)
                                    },
                                    DialogItem(
                                        context.getString(R.string.set_studio),
                                        DataType.STUDIO.iconStringId,
                                    ) {
                                        searchForDataType = SearchForParams(DataType.STUDIO)
                                    },
                                    DialogItem(
                                        context.getString(R.string.add_group),
                                        DataType.GROUP.iconStringId,
                                    ) {
                                        searchForDataType = SearchForParams(DataType.GROUP)
                                    },
                                    DialogItem(
                                        context.getString(R.string.add_gallery),
                                        DataType.GALLERY.iconStringId,
                                    ) {
                                        searchForDataType = SearchForParams(DataType.GALLERY)
                                    },
                                ) +
                                    buildList {
                                        if (uiConfig.preferences.advancedPreferences.experimentalFeaturesEnabled) {
                                            add(
                                                DialogItem(
                                                    context.getString(R.string.stashapp_actions_delete),
                                                    Icons.Default.Delete,
                                                    Color.Red,
                                                ) { showDeleteDialog = true },
                                            )
                                        }
                                    },
                        )
                },
                moreOnClick = {
                    showDialog =
                        DialogParams(
                            title = context.getString(R.string.more) + "...",
                            fromLongClick = false,
                            items =
                                moreOptionsItems(
                                    server = server,
                                    context = context,
                                    navigationManager = navigationManager,
                                    scene = scene,
                                    markers = markers,
                                    playOnClick = playOnClick,
                                    showDurationDialog = { showMarkerDurationDialog = true },
                                    detailsOnClick = { showDetailsDialog = true },
                                    streamChoice = uiConfig.preferences.playbackPreferences.streamChoice,
                                ),
                        )
                },
                detailsOnClick = { showDetailsDialog = true },
                oCounterOnClick = { oCountAction.invoke(MutationEngine::incrementOCounter) },
                oCounterOnLongClick = {
                    showDialog =
                        DialogParams(
                            title = context.getString(R.string.stashapp_o_count),
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

        sceneDetailsBody(
            scene = scene,
            tags = tags,
            performers = performers,
            galleries = galleries,
            groups = groups,
            markers = markers,
            suggestions = suggestions,
            uiConfig = uiConfig,
            itemOnClick = itemOnClick,
            removeLongClicker = removeLongClicker,
            defaultLongClicker = defaultLongClicker,
            cardOnFocus = cardOnFocus,
            createFocusPair = createFocusPair,
            startPadding = startPadding,
            bottomPadding = bottomPadding,
        )

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
            focusPosition = savedFocusPosition
//            Log.v("SceneDetails", "Focusing on $focusPosition")
            focusPositionRequester.tryRequestFocus()
        } else {
            bringIntoViewRequester.bringIntoView()
            listState.animateScrollToItem(0)
            headerFocusRequester.tryRequestFocus()
        }
    }
    SceneDescriptionDialog(
        show = showDetailsDialog,
        scene = scene,
        onDismissRequest = { showDetailsDialog = false },
    )
    if (showDeleteDialog) {
        DeleteDialog(
            onDismissRequest = { showDeleteDialog = false },
            dataType = DataType.SCENE,
            name = scene.titleOrFilename ?: "",
            files = scene.files.map { it.videoFile.path },
            onDeleteConfirm = { deleteFiles, deleteGenerated ->
                showDeleteDialog = false
                onSceneDelete.invoke(deleteFiles, deleteGenerated)
            },
        )
    }
}

fun moreOptionsItems(
    server: StashServer,
    context: Context,
    navigationManager: NavigationManager,
    scene: FullSceneData,
    markers: List<MarkerData>,
    streamChoice: StreamChoice,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    showDurationDialog: () -> Unit,
    detailsOnClick: () -> Unit,
): List<DialogItem> =
    buildList {
        add(
            DialogItem(
                context.getString(R.string.stashapp_details),
                Icons.Default.Info,
                onClick = detailsOnClick,
            ),
        )
        add(
            DialogItem(
                context.getString(R.string.play_direct),
                Icons.Default.PlayArrow,
            ) {
                playOnClick(
                    if (server.serverPreferences.alwaysStartFromBeginning) {
                        0L
                    } else {
                        scene.resume_position ?: 0
                    },
                    PlaybackMode.ForcedDirectPlay,
                )
            },
        )
        add(
            DialogItem(
                context.getString(R.string.play_transcoding),
                Icons.Default.PlayArrow,
            ) {
                // TODO show options for other resolutions
                val format = streamChoice.asString
                playOnClick(
                    if (server.serverPreferences.alwaysStartFromBeginning) {
                        0L
                    } else {
                        scene.resume_position ?: 0
                    },
                    PlaybackMode.ForcedTranscode(format),
                )
            },
        )
        if (markers.isNotEmpty()) {
            add(
                DialogItem(
                    context.getString(R.string.play_all_markers),
                    Icons.Default.Place,
                ) {
                    if (markers.firstOrNull { it.end_seconds == null } != null) {
                        showDurationDialog.invoke()
                    } else {
                        playAllMarkers(navigationManager, scene.id, 0)
                    }
                },
            )
        }
    }

fun playAllMarkers(
    navigationManager: NavigationManager,
    sceneId: String,
    durationMs: Long,
) {
    val objectFilter =
        SceneMarkerFilterType(
            scenes =
                Optional.present(
                    MultiCriterionInput(
                        value = Optional.present(listOf(sceneId)),
                        modifier = CriterionModifier.INCLUDES,
                    ),
                ),
        )
    val filterArgs =
        FilterArgs(
            dataType = DataType.MARKER,
            objectFilter = objectFilter,
            findFilter =
                StashFindFilter(
                    SortAndDirection(
                        SortOption.Seconds,
                        SortDirectionEnum.ASC,
                    ),
                ),
        )
    val destination =
        Destination.Playlist(
            filterArgs = filterArgs,
            position = 0,
            duration = durationMs,
        )
    navigationManager.navigate(destination)
}
