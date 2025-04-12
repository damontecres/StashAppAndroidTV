package com.github.damontecres.stashapp.ui.components.playback

import androidx.annotation.IntRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Scale
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.StreamDecision
import com.github.damontecres.stashapp.playback.TranscodeDecision
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.cards.RootCard
import com.github.damontecres.stashapp.ui.components.StarRatingPrecision
import com.github.damontecres.stashapp.ui.util.CoilPreviewTransformation
import com.github.damontecres.stashapp.util.defaultCardHeight
import com.github.damontecres.stashapp.util.defaultCardWidth
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.formatDate
import com.github.damontecres.stashapp.views.models.CardUiSettings
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ControllerViewState internal constructor(
    @IntRange(from = 0)
    private val hideMilliseconds: Int,
    val controlsEnabled: Boolean,
) {
    private val channel = Channel<Int>(CONFLATED)
    private var _controlsVisible by mutableStateOf(false)
    val controlsVisible get() = _controlsVisible

    fun showControls(milliseconds: Int = hideMilliseconds) {
        if (controlsEnabled) {
            _controlsVisible = true
        }
        pulseControls(milliseconds)
    }

    fun hideControls() {
        _controlsVisible = false
    }

    fun pulseControls(milliseconds: Int = hideMilliseconds) {
//        Log.i("PlaybackPageContent", "pulseControls=$milliseconds")
        channel.trySend(milliseconds)
    }

    @OptIn(FlowPreview::class)
    suspend fun observe() {
        channel
            .consumeAsFlow()
            .debounce { it.toLong() }
            .collect {
//                Log.i("PlaybackPageContent", "collect")
                _controlsVisible = false
            }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlaybackOverlay(
    uiConfig: ComposeUiConfig,
    scene: Scene,
    markers: List<BasicMarker>,
    streamDecision: StreamDecision?,
    oCounter: Int,
    playerControls: PlayerControls,
    controllerViewState: ControllerViewState,
    showPlay: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    seekEnabled: Boolean,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    onSeekBarChange: (Float) -> Unit,
    showDebugInfo: Boolean,
    modifier: Modifier = Modifier,
    seekPreviewPlaceholder: Painter? = null,
    seekBarInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var seekProgress by remember { mutableFloatStateOf(-1f) }
    val seekBarFocused by seekBarInteractionSource.collectIsFocusedAsState()

    val previewImageUrl = scene.spriteUrl
    val imageLoader = SingletonImageLoader.get(LocalPlatformContext.current)
    var imageLoaded by remember { mutableStateOf(false) }
    if (previewImageUrl.isNotNullOrBlank()) {
        LaunchedEffect(previewImageUrl) {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(previewImageUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .scale(Scale.FILL)
                    .build()
            val result = imageLoader.enqueue(request).job.await()
            imageLoaded = result.image != null
        }
    }

    AnimatedVisibility(
        controllerViewState.controlsVisible,
        Modifier,
        slideInVertically { it },
        slideOutVertically { it },
    ) {
        Box(
            modifier,
        ) {
            if (showDebugInfo && streamDecision != null) {
                PlaybackDebugInfo(
                    scene = scene,
                    streamDecision = streamDecision,
                    modifier =
                        Modifier
                            .padding(8.dp)
                            .background(AppColors.TransparentBlack50)
                            .align(Alignment.TopStart)
                            // TODO the width isn't be used correctly
                            .width(280.dp),
                )
            }
            val controlHeight = .4f
            val listState = rememberLazyListState()
            var height = 208.dp
            if (scene.title.isNullOrBlank()) height -= 24.dp
            if (scene.date.isNullOrBlank()) height -= 32.dp
            if (markers.isEmpty()) height -= 16.dp
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(height),
//                        .fillMaxHeight(controlHeight),
//                contentPadding = PaddingValues(top = 420.dp),
            ) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier =
                            Modifier
                                .padding(start = 8.dp)
                                .fillMaxWidth(.7f),
                    ) {
                        if (scene.title.isNotNullOrBlank()) {
                            Text(
                                text = scene.title,
                                color = MaterialTheme.colorScheme.onBackground,
                                style =
                                    MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 24.sp,
                                    ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (scene.date.isNotNullOrBlank()) {
                            Text(
                                text = formatDate(scene.date)!!,
                                color = MaterialTheme.colorScheme.onBackground,
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                    ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                item {
                    PlaybackControls(
                        modifier = Modifier.fillMaxWidth(),
                        scene = scene,
                        oCounter = oCounter,
                        playerControls = playerControls,
                        onPlaybackActionClick = onPlaybackActionClick,
                        controllerViewState = controllerViewState,
                        showDebugInfo = showDebugInfo,
                        onSeekProgress = {
                            seekProgress = it
                            onSeekBarChange(it)
                        },
                        showPlay = showPlay,
                        previousEnabled = previousEnabled,
                        nextEnabled = nextEnabled,
                        seekEnabled = seekEnabled,
                        seekBarInteractionSource = seekBarInteractionSource,
                    )
                }
                if (markers.isNotEmpty()) {
                    item {
//                    Spacer(Modifier.height(12.dp))
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = stringResource(DataType.MARKER.pluralStringId),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    item {
                        SceneMarkerBar(
                            modifier =
                                Modifier
                                    .padding(start = 8.dp, top = 64.dp, bottom = 64.dp)
                                    .fillMaxWidth()
                                    .height(height),
                            markers = markers,
                            player = playerControls,
                            controllerViewState = controllerViewState,
                            uiConfig = uiConfig,
                            onCardFocus = {
//                                scope.launch {
//                                    listState.scrollToItem(2)
//                                }
                            },
                        )
                    }
                }
            }
            AnimatedVisibility(seekBarFocused && seekProgress >= 0) {
                val yOffsetDp = height + (if (imageLoaded) (160.dp) else 24.dp) - 16.dp
                val heightPx = with(LocalDensity.current) { yOffsetDp.toPx().toInt() }
                SeekPreviewImage(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .offsetByPercent(
                                xPercentage = seekProgress.coerceIn(0f, 1f),
                                yOffset = heightPx,
//                                yPercentage = 1 - controlHeight,
                            ),
                    previewImageUrl = previewImageUrl,
                    imageLoaded = imageLoaded,
                    imageLoader = imageLoader,
                    duration = playerControls.duration,
                    seekProgress = seekProgress,
                    videoWidth = scene.videoWidth,
                    videoHeight = scene.videoHeight,
                    placeHolder = seekPreviewPlaceholder,
                )
            }
        }
    }
}

fun Modifier.offsetByPercent(
    xPercentage: Float,
    yOffset: Int,
) = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(
                x =
                    ((constraints.maxWidth * xPercentage).toInt() - placeable.width / 2)
                        .coerceIn(0, constraints.maxWidth - placeable.width),
                y = constraints.maxHeight - yOffset, // (constraints.maxHeight * yPercentage).toInt() - (placeable.height / 1.33f).toInt(),
            )
        }
    },
)

@Composable
fun SeekPreviewImage(
    imageLoaded: Boolean,
    previewImageUrl: String?,
    imageLoader: ImageLoader,
    duration: Long,
    seekProgress: Float,
    videoWidth: Int?,
    videoHeight: Int?,
    modifier: Modifier = Modifier,
    placeHolder: Painter? = null,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (imageLoaded &&
            previewImageUrl.isNotNullOrBlank() &&
            videoWidth != null &&
            videoHeight != null
        ) {
            val height = 160.dp
            val width = height * (videoWidth.toFloat() / videoHeight)
            val heightPx = with(LocalDensity.current) { height.toPx().toInt() }
            val widthPx = with(LocalDensity.current) { width.toPx().toInt() }

            AsyncImage(
                modifier =
                    Modifier
                        .width(width)
                        .height(height)
                        .background(Color.Black)
                        .border(1.5.dp, color = MaterialTheme.colorScheme.border),
                model =
                    ImageRequest
                        .Builder(context)
                        .data(previewImageUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .transformations(
                            CoilPreviewTransformation(
                                widthPx,
                                heightPx,
                                duration,
                                (duration * seekProgress).toLong(),
                            ),
                        ).build(),
                contentScale = ContentScale.None,
                imageLoader = imageLoader,
                contentDescription = null,
                placeholder = placeHolder,
            )
        }
        Text(
            text = (seekProgress * duration / 1000).toLong().seconds.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SceneMarkerBar(
    uiConfig: ComposeUiConfig,
    markers: List<BasicMarker>,
    player: PlayerControls,
    controllerViewState: ControllerViewState,
    onCardFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (markers.isNotEmpty()) {
        val firstFocus = remember { FocusRequester() }
        LazyRow(
            modifier =
                modifier
                    .focusRestorer { firstFocus },
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(markers, key = { _, item -> item.id }) { index, item ->
                val cardModifier =
                    if (index == 0) {
                        Modifier.focusRequester(firstFocus)
                    } else {
                        Modifier
                    }
                BasicMarkerCard(
                    marker = item,
                    onClick = {
                        player.seekTo(item.seconds.inWholeMilliseconds)
                        controllerViewState.hideControls()
                    },
                    uiConfig = uiConfig,
                    modifier =
                        cardModifier
                            .onFocusChanged {
//                                    Log.i(TAG, "Marker ${item.id} focused: ${it.isFocused}")
                                if (it.isFocused) {
                                    controllerViewState.pulseControls(Int.MAX_VALUE)
                                    onCardFocus.invoke()
                                } else {
                                    controllerViewState.pulseControls()
                                }
                            },
                )
            }
        }
    }
}

data class BasicMarker(
    val id: String,
    val title: String,
    val seconds: Duration,
    val imageUrl: String,
    val videoUrl: String?,
) {
    constructor(marker: MarkerData) : this(
        id = marker.id,
        title =
            marker.title.ifBlank {
                marker.primary_tag.slimTagData.name
            } + " - ${marker.seconds.toInt().toDuration(DurationUnit.SECONDS)}",
        seconds = marker.seconds.seconds,
        imageUrl = marker.screenshot,
        videoUrl = marker.stream,
    )
}

@Composable
fun BasicMarkerCard(
    marker: BasicMarker,
    onClick: () -> Unit,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    RootCard(
        item = marker,
        modifier =
            modifier
                .padding(0.dp)
                .width(DataType.MARKER.defaultCardWidth.dp / 2),
        onClick = onClick,
        longClicker = { _, _ -> },
        getFilterAndPosition = null,
        uiConfig = uiConfig,
        imageWidth = DataType.MARKER.defaultCardWidth.dp / 2,
        imageHeight = DataType.MARKER.defaultCardHeight.dp / 2,
        imageUrl = marker.imageUrl,
        defaultImageDrawableRes = R.drawable.default_scene,
        videoUrl = null,
        title = marker.title,
        subtitle = {},
        description = {},
        imageOverlay = {},
    )
}

@Preview(device = "spec:parent=tv_1080p", backgroundColor = 0xFF383535)
@Composable
private fun PlaybackOverlayPreview() {
    AppTheme {
        val controllerViewState = ControllerViewState(3000, true)
        controllerViewState.showControls(Int.MAX_VALUE)
        PlaybackOverlay(
            uiConfig =
                ComposeUiConfig(
                    ratingAsStars = true,
                    starPrecision = StarRatingPrecision.FULL,
                    showStudioAsText = true,
                    debugTextEnabled = true,
                    cardSettings =
                        CardUiSettings(
                            maxSearchResults = 25,
                            playVideoPreviews = true,
                            videoPreviewAudio = false,
                            columns = 5,
                            showRatings = true,
                            imageCrop = true,
                            videoDelay = 3000,
                        ),
                ),
            scene =
                Scene(
                    id = "id",
                    title = null, // "title",
                    date = null, // "2025-01-01",
                    streamUrl = "",
                    screenshotUrl = "",
                    streams = mapOf(),
                    spriteUrl = "",
                    duration = 600.2,
                    resumeTime = 0.0,
                    videoCodec = "h264",
                    videoWidth = 1920,
                    videoHeight = 1080,
                    audioCodec = "aac",
                    format = "mkv",
                    oCounter = 1,
                    captionUrl = "",
                    captions = listOf(),
                ),
            markers =
                List(1) {
                    BasicMarker(
                        id = "123",
                        title = "Title - 3m2s",
                        seconds = 182.seconds,
                        imageUrl = "",
                        videoUrl = null,
                    )
                },
            streamDecision =
                StreamDecision(
                    sceneId = "id",
                    transcodeDecision = TranscodeDecision.DirectPlay,
                    videoSupported = true,
                    audioSupported = true,
                    containerSupported = true,
                ),
            oCounter = 1,
            playerControls = FakePlayerControls,
            controllerViewState = controllerViewState,
            onPlaybackActionClick = {},
            onSeekBarChange = {},
            showDebugInfo = true,
            showPlay = true,
            previousEnabled = true,
            nextEnabled = true,
            seekEnabled = true,
            modifier =
                Modifier
                    .fillMaxSize(),
        )
    }
}
