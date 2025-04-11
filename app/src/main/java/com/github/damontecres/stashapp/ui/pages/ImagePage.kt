package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.preference.PreferenceManager
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.playback.maybeMuteAudio
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.Material3MainTheme
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.DotSeparatedRow
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.ItemsRow
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.StarRating
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.ui.components.playback.isDpad
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.listOfNotNullOrBlank
import com.github.damontecres.stashapp.util.readOnlyModeDisabled
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.formatBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

private const val TAG = "ImagePage"

class ImageDetailsViewModel : ViewModel() {
    private var server: StashServer? = null

    private val _slideshow = MutableLiveData(false)

    /**
     * Whether slideshow mode is on or off
     */
    val slideshow: LiveData<Boolean> = _slideshow
    private val _slideshowPaused = MutableLiveData(false)
    val slideshowPaused: LiveData<Boolean> = _slideshowPaused

    /**
     * Whether the slideshow is actively running meaning slideshow mode is ON and is currently NOT paused
     */
    val slideshowActive =
        slideshow
            .asFlow()
            .combine(slideshowPaused.asFlow()) { slideshow, paused ->
                slideshow && !paused
            }.asLiveData()

    var slideshowDelay by Delegates.notNull<Long>()

    val pager = MutableLiveData<ComposePager<ImageData>>()
    private var position = 0

    private val _image = MutableLiveData<ImageData>()
    val image: LiveData<ImageData> = _image

    val loadingState = MutableLiveData<ImageLoadingState>(ImageLoadingState.Loading)
    val tags = MutableLiveData<List<TagData>>(listOf())
    val performers = MutableLiveData<List<PerformerData>>(listOf())

    val rating100 = MutableLiveData(0)
    val oCount = MutableLiveData(0)

    fun init(
        server: StashServer,
        filterArgs: FilterArgs,
        startPosition: Int,
        slideshow: Boolean,
        slideshowDelay: Long,
    ): ImageDetailsViewModel {
        Log.v(TAG, "View model init")
        this.slideshowDelay = slideshowDelay
        if (pager.value?.filter != filterArgs || server != this.server) {
            if (filterArgs.dataType != DataType.IMAGE) {
                throw IllegalArgumentException("Cannot use ${filterArgs.dataType}")
            }
            this.server = server
            val dataSupplierFactory = DataSupplierFactory(server.version)
            val dataSupplier =
                dataSupplierFactory.create<Query.Data, ImageData, Query.Data>(filterArgs)
            val pagingSource =
                StashPagingSource(QueryEngine(server), dataSupplier) { _, _, item -> item }
            val pager = ComposePager(filterArgs, pagingSource, viewModelScope)
            Log.v(TAG, "Pager created")
            viewModelScope.launch {
                pager.init()
                Log.v(TAG, "Pager size: ${pager.size}")
                this@ImageDetailsViewModel.pager.value = pager
                this@ImageDetailsViewModel._slideshow.value = slideshow
                updatePosition(startPosition)
                if (slideshow) {
                    startSlideshow()
                    pulseSlideshow()
                }
            }
        }

        return this
    }

    fun nextImage(): Boolean {
        val size = pager.value?.size
        val newPosition = position + 1
        return if (size != null && newPosition < size) {
            updatePosition(newPosition)
            true
        } else {
            // TODO
            false
        }
    }

    fun previousImage(): Boolean {
        val newPosition = position - 1
        return if (newPosition >= 0) {
            updatePosition(newPosition)
            true
        } else {
            // TODO
            false
        }
    }

