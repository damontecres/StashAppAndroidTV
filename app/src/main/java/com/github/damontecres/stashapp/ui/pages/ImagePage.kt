package com.github.damontecres.stashapp.ui.pages

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.VideoFilter
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.playback.maybeMuteAudio
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.compat.isNotTvDevice
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.image.DRAG_THROTTLE_DELAY
import com.github.damontecres.stashapp.ui.components.image.ImageDetailsViewModel
import com.github.damontecres.stashapp.ui.components.image.ImageFilterDialog
import com.github.damontecres.stashapp.ui.components.image.ImageLoadingPlaceholder
import com.github.damontecres.stashapp.ui.components.image.ImageOverlay
import com.github.damontecres.stashapp.ui.components.image.SlideshowControls
import com.github.damontecres.stashapp.ui.components.playback.isDirectionalDpad
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.maxFileSize
import kotlin.math.abs

private const val TAG = "ImagePage"
private const val DEBUG = false

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(UnstableApi::class)
@Composable
fun ImagePage(
    server: StashServer,
    navigationManager: NavigationManagerCompose,
    filter: FilterArgs,
    startPosition: Int,
    startSlideshow: Boolean,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
    viewModel: ImageDetailsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val isNotTvDevice = isNotTvDevice
    LaunchedEffect(server, filter) {
        val slideshowDelay = uiConfig.preferences.interfacePreferences.slideShowIntervalMs

        viewModel.init(
            server,
            filter,
            startPosition,
            startSlideshow,
            slideshowDelay,
            uiConfig.persistVideoFilters,
        )
        if (isNotTvDevice) {
            // Reduce the throttling for touch devices since a delay when dragging feels like lag
            viewModel.imageFilter.startThrottling(DRAG_THROTTLE_DELAY)
        }
    }

    val imageState by viewModel.image.observeAsState()
    val tags by viewModel.tags.observeAsState(listOf())
    val performers by viewModel.performers.observeAsState(listOf())
    val galleries by viewModel.galleries.observeAsState(listOf())
    val rating100 by viewModel.rating100.observeAsState(0)
    val oCount by viewModel.oCount.observeAsState(0)
    val imageFilter by viewModel.imageFilter.observeAsState(VideoFilter())
    val position by viewModel.position.observeAsState(0)
    val pager by viewModel.pager.observeAsState()

    var zoomFactor by rememberSaveable { mutableFloatStateOf(1f) }
    val isZoomed = zoomFactor * 100 > 102
    var rotation by rememberSaveable { mutableFloatStateOf(0f) }
    var showOverlay by rememberSaveable { mutableStateOf(false) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    val galleryId by viewModel.galleryId.observeAsState(null)

    val slideshowControls =
        object : SlideshowControls {
            override fun startSlideshow() {
                showOverlay = false
                viewModel.startSlideshow()
            }

            override fun stopSlideshow() {
                viewModel.stopSlideshow()
            }
        }

    val rotateAnimation: Float by animateFloatAsState(
        targetValue = rotation,
        label = "image_rotation",
    )
    val zoomAnimation: Float by animateFloatAsState(
        targetValue = zoomFactor,
        label = "image_zoom",
    )
    val panXAnimation: Float by animateFloatAsState(
        targetValue = panX,
        label = "image_panX",
    )
    val panYAnimation: Float by animateFloatAsState(
        targetValue = panY,
        label = "image_panY",
    )

    val state =
        rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            zoomFactor *= zoomChange
            rotation += rotationChange
            panX += offsetChange.x
            panY += offsetChange.y
        }

    val slideshowEnabled by viewModel.slideshow.observeAsState(false)
    val slideshowActive by viewModel.slideshowActive.observeAsState(false)

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.tryRequestFocus()
    }

    val density = LocalDensity.current
    val screenHeight = LocalWindowInfo.current.containerSize.height
    val screenWidth = LocalWindowInfo.current.containerSize.width

    val maxPanX = screenWidth * .75f
    val maxPanY = screenHeight * .75f

    fun reset(resetRotate: Boolean) {
        zoomFactor = 1f
        panX = 0f
        panY = 0f
        if (resetRotate) rotation = 0f
    }

    fun pan(
        xFactor: Int,
        yFactor: Int,
    ) {
        if (xFactor != 0) {
            panX = (panX + with(density) { xFactor.dp.toPx() }).coerceIn(-maxPanX, maxPanX)
        }
        if (yFactor != 0) {
            panY = (panY + with(density) { yFactor.dp.toPx() }).coerceIn(-maxPanY, maxPanY)
        }
    }

    fun zoom(factor: Float) {
        if (factor < 0) {
            val diffFactor = factor / (zoomFactor - 1f)
            // zooming out
            val panXDiff = abs(panX * diffFactor)
            val panYDiff = abs(panY * diffFactor)
            if (DEBUG) {
                Log.d(
                    TAG,
                    "zoomFactor=$zoomFactor, factor=$factor, panX=$panX, panY=$panY, panXDiff=$panXDiff, panYDiff=$panYDiff",
                )
            }
            if (panX > 0f) {
                panX -= panXDiff
            } else if (panX < 0f) {
                panX += panXDiff
            }
            if (panY > 0f) {
                panY -= panYDiff
            } else if (panY < 0f) {
                panY += panYDiff
            }
        }
        zoomFactor = (zoomFactor + factor).coerceIn(1f, 5f)
        if (!isZoomed) {
            // Always reset if not zoomed
            panX = 0f
            panY = 0f
        }
    }

    LaunchedEffect(imageState) {
        reset(true)
    }
    val player =
        remember {
            StashExoPlayer.getInstance(context, server).apply {
                maybeMuteAudio(uiConfig.preferences, false, this)
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
        }
    LifecycleStartEffect(Unit) {
        onStopOrDispose {
            StashExoPlayer.releasePlayer()
        }
    }

    val playSlideshowDelay = uiConfig.preferences.interfacePreferences.slideShowIntervalMs
    val presentationState = rememberPresentationState(player)
    LaunchedEffect(player) {
        StashExoPlayer.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        viewModel.pulseSlideshow(playSlideshowDelay)
                    }
                }
            },
        )
    }
    LaunchedEffect(slideshowActive) {
        player.repeatMode = if (slideshowEnabled) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
    }

    var longPressing by remember { mutableStateOf(false) }

    var dragXAmount by remember { mutableFloatStateOf(0f) }

    // TODO move content into a function
    val contentModifier =
        Modifier.ifElse(
            isNotTvDevice,
            Modifier
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = {
                        showOverlay = !showOverlay
                    },
                ).pointerInput(isZoomed) {
                    detectTapGestures(
                        onTap = {
                            showOverlay = !showOverlay
                        },
                        onDoubleTap = {
                            if (!showOverlay) {
                                if (isZoomed) {
                                    reset(false)
                                } else {
                                    zoom(1.5f)
                                }
                            }
                        },
                    )
                }.ifElse(
                    condition = isZoomed || showOverlay,
                    Modifier
                        .transformable(
                            state = state,
                            enabled = !showOverlay,
                            lockRotationOnZoomPan = true,
                        ),
                    Modifier
                        .transformable(state, lockRotationOnZoomPan = true)
                        .pointerInput(Unit) {
                            // TODO use https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling#swiping
                            detectDragGestures(
                                onDragStart = { dragXAmount = 0f },
                                onDragCancel = { dragXAmount = 0f },
                                onDragEnd = {
                                    if (dragXAmount > 300f) {
                                        viewModel.previousImage()
                                    } else if (dragXAmount < -300f) {
                                        viewModel.nextImage()
                                    }
                                    dragXAmount = 0f
                                },
                            ) { change, dragAmount ->
                                dragXAmount += dragAmount.x
                                change.consume()
                            }
                        },
                ),
        )

    Box(
        modifier =
            modifier
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent {
                    val isOverlayShowing = showOverlay || showFilterDialog
                    var result = false
                    if (!isOverlayShowing) {
                        if (longPressing && it.type == KeyEventType.KeyUp) {
                            // User stopped long pressing, so cancel the zooming action, but still consume the event so it doesn't move the image
                            longPressing = false
                            return@onKeyEvent true
                        }
                        longPressing =
                            it.nativeKeyEvent.isLongPress ||
                            it.nativeKeyEvent.repeatCount > 0
                        if (longPressing) {
                            when (it.key) {
                                Key.DirectionUp -> zoom(.05f)
                                Key.DirectionDown -> zoom(-.05f)

                                // These work, but feel awkward because Up/Down zoom, so you can't long press them to pan
                                // Key.DirectionLeft -> panX += with(density) { 15.dp.toPx() }
                                // Key.DirectionRight -> panX -= with(density) { 15.dp.toPx() }
                            }
                            return@onKeyEvent true
                        }
                    }
                    if (it.type != KeyEventType.KeyUp) {
                        result = false
                    } else if (!isOverlayShowing && isZoomed && isDirectionalDpad(it)) {
                        // Image is zoomed in
                        when (it.key) {
                            Key.DirectionLeft -> pan(30, 0)
                            Key.DirectionRight -> pan(-30, 0)
                            Key.DirectionUp -> pan(0, 30)
                            Key.DirectionDown -> pan(0, -30)
                        }
                        result = true
                    } else if (!isOverlayShowing && isZoomed && it.key == Key.Back) {
                        reset(false)
                        result = true
                    } else if (!isOverlayShowing && (it.key == Key.DirectionLeft || it.key == Key.DirectionRight)) {
                        when (it.key) {
                            Key.DirectionLeft, Key.DirectionUpLeft, Key.DirectionDownLeft -> {
                                if (!viewModel.previousImage()) {
                                    Toast
                                        .makeText(
                                            context,
                                            R.string.slideshow_at_beginning,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            }

                            Key.DirectionRight, Key.DirectionUpRight, Key.DirectionDownRight -> {
                                if (!viewModel.nextImage()) {
                                    Toast
                                        .makeText(
                                            context,
                                            R.string.no_more_images,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            }
                        }
                    } else if (isOverlayShowing && it.key == Key.Back) {
                        showOverlay = false
                        viewModel.unpauseSlideshow()
                        result = true
                    } else if (!isOverlayShowing && it.key != Key.Back) {
                        showOverlay = true
                        viewModel.pauseSlideshow()
                        result = true
                    }
                    if (result) {
                        // Handled the key, so reset the slideshow timer
                        viewModel.pulseSlideshow()
                    }
                    result
                },
    ) {
        imageState?.let { image ->
            if (image.paths.image.isNotNullOrBlank()) {
                if (image.isImageClip) {
                    LaunchedEffect(image.id) {
                        val mediaItem =
                            MediaItem
                                .Builder()
                                .setUri(image.paths.image)
                                .build()
                        player.setMediaItem(mediaItem)
                        player.repeatMode =
                            if (slideshowEnabled) {
                                Player.REPEAT_MODE_OFF
                            } else {
                                Player.REPEAT_MODE_ONE
                            }
                        player.prepare()
                        player.play()
                        viewModel.pulseSlideshow(Long.MAX_VALUE)
                    }
                    LifecycleStartEffect(Unit) {
                        onStopOrDispose {
                            player.stop()
                        }
                    }
                    val contentScale = ContentScale.Fit
                    val scaledModifier =
                        contentModifier.resizeWithContentScale(
                            contentScale,
                            presentationState.videoSizeDp,
                        )
                    PlayerSurface(
                        player = player,
                        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                        modifier =
                            scaledModifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomAnimation
                                    scaleY = zoomAnimation
                                    translationX = panXAnimation
                                    translationY = panYAnimation
                                }.rotate(rotateAnimation),
                    )
                    if (presentationState.coverSurface) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(Color.Black),
                        )
                    }
                } else {
                    val colorFilter =
                        remember(image.id, imageFilter) {
                            if (imageFilter.hasImageFilter()) {
                                ColorMatrixColorFilter(imageFilter.createComposeColorMatrix())
                            } else {
                                null
                            }
                        }
                    // If the image loading is large, show the thumbnail while waiting
                    val showLoadingThumbnail =
                        image.paths.thumbnail.isNotNullOrBlank() && image.maxFileSize > 1024 * 1024
                    SubcomposeAsyncImage(
                        modifier =
                            contentModifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomAnimation
                                    scaleY = zoomAnimation
                                    translationX = panXAnimation
                                    translationY = panYAnimation

                                    val xTransform =
                                        (screenWidth - panXAnimation) / (screenWidth * 2)
                                    val yTransform =
                                        (screenHeight - panYAnimation) / (screenHeight * 2)
                                    if (DEBUG) {
                                        Log.d(
                                            TAG,
                                            "graphicsLayer: xTransform=$xTransform, yTransform=$yTransform",
                                        )
                                    }

                                    transformOrigin = TransformOrigin(xTransform, yTransform)
                                }.rotate(rotateAnimation),
                        model =
                            ImageRequest
                                .Builder(LocalContext.current)
                                .data(image.paths.image)
                                .size(Size.ORIGINAL)
                                .crossfade(!showLoadingThumbnail)
                                .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter,
                        error = {
                            Text(
                                modifier =
                                    Modifier
                                        .align(Alignment.Center),
                                text = "Error loading image",
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        },
                        loading = {
                            ImageLoadingPlaceholder(
                                thumbnailUrl = image.paths.thumbnail,
                                showThumbnail = showLoadingThumbnail,
                                colorFilter = colorFilter,
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                        // Ensure that if an image takes a long time to load, it won't be skipped
                        onLoading = {
                            viewModel.pulseSlideshow(Long.MAX_VALUE)
                        },
                        onSuccess = {
                            viewModel.pulseSlideshow()
                        },
                        onError = {
                            Log.e(TAG, "Error loading image ${image.id}", it.result.throwable)
                            Toast
                                .makeText(
                                    context,
                                    "Error loading image: ${it.result.throwable.localizedMessage}",
                                    Toast.LENGTH_LONG,
                                ).show()
                            viewModel.pulseSlideshow()
                        },
                    )
                }
            } else {
                // TODO
                Text("No image URL")
            }
            val focusManager = LocalFocusManager.current
            AnimatedVisibility(
                showOverlay,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                ImageOverlay(
                    modifier =
                        contentModifier
                            .fillMaxSize()
                            .background(AppColors.TransparentBlack50),
                    server = server,
                    player = player,
                    slideshowControls = slideshowControls,
                    slideshowEnabled = slideshowEnabled,
                    image = image,
                    tags = tags,
                    performers = performers,
                    galleries = galleries,
                    position = position,
                    count = pager?.size ?: -1,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    onZoom = ::zoom,
                    onRotate = { rotation += it },
                    onReset = { reset(true) },
                    rating100 = rating100,
                    oCount = oCount,
                    uiConfig = uiConfig,
                    oCountAction = viewModel::updateOCount,
                    onRatingChange = { viewModel.updateRating(image.id, it) },
                    addItem = { item ->
                        when (item) {
                            is TagData -> viewModel.addTag(image.id, item.id)
                            is PerformerData -> viewModel.addPerformer(image.id, item.id)
                            else -> {}
                        }
                    },
                    removeItem = { item ->
                        focusManager.moveFocus(FocusDirection.Previous)
                        when (item) {
                            is TagData -> viewModel.removeTag(image.id, item.id)
                            is PerformerData -> viewModel.removePerformer(image.id, item.id)
                            else -> {}
                        }
                    },
                    onShowFilterDialogClick = {
                        showFilterDialog = true
                        showOverlay = false
                        viewModel.pauseSlideshow()
                    },
                )
            }
            AnimatedVisibility(showFilterDialog) {
                ImageFilterDialog(
                    filter = imageFilter,
                    showVideoOptions = false,
                    showSaveGalleryButton = galleryId != null,
                    uiConfig = uiConfig,
                    onChange = viewModel::updateImageFilter,
                    onClickSave = viewModel::saveImageFilter,
                    onClickSaveGallery = viewModel::saveGalleryFilter,
                    onDismissRequest = {
                        showFilterDialog = false
                        viewModel.unpauseSlideshow()
                        viewModel.pulseSlideshow()
                    },
                )
            }
        }
    }
}
