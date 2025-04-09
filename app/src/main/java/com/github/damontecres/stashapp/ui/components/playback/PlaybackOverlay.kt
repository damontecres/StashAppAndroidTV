package com.github.damontecres.stashapp.ui.components.playback

import android.util.Log
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
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
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.cards.RootCard
import com.github.damontecres.stashapp.ui.util.CoilPreviewTransformation
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.toLongMilliseconds
import com.github.damontecres.stashapp.views.formatDate
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
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

@Composable
fun PlaybackOverlay(
    server: StashServer,
    uiConfig: ComposeUiConfig,
    scene: Scene,
    markers: List<MarkerData>,
    streamDecision: StreamDecision?,
    oCounter: Int,
    player: Player,
    controllerViewState: ControllerViewState,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    showDebugInfo: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var seekProgress by remember { mutableFloatStateOf(-1f) }
    val seekBarInteractionSource = remember { MutableInteractionSource() }
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
            LazyColumn(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(controlHeight),
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
                        player = player,
                        onPlaybackActionClick = onPlaybackActionClick,
                        controllerViewState = controllerViewState,
                        showDebugInfo = showDebugInfo,
                        onSeekProgress = {
                            seekProgress = it
                        },
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
                                    .padding(start = 8.dp, top = 48.dp, bottom = 64.dp)
                                    .fillMaxWidth(),
                            server = server,
                            markers = markers,
                            player = player,
                            controllerViewState = controllerViewState,
                            uiConfig = uiConfig,
                        )
                    }
                }
            }
            AnimatedVisibility(seekBarFocused && seekProgress >= 0) {
                SeekPreviewImage(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .offsetByPercent(
                                xPercentage = seekProgress.coerceIn(0f, 1f),
                                yPercentage = 1 - controlHeight,
                            ),
                    previewImageUrl = previewImageUrl,
                    imageLoaded = imageLoaded,
                    imageLoader = imageLoader,
                    duration = player.duration,
                    seekProgress = seekProgress,
                    videoWidth = scene.videoWidth,
                    videoHeight = scene.videoHeight,
                )
            }
        }
    }
}

fun Modifier.offsetByPercent(
    xPercentage: Float,
    yPercentage: Float,
) = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(
                x =
                    ((constraints.maxWidth * xPercentage).toInt() - placeable.width / 2)
                        .coerceIn(0, constraints.maxWidth - placeable.width),
                y = (constraints.maxHeight * yPercentage).toInt() - (placeable.height / 1.33f).toInt(),
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
    server: StashServer,
    uiConfig: ComposeUiConfig,
    markers: List<MarkerData>,
    player: Player,
    controllerViewState: ControllerViewState,
    modifier: Modifier = Modifier,
) {
    if (markers.isNotEmpty()) {
        val context = LocalContext.current
        val firstFocus = remember { FocusRequester() }
        Column(
            modifier = modifier,
        ) {
            LazyRow(
                modifier =
                    Modifier
                        .padding(top = 8.dp)
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
                            player.seekTo(item.seconds.toLongMilliseconds)
                            controllerViewState.hideControls()
                        },
                        uiConfig = uiConfig,
                        modifier =
                            cardModifier
                                .onFocusChanged {
                                    Log.i(TAG, "Marker ${item.id} focused: ${it.isFocused}")
                                    if (it.isFocused) {
                                        controllerViewState.pulseControls(Int.MAX_VALUE)
                                    } else {
                                        controllerViewState.pulseControls()
                                    }
                                },
                    )
                }
            }
        }
    }
}

@Composable
fun BasicMarkerCard(
    marker: MarkerData,
    onClick: () -> Unit,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    val title =
        marker.title.ifBlank {
            marker.primary_tag.slimTagData.name
        } + " - ${marker.seconds.toInt().toDuration(DurationUnit.SECONDS)}"

    val imageUrl = marker.screenshot
    val videoUrl = null // marker.stream

    RootCard(
        item = marker,
        modifier =
            modifier
                .padding(0.dp)
                .width(MarkerPresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        longClicker = { _, _ -> },
        getFilterAndPosition = null,
        uiConfig = uiConfig,
        imageWidth = MarkerPresenter.CARD_WIDTH.dp / 2,
        imageHeight = MarkerPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        defaultImageDrawableRes = R.drawable.default_scene,
        videoUrl = videoUrl,
        title = title,
        subtitle = {},
        description = {},
        imageOverlay = {},
    )
}