    fun updatePosition(position: Int) {
        pager.value?.let { pager ->
            viewModelScope.launch {
                try {
                    val image = pager.getBlocking(position)
                    Log.v(TAG, "Got image for $position: ${image != null}")
                    if (image != null) {
                        this@ImageDetailsViewModel.position = position
                        val queryEngine = QueryEngine(server!!)
                        rating100.value = image.rating100 ?: 0
                        oCount.value = image.o_counter ?: 0
                        tags.value = listOf()
                        performers.value = listOf()
                        _image.value = image

                        loadingState.value = ImageLoadingState.Success(image)
                        if (image.tags.isNotEmpty()) {
                            tags.value =
                                queryEngine.getTags(image.tags.map { it.id })
                            Log.v(TAG, "Got ${tags.value?.size} tags")
                        }
                        if (image.performers.isNotEmpty()) {
                            performers.value =
                                queryEngine.findPerformers(performerIds = image.performers.map { it.id })
                        }
                    } else {
                        loadingState.value = ImageLoadingState.Error
                    }
                } catch (ex: Exception) {
                    loadingState.value = ImageLoadingState.Error
                }
            }
        }
    }

    fun addTag(
        imageId: String,
        tagId: String,
    ) = mutateTags(imageId) { add(tagId) }

    fun removeTag(
        imageId: String,
        tagId: String,
    ) = mutateTags(imageId) { remove(tagId) }

    private fun mutateTags(
        imageId: String,
        mutator: MutableList<String>.() -> Unit,
    ) {
        val ids = tags.value?.map { it.id }
        ids?.let {
            val mutable = it.toMutableList()
            mutator.invoke(mutable)
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                val mutationEngine = MutationEngine(server!!)
                val result = mutationEngine.updateImage(imageId = imageId, tagIds = mutable)
                if (result != null) {
                    tags.value = result.tags.map { it.tagData }
                }
            }
        }
    }

    fun addPerformer(
        imageId: String,
        performerId: String,
    ) = mutatePerformers(imageId) { add(performerId) }

    fun removePerformer(
        imageId: String,
        performerId: String,
    ) = mutatePerformers(imageId) { remove(performerId) }

    private fun mutatePerformers(
        imageId: String,
        mutator: MutableList<String>.() -> Unit,
    ) {
        val perfs = performers.value?.map { it.id }
        perfs?.let {
            val mutable = it.toMutableList()
            mutator.invoke(mutable)
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                val mutationEngine = MutationEngine(server!!)
                val result = mutationEngine.updateImage(imageId = imageId, performerIds = mutable)
                if (result != null) {
                    performers.value = result.performers.map { it.performerData }
                }
            }
        }
    }

    fun updateRating(
        imageId: String,
        rating100: Int,
    ) {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val mutationEngine = MutationEngine(server!!)
            val newRating =
                mutationEngine.updateImage(imageId, rating100 = rating100)?.rating100 ?: 0
            this@ImageDetailsViewModel.rating100.value = newRating
            showSetRatingToast(StashApplication.getApplication(), newRating)
        }
    }

    fun updateOCount(action: suspend MutationEngine.(String) -> OCounter) {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val mutationEngine = MutationEngine(server!!)
            val newOCount = action.invoke(mutationEngine, _image.value!!.id)
            oCount.value = newOCount.count
        }
    }

    private var slideshowJob: Job? = null

    fun startSlideshow() {
        _slideshow.value = true
        _slideshowPaused.value = false
        if (_image.value?.isImageClip == false) {
            pulseSlideshow()
        }
    }

    fun stopSlideshow() {
        slideshowJob?.cancel()
        _slideshow.value = false
    }

    fun pauseSlideshow() {
        if (_slideshow.value == true) {
            _slideshowPaused.value = true
            slideshowJob?.cancel()
        }
    }

    fun unpauseSlideshow() {
        if (_slideshow.value == true) {
            _slideshowPaused.value = false
        }
    }

    fun pulseSlideshow() = pulseSlideshow(slideshowDelay)

    fun pulseSlideshow(milliseconds: Long) {
        Log.v(TAG, "pulseSlideshow")
        slideshowJob?.cancel()
        if (slideshow.value!!) {
            slideshowJob =
                viewModelScope
                    .launch(StashCoroutineExceptionHandler()) {
                        delay(milliseconds)
                        Log.v(TAG, "pulseSlideshow after delay")
                        if (slideshowActive.value == true) {
                            nextImage()
                        }
                    }.apply {
                        invokeOnCompletion { if (it !is CancellationException) pulseSlideshow() }
                    }
        }
    }

    companion object {
//        val SERVER_KEY = object : CreationExtras.Key<StashServer> {}
//        val FILTER_KEY = object : CreationExtras.Key<FilterArgs> {}
//        val POSITION_KEY = object : CreationExtras.Key<Int> {}
//        val Factory: ViewModelProvider.Factory =
//            viewModelFactory {
//                initializer {
//                    val server = this[SERVER_KEY]!!
//                    val filter = this[FILTER_KEY]!!
//                    val position = this[POSITION_KEY]!!
//                    ImageDetailsViewModel(server, filter, position).init()
//                }
//            }
    }
}

