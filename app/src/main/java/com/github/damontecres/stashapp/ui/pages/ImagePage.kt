package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.playback.maybeMuteAudio
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.image.ImageDetailsViewModel
import com.github.damontecres.stashapp.ui.components.image.ImageOverlay
import com.github.damontecres.stashapp.ui.components.image.SlideshowControls
import com.github.damontecres.stashapp.ui.components.playback.isDpad
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.isNotNullOrBlank

private const val TAG = "ImagePage"

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
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
    LaunchedEffect(server, filter) {
        val slideshowDelay =
            PreferenceManager.getDefaultSharedPreferences(context).getInt(
                StashApplication.getApplication().getString(R.string.pref_key_slideshow_duration),
                StashApplication.getApplication().resources.getInteger(R.integer.pref_key_slideshow_duration_default),
            ) * 1000L

        viewModel.init(server, filter, startPosition, startSlideshow, slideshowDelay)
    }

    val imageState by viewModel.image.observeAsState()
    val tags by viewModel.tags.observeAsState(listOf())
    val performers by viewModel.performers.observeAsState(listOf())
    val rating100 by viewModel.rating100.observeAsState(0)
    val oCount by viewModel.oCount.observeAsState(0)

    var zoomFactor by rememberSaveable { mutableFloatStateOf(1f) }
    var rotation by rememberSaveable { mutableIntStateOf(0) }
    var showOverlay by rememberSaveable { mutableStateOf(false) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }

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
        targetValue = rotation.toFloat(),
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

    val slideshowEnabled by viewModel.slideshow.observeAsState(false)
    val slideshowActive by viewModel.slideshowActive.observeAsState(false)

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.tryRequestFocus()
    }

    val density = LocalDensity.current

    fun reset(resetRotate: Boolean) {
        zoomFactor = 1f
        panX = 0f
        panY = 0f
        if (resetRotate) rotation = 0
    }

    LaunchedEffect(imageState) {
        reset(true)
    }
    val player =
        remember {
            StashExoPlayer.getInstance(context, server).apply {
                maybeMuteAudio(context, false, this)
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
        }
    LifecycleStartEffect(Unit) {
        onStopOrDispose {
            StashExoPlayer.releasePlayer()
        }
    }

    val playSlideshowDelay =
        remember {
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .getInt(
                    context.getString(R.string.pref_key_slideshow_duration_image_clip),
                    context.resources.getInteger(R.integer.pref_key_slideshow_duration_default_image_clip),
                ).toLong()
        }
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

    Box(
        modifier =
            modifier
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent {
                    var result = false
                    if (it.type != KeyEventType.KeyUp) {
                        result = false
                    } else if (!showOverlay && zoomFactor * 100 > 105 && isDpad(it)) {
                        // Image is zoomed in
                        when (it.key) {
                            Key.DirectionLeft -> panX += with(density) { 30.dp.toPx() }
                            Key.DirectionRight -> panX -= with(density) { 30.dp.toPx() }
                            Key.DirectionUp -> panY += with(density) { 30.dp.toPx() }
                            Key.DirectionDown -> panY -= with(density) { 30.dp.toPx() }
                        }
                        result = true
                    } else if (!showOverlay && zoomFactor * 100 > 105 && it.key == Key.Back) {
                        reset(false)
                        result = true
                    } else if (!showOverlay && (it.key == Key.DirectionLeft || it.key == Key.DirectionRight)) {
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
                    } else if (showOverlay && it.key == Key.Back) {
                        showOverlay = false
                        viewModel.unpauseSlideshow()
                        result = true
                    } else if (!showOverlay && it.key != Key.Back) {
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
                        Modifier.resizeWithContentScale(contentScale, presentationState.videoSizeDp)
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
                    SubcomposeAsyncImage(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomAnimation
                                    scaleY = zoomAnimation
                                    translationX = panXAnimation
                                    translationY = panYAnimation
                                }.rotate(rotateAnimation),
                        model =
                            ImageRequest
                                .Builder(LocalContext.current)
                                .data(image.paths.image)
                                .crossfade(true)
                                .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight,
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
                            CircularProgress(
                                Modifier
                                    .size(120.dp)
                                    .align(Alignment.Center),
                                false,
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
            AnimatedVisibility(showOverlay) {
                ImageOverlay(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(AppColors.TransparentBlack50),
                    server = server,
                    player = player,
                    slideshowControls = slideshowControls,
                    slideshowEnabled = slideshowEnabled,
                    image = image,
                    tags = tags,
                    performers = performers,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    onZoom = { zoomFactor = (zoomFactor + it).coerceIn(1f, 5f) },
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
                        when (item) {
                            is TagData -> viewModel.removeTag(image.id, item.id)
                            is PerformerData -> viewModel.removePerformer(image.id, item.id)
                            else -> {}
                        }
                    },
                )
            }
        }
    }
}
