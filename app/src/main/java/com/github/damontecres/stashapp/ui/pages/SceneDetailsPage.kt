package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import coil3.compose.AsyncImage
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.playback.displayString
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.components.DotSeparatedRow
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.asMarkerData
import com.github.damontecres.stashapp.util.bitRateString
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.listOfNotNullOrBlank
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.resume_position
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.StashRatingBar
import com.github.damontecres.stashapp.views.durationToString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    longClicker: LongClicker<Any>,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var loadingState by remember { mutableStateOf<SceneLoadingState>(SceneLoadingState.Loading) }
    var performers by remember { mutableStateOf<List<PerformerData>>(listOf()) }
    var galleries by remember { mutableStateOf<List<GalleryData>>(listOf()) }

    LaunchedEffect(sceneId) {
        try {
            val queryEngine = QueryEngine(server)
            val scene = queryEngine.getScene(sceneId)
            if (scene != null) {
                loadingState = SceneLoadingState.Success(scene)
                if (scene.performers.isNotEmpty()) {
                    performers = queryEngine.findPerformers(performerIds = scene.performers.map { it.id })
                }
                if (scene.galleries.isNotEmpty()) {
                    galleries = queryEngine.getGalleries(scene.galleries.map { it.id })
                }
            } else {
                loadingState = SceneLoadingState.Error
            }
        } catch (ex: Exception) {
            loadingState = SceneLoadingState.Error
        }
    }
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
                scene = state.scene,
                performers = performers,
                galleries = galleries,
                uiConfig = ComposeUiConfig.fromStashServer(server),
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                playOnClick = playOnClick,
                modifier = modifier.animateContentSize(),
            )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun SceneDetails(
    scene: FullSceneData,
    performers: List<PerformerData>,
    galleries: List<GalleryData>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    var moreInteractionSource by remember { mutableStateOf(MutableInteractionSource()) }
    val pressed by moreInteractionSource.collectIsPressedAsState()

    LazyColumn(
        contentPadding = PaddingValues(bottom = 135.dp),
        modifier = modifier,
    ) {
        item {
            SceneDetailsHeader(scene, itemOnClick, playOnClick, moreOnClick = {
                showDialog = true
            }, moreInteractionSource)
        }
        val startPadding = 24.dp
        val bottomPadding = 16.dp
        if (scene.scene_markers.isNotEmpty()) {
            item {
                ItemsRow(
                    title = R.string.stashapp_markers,
                    items = scene.scene_markers.map { it.asMarkerData(scene) },
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
                )
            }
        }
        if (scene.groups.isNotEmpty()) {
            item {
                ItemsRow(
                    title = R.string.stashapp_groups,
                    items = scene.groups.map { it.group.groupData },
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
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
                    longClicker = longClicker,
                    modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
                )
            }
        }
        if (scene.tags.isNotEmpty()) {
            item {
                ItemsRow(
                    title = R.string.stashapp_tags,
                    items = scene.tags.map { it.tagData },
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
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
                    longClicker = longClicker,
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
    DialogTest(
        showDialog = showDialog,
        pressed = pressed,
        interactionSource = moreInteractionSource,
        onDismissRequest = { showDialog = false },
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun DialogTest(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    pressed: Boolean,
    interactionSource: MutableInteractionSource,
) {
    var waiting by remember { mutableStateOf(true) }
    if (showDialog) {
        LaunchedEffect(Unit) {
            delay(500)
            waiting = false
        }
    }
    if (showDialog) {
        Log.v("Compose", "pressed=$pressed")
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(),
        ) {
            val elevatedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            Column(
                modifier =
                    Modifier
//                        .widthIn(min = 520.dp, max = 300.dp)
//                        .dialogFocusable()
                        .graphicsLayer {
                            this.clip = true
                            this.shape = RoundedCornerShape(28.0.dp)
                        }.drawBehind { drawRect(color = elevatedContainerColor) }
                        .padding(PaddingValues(24.dp)),
            ) {
                Text("This is the title")
                ListItem(
                    selected = false,
                    enabled = !waiting,
                    onClick = {
                        Log.w("Compose", "ListItem clicked!")
                    },
                    headlineContent = {
                        Text("Go to")
                    },
                    modifier = Modifier,
                    interactionSource = interactionSource,
                )
                Button(
                    enabled = !waiting,
                    onClick = {
                        Log.w("Compose", "Go to clicked!")
                    },
                    onLongClick = {
                        Log.w("Compose", "Go to long clicked!")
                    },
                ) {
                    Text("Go to")
                }
                Button(
                    enabled = !waiting,
                    onClick = {},
                ) {
                    Text("Remove")
                }
                Button(
                    enabled = !waiting,
                    onClick = onDismissRequest,
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SceneDetailsHeader(
    scene: FullSceneData,
    itemOnClick: ItemOnClicker<Any>,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    moreOnClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var rating100 by remember { mutableIntStateOf(scene.rating100 ?: 0) }
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
                    AndroidView(
                        modifier = Modifier.height(40.dp),
                        factory = { context ->
                            StashRatingBar(context)
                        },
                        update = { view ->
                            view.rating100 = rating100
                            val lp = view.layoutParams
                            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
                            view.layoutParams = lp
                        },
                    )
                    // Quick info
                    val file = scene.files.firstOrNull()?.videoFile
                    DotSeparatedRow(
                        modifier = Modifier.padding(top = 20.dp),
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
//                            var borderColor by remember { mutableStateOf(Color.Transparent) }
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused = interactionSource.collectIsFocusedAsState().value
                        val borderColor =
                            if (isFocused) {
//                                    scope.launch { bringIntoViewRequester.bringIntoView() }
                                Color.Red
                            } else {
                                Color.Unspecified
                            }
                        Text(
                            text = scene.details,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier
                                    .padding(top = 8.dp)
//                                        .clickable { Toast.makeText(context, "Details clicked", Toast.LENGTH_SHORT).show() }
                                    .focusable(interactionSource = interactionSource)
                                    .border(3.dp, color = borderColor)
                                    .onFocusChanged {
                                        Log.v("SceneDetails", "Details focused: ${it.isFocused}")
                                        if (it.isFocused) {
//                                                borderColor = Color.DarkGray
                                            scope.launch { bringIntoViewRequester.bringIntoView() }
                                        } else {
//                                                borderColor = Color.Transparent
                                        }
                                    },
                        )
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
                            TitleValueText(
                                stringResource(R.string.stashapp_studio),
                                scene.studio.studioData.name,
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
                        scene,
                        playOnClick,
                        moreOnClick,
                        interactionSource,
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
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    moreOnClick: () -> Unit,
    interactionSource: MutableInteractionSource,
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
                PlayButton(
                    R.string.resume,
                    resume,
                    Icons.Default.PlayArrow,
                    PlaybackMode.CHOOSE,
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
                    PlaybackMode.CHOOSE,
                    playOnClick,
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged),
                )
            }
        } else {
            item {
                PlayButton(
                    R.string.restart,
                    0L,
                    Icons.Default.PlayArrow,
                    PlaybackMode.CHOOSE,
                    playOnClick,
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged)
                        .focusRequester(firstFocus),
                )
            }
        }
        // More button
        item {
            Button(
                interactionSource = interactionSource,
                onClick = {},
                onLongClick = {
                    moreOnClick.invoke()
                },
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : StashData> ItemsRow(
    @StringRes title: Int,
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
            text = stringResource(title),
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