interface SlideshowControls {
    fun startSlideshow()

    fun stopSlideshow()
}

sealed class ImageLoadingState {
    data object Loading : ImageLoadingState()

    data object Error : ImageLoadingState()

    data class Success(
        val image: ImageData,
    ) : ImageLoadingState()
}

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
        maybeMuteAudio(context, player)
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
                    AsyncImage(
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ImageOverlay(
    server: StashServer,
    player: Player,
    slideshowControls: SlideshowControls,
    slideshowEnabled: Boolean,
    image: ImageData,
    tags: List<TagData>,
    performers: List<PerformerData>,
    rating100: Int,
    oCount: Int,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    oCountAction: (action: suspend MutationEngine.(String) -> OCounter) -> Unit,
    onRatingChange: (Int) -> Unit,
    longClicker: LongClicker<Any>,
    addItem: (item: StashData) -> Unit,
    removeItem: (item: StashData) -> Unit,
    onZoom: (Float) -> Unit,
    onRotate: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
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
                            if (readOnlyModeDisabled()) {
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

    val startStopSlideshow =
        DialogItem(
            headlineContent = {
                Text(
                    text =
                        if (slideshowEnabled) {
                            stringResource(R.string.stop_slideshow)
                        } else {
                            stringResource(R.string.play_slideshow)
                        },
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(if (slideshowEnabled) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24),
                    contentDescription = null,
                )
            },
            onClick = { if (slideshowEnabled) slideshowControls.stopSlideshow() else slideshowControls.startSlideshow() },
        )

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 135.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            ImageDetailsHeader(
                player = player,
                image = image,
                rating100 = rating100,
                oCount = oCount,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                moreOnClick = {
                    showDialog =
                        DialogParams(
                            title = context.getString(R.string.more) + "...",
                            fromLongClick = false,
                            items =
                                buildList {
                                    add(startStopSlideshow)
                                    if (readOnlyModeDisabled()) {
                                        add(
                                            DialogItem(
                                                context.getString(R.string.add_performer),
                                                DataType.PERFORMER.iconStringId,
                                            ) {
                                                searchForDataType =
                                                    SearchForParams(DataType.PERFORMER)
                                            },
                                        )
                                        add(
                                            DialogItem(
                                                context.getString(R.string.add_tag),
                                                DataType.TAG.iconStringId,
                                            ) {
                                                searchForDataType = SearchForParams(DataType.TAG)
                                            },
                                        )
                                    }
                                },
                        )
                },
                oCounterOnClick = { oCountAction.invoke(MutationEngine::incrementImageOCounter) },
                oCounterOnLongClick = {
                    showDialog =
                        DialogParams(
                            title = context.getString(R.string.stashapp_o_counter),
                            fromLongClick = true,
                            items =
                                listOf(
                                    DialogItem(context.getString(R.string.increment)) {
                                        oCountAction.invoke(MutationEngine::incrementImageOCounter)
                                    },
                                    DialogItem(context.getString(R.string.decrement)) {
                                        oCountAction.invoke(MutationEngine::decrementImageOCounter)
                                    },
                                    DialogItem(context.getString(R.string.reset)) {
                                        oCountAction.invoke(MutationEngine::resetImageOCounter)
                                    },
                                ),
                        )
                },
                onRatingChange = onRatingChange,
                onZoom = onZoom,
                onRotate = onRotate,
                onReset = onReset,
                modifier = Modifier,
            )
        }
        val startPadding = 24.dp
        val bottomPadding = 16.dp

        if (performers.isNotEmpty()) {
            item {
                ItemsRow(
                    title = stringResource(R.string.stashapp_performers),
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
                    title = stringResource(R.string.stashapp_tags),
                    items = tags,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = removeLongClicker,
                    modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
                )
            }
        }
        item {
            ImageDetailsFooter(
                image,
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
                            addItem.invoke(item)
                        },
                        uiConfig = uiConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageDetailsHeader(
    player: Player,
    image: ImageData,
    rating100: Int,
    oCount: Int,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    moreOnClick: () -> Unit,
    oCounterOnClick: () -> Unit,
    oCounterOnLongClick: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onZoom: (Float) -> Unit,
    onRotate: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Column(
        modifier =
            modifier
                .fillMaxWidth(0.8f)
                .height(440.dp)
                .bringIntoViewRequester(bringIntoViewRequester),
    ) {
        // Title
        Text(
            text = image.titleOrFilename ?: "",
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
            StarRating(
                rating100 = rating100,
                precision = uiConfig.starPrecision,
                onRatingChange = onRatingChange,
                enabled = true,
                modifier =
                    Modifier
                        .height(30.dp),
            )
            // Quick info
            val imageFile = image.visual_files.firstOrNull()?.onImageFile
            val videoFile = image.visual_files.firstOrNull()?.onVideoFile

            val imageRes = imageFile?.let { "${it.width}x${it.height}" }

            DotSeparatedRow(
                modifier = Modifier.padding(top = 6.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                texts =
                    listOfNotNullOrBlank(
                        image.date,
                        imageRes,
                        videoFile?.let { durationToString(it.duration) },
                        videoFile?.resolutionName(),
                    ),
            )
            // Description
            if (image.details.isNotNullOrBlank()) {
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
                        text = image.details,
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
            }
            // Key-Values
            Row(
                modifier =
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (image.studio != null) {
                    TitleValueText(
                        stringResource(R.string.stashapp_studio),
                        image.studio.name,
                    )
                }
                if (image.code.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_scene_code),
                        image.code,
                    )
                }
                if (image.photographer.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_director),
                        image.photographer,
                    )
                }
            }
        }
        ImageControlsOverlay(
            player = player,
            isImageClip = image.isImageClip,
            oCount = oCount,
            bringIntoViewRequester = bringIntoViewRequester,
            onZoom = onZoom,
            onRotate = onRotate,
            onReset = onReset,
            moreOnClick = moreOnClick,
            oCounterOnClick = oCounterOnClick,
            oCounterOnLongClick = oCounterOnLongClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageControlsOverlay(
    player: Player,
    isImageClip: Boolean,
    oCount: Int,
    onZoom: (Float) -> Unit,
    onRotate: (Int) -> Unit,
    onReset: () -> Unit,
    moreOnClick: () -> Unit,
    oCounterOnClick: () -> Unit,
    oCounterOnLongClick: () -> Unit,
    bringIntoViewRequester: BringIntoViewRequester?,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.tryRequestFocus()
    }
    val onFocused = { focusState: FocusState ->
        if (focusState.isFocused && bringIntoViewRequester != null) {
            scope.launch { bringIntoViewRequester.bringIntoView() }
        }
    }
    val playPauseState = rememberPlayPauseButtonState(player)
    LazyRow(
        modifier =
            modifier
                .focusGroup()
                .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isImageClip) {
            item {
                Button(
                    onClick = playPauseState::onClick,
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged(onFocused),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        painter =
                            painterResource(
                                if (playPauseState.showPlay) R.drawable.baseline_play_arrow_24 else R.drawable.baseline_pause_24,
                            ),
                        contentDescription = null,
                    )
                }
            }
        } else {
            // Regular image
            // TODO might be able to apply to the player surface?
            // If enabling these, make sure the focusRequester is updated!
            item {
                ImageControlButton(
                    stringRes = R.string.fa_rotate_left,
                    onClick = { onRotate(-90) },
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged(onFocused),
                )
            }
            item {
                ImageControlButton(
                    stringRes = R.string.fa_rotate_right,
                    onClick = { onRotate(90) },
                    modifier =
                        Modifier
                            .onFocusChanged(onFocused),
                )
            }
            item {
                ImageControlButton(
                    stringRes = R.string.fa_magnifying_glass_plus,
                    onClick = { onZoom(.15f) },
                    modifier =
                        Modifier
                            .onFocusChanged(onFocused),
                )
            }
            item {
                ImageControlButton(
                    stringRes = R.string.fa_magnifying_glass_minus,
                    onClick = { onZoom(-.15f) },
                    modifier =
                        Modifier
                            .onFocusChanged(onFocused),
                )
            }
            item {
                ImageControlButton(
                    drawableRes = R.drawable.baseline_undo_24,
                    onClick = onReset,
                    modifier =
                        Modifier
                            .onFocusChanged(onFocused),
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
                        .onFocusChanged(onFocused),
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
                        .onFocusChanged(onFocused),
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
fun ImageControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes stringRes: Int = 0,
    @DrawableRes drawableRes: Int = 0,
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(8.dp),
        modifier = modifier,
    ) {
        if (stringRes != 0) {
            Text(
                text = stringResource(stringRes),
                fontFamily = FontAwesome,
                style = MaterialTheme.typography.titleLarge,
            )
        } else {
            Icon(
                painter = painterResource(drawableRes),
                contentDescription = "",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun ImageDetailsFooter(
    image: ImageData,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TitleValueText(stringResource(R.string.id), image.id)
        if (image.created_at.toString().length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_created_at),
                image.created_at.toString().substring(0..<10),
            )
        }
        if (image.updated_at.toString().length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_updated_at),
                image.updated_at.toString().substring(0..<10),
            )
        }
        val baseFile = image.visual_files.firstOrNull()?.onBaseFile
//        val imageFile = image.visual_files.firstOrNull()?.onImageFile
        val videoFile = image.visual_files.firstOrNull()?.onVideoFile
        baseFile?.let {
            TitleValueText(
                stringResource(R.string.stashapp_filesize),
                it.size
                    .toString()
                    .toIntOrNull()
                    ?.let { bytes -> formatBytes(bytes) } ?: it.size.toString(),
            )
        }
        videoFile?.let {
            TitleValueText(
                stringResource(R.string.stashapp_video_codec),
                it.video_codec,
            )
            TitleValueText(
                stringResource(R.string.stashapp_audio_codec),
                it.audio_codec,
            )
            TitleValueText(
                stringResource(R.string.format),
                it.format,
            )
        }
    }
}

// @OptIn(ExperimentalFoundationApi::class)
// @Preview(widthDp = 600)
// @Composable
// private fun ImageControlsOverlayPreview() {
//    MainTheme {
//        ImageControlsOverlay(
//            player = PreviewFakePlayer(),
//            isImageClip = false,
//            onZoom = {},
//            onRotate = {},
//            onReset = {},
//            bringIntoViewRequester = null,
//            oCount = 1,
//            moreOnClick = {},
//            oCounterOnClick = {},
//            oCounterOnLongClick = {},
//            modifier = Modifier.fillMaxWidth(),
//        )
//    }
// }
