package com.github.damontecres.stashapp.ui.components.playback

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberNextButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.media3.ui.compose.state.rememberPreviousButtonState
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Scale
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.ThrottledLiveData
import com.github.damontecres.stashapp.data.VideoFilter
import com.github.damontecres.stashapp.data.room.PlaybackEffect
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.playback.PlaybackSceneFragment
import com.github.damontecres.stashapp.playback.PlaylistFragment
import com.github.damontecres.stashapp.playback.TrackActivityPlaybackListener
import com.github.damontecres.stashapp.playback.TrackSupport
import com.github.damontecres.stashapp.playback.TrackSupportReason
import com.github.damontecres.stashapp.playback.TrackType
import com.github.damontecres.stashapp.playback.TranscodeDecision
import com.github.damontecres.stashapp.playback.checkForSupport
import com.github.damontecres.stashapp.playback.maybeMuteAudio
import com.github.damontecres.stashapp.playback.switchToTranscode
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.image.ImageFilterDialog
import com.github.damontecres.stashapp.ui.indexOfFirstOrNull
import com.github.damontecres.stashapp.ui.pages.SearchForDialog
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.util.toLongMilliseconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.milliseconds

const val TAG = "PlaybackPageContent"

class PlaybackViewModel : ViewModel() {
    private lateinit var server: StashServer
    private lateinit var exceptionHandler: CoroutineExceptionHandler
    private var markersEnabled by Delegates.notNull<Boolean>()
    private var saveFilters = true
    private var videoFiltersEnabled = false

    val scene = MutableLiveData<FullSceneData>()
    val performers = MutableLiveData<List<PerformerData>>(listOf())

    val mediaItemTag = MutableLiveData<PlaylistFragment.MediaItemTag>()
    val markers = MutableLiveData<List<BasicMarker>>(listOf())
    val oCount = MutableLiveData(0)
    val rating100 = MutableLiveData(0)
    val spriteImageLoaded = MutableLiveData(false)

    private val _videoFilter = MutableLiveData<VideoFilter?>(null)
    val videoFilter = ThrottledLiveData(_videoFilter, 500L)

    fun init(
        server: StashServer,
        markersEnabled: Boolean,
        saveFilters: Boolean,
        videoFiltersEnabled: Boolean,
    ) {
        this.server = server
        this.markersEnabled = markersEnabled
        this.saveFilters = saveFilters
        this.videoFiltersEnabled = videoFiltersEnabled
        this.exceptionHandler = LoggingCoroutineExceptionHandler(server, viewModelScope)
    }

    private var sceneJob: Job = Job()

    fun changeScene(tag: PlaylistFragment.MediaItemTag) {
        sceneJob.cancelChildren()
        this.mediaItemTag.value = tag
        this.oCount.value = 0
        this.rating100.value = 0
        this.markers.value = listOf()
        this.spriteImageLoaded.value = false

        refreshScene(tag.item.id)

        if (videoFiltersEnabled) {
            updateVideoFilter(VideoFilter())
            if (saveFilters && videoFiltersEnabled) {
                viewModelScope.launch(sceneJob + StashCoroutineExceptionHandler() + Dispatchers.IO) {
                    val vf =
                        StashApplication
                            .getDatabase()
                            .playbackEffectsDao()
                            .getPlaybackEffect(server.url, tag.item.id, DataType.SCENE)
                    if (vf != null) {
                        Log.d(
                            TAG,
                            "Loaded VideoFilter for scene ${tag.item.id}",
                        )
                        withContext(Dispatchers.Main) {
                            videoFilter.stopThrottling(true)
                            updateVideoFilter(vf.videoFilter)
                            videoFilter.startThrottling()
                        }
                    }
                }
            }
        }

        // Fetch preview sprites
        viewModelScope.launch(sceneJob + StashCoroutineExceptionHandler()) {
            val context = StashApplication.getApplication()
            val imageLoader = SingletonImageLoader.get(context)
            if (tag.item.spriteUrl.isNotNullOrBlank()) {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(tag.item.spriteUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .scale(Scale.FILL)
                        .build()
                val result = imageLoader.enqueue(request).job.await()
                spriteImageLoaded.value = result.image != null
            }
        }
    }

    private fun refreshScene(sceneId: String) {
        // Fetch o count & markers
        viewModelScope.launch(sceneJob + exceptionHandler) {
            oCount.value = 0
            rating100.value = 0
            markers.value = listOf()
            performers.value = listOf()

            val queryEngine = QueryEngine(server)
            val scene = queryEngine.getScene(sceneId)
            if (scene != null) {
                oCount.value = scene.o_counter ?: 0
                rating100.value = scene.rating100 ?: 0
                if (markersEnabled) {
                    markers.value =
                        scene.scene_markers
                            .sortedBy { it.seconds }
                            .map(::BasicMarker)
                }
                if (scene.performers.isNotEmpty()) {
                    performers.value =
                        queryEngine.findPerformers(performerIds = scene.performers.map { it.id })
                }
            }
            this@PlaybackViewModel.scene.value = scene
        }
    }

    fun addMarker(
        position: Long,
        tagId: String,
    ) {
        mediaItemTag.value?.let {
            viewModelScope.launch(exceptionHandler) {
                val mutationEngine = MutationEngine(server)
                val newMarker = mutationEngine.createMarker(it.item.id, position, tagId)
                if (newMarker != null) {
                    // Refresh markers
                    refreshScene(it.item.id)
                    Toast
                        .makeText(
                            StashApplication.getApplication(),
                            "Created marker at ${position.milliseconds}",
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    }

    fun incrementOCount(sceneId: String) {
        viewModelScope.launch(exceptionHandler) {
            val mutationEngine = MutationEngine(server)
            oCount.value = mutationEngine.incrementOCounter(sceneId).count
        }
    }

    fun updateVideoFilter(newFilter: VideoFilter?) {
        _videoFilter.value = newFilter
    }

    fun saveVideoFilter() {
        mediaItemTag.value?.item?.let {
            viewModelScope.launchIO(StashCoroutineExceptionHandler(autoToast = true)) {
                val vf = _videoFilter.value
                if (vf != null) {
                    StashApplication
                        .getDatabase()
                        .playbackEffectsDao()
                        .insert(PlaybackEffect(server.url, it.id, DataType.SCENE, vf))
                    Log.d(TAG, "Saved VideoFilter for scene ${it.id}")
                    withContext(Dispatchers.Main) {
                        Toast
                            .makeText(
                                StashApplication.getApplication(),
                                "Saved",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            }
        }
    }

    fun updateRating(
        sceneId: String,
        rating100: Int,
    ) {
        viewModelScope.launch(exceptionHandler) {
            val newRating =
                MutationEngine(server).setRating(sceneId, rating100)?.rating100 ?: 0
            this@PlaybackViewModel.rating100.value = newRating
            showSetRatingToast(StashApplication.getApplication(), newRating)
        }
    }
}

val playbackScaleOptions =
    mapOf(
        ContentScale.Fit to "Fit",
        ContentScale.None to "None",
        ContentScale.Crop to "Crop",
//        ContentScale.Inside to "Inside",
        ContentScale.FillBounds to "Fill",
        ContentScale.FillWidth to "Fill Width",
        ContentScale.FillHeight to "Fill Height",
    )

@OptIn(UnstableApi::class)
@Composable
fun PlaybackPageContent(
    server: StashServer,
    player: ExoPlayer,
    playlist: List<MediaItem>,
    startIndex: Int,
    uiConfig: ComposeUiConfig,
    markersEnabled: Boolean,
    playlistPager: ComposePager<StashData>?,
    onClickPlaylistItem: ((Int) -> Unit)?,
    itemOnClick: ItemOnClicker<Any>,
    modifier: Modifier = Modifier,
    controlsEnabled: Boolean = true,
    viewModel: PlaybackViewModel = viewModel(),
    startPosition: Long = C.TIME_UNSET,
) {
    var savedStartPosition by rememberSaveable(startPosition) { mutableLongStateOf(startPosition) }
    var currentPlaylistIndex by rememberSaveable(startIndex) { mutableIntStateOf(startIndex) }
    if (playlist.isEmpty() || playlist.size < currentPlaylistIndex) {
        return
    }

    val context = LocalContext.current
    val navigationManager = LocalGlobalContext.current.navigationManager
    val currentScene by viewModel.mediaItemTag.observeAsState(
        playlist[currentPlaylistIndex].localConfiguration!!.tag as PlaylistFragment.MediaItemTag,
    )
    val markers by viewModel.markers.observeAsState(listOf())
    val oCount by viewModel.oCount.observeAsState(0)
    val rating100 by viewModel.rating100.observeAsState(0)
    val spriteImageLoaded by viewModel.spriteImageLoaded.observeAsState(false)
    var captions by remember { mutableStateOf<List<TrackSupport>>(listOf()) }
    var subtitles by remember { mutableStateOf<List<Cue>?>(null) }
    var subtitleIndex by remember { mutableStateOf<Int?>(null) }
    var mediaIndexSubtitlesActivated by remember { mutableStateOf<Int>(-1) }
    var audioIndex by remember { mutableStateOf<Int?>(null) }
    var audioOptions by remember { mutableStateOf<List<String>>(listOf()) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    val videoFilter by viewModel.videoFilter.observeAsState()

    var showSceneDetails by rememberSaveable { mutableStateOf(false) }
    val scene by viewModel.scene.observeAsState()
    val performers by viewModel.performers.observeAsState(listOf())

    var trackActivityListener = remember<TrackActivityPlaybackListener?>(server) { null }
    AmbientPlayerListener(player)

    LifecycleStartEffect(Unit) {
        onStopOrDispose {
            savedStartPosition = player.currentPosition
            currentPlaylistIndex = player.currentMediaItemIndex
            trackActivityListener?.release(player.currentPosition)
            StashExoPlayer.releasePlayer()
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var showPlaylist by remember { mutableStateOf(false) }
    var contentScale by remember { mutableStateOf(ContentScale.Fit) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    LaunchedEffect(playbackSpeed) { player.setPlaybackSpeed(playbackSpeed) }

    val presentationState = rememberPresentationState(player)
    val scaledModifier =
        Modifier.resizeWithContentScale(contentScale, presentationState.videoSizeDp)

    var contentCurrentPosition by remember { mutableLongStateOf(0L) }

    var createMarkerPosition by remember { mutableLongStateOf(-1L) }
    var playingBeforeDialog by remember { mutableStateOf(false) }

    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var showDebugInfo by remember {
        mutableStateOf(
            prefs.getBoolean(context.getString(R.string.pref_key_show_playback_debug_info), false),
        )
    }
    val showSkipProgress =
        remember {
            prefs.getBoolean(
                context.getString(R.string.pref_key_playback_show_skip_progress),
                true,
            )
        }
    val skipWithLeftRight =
        remember {
            prefs.getBoolean(
                context.getString(R.string.pref_key_playback_skip_left_right),
                true,
            )
        }
    // Enabled if the preference is enabled and playing a playlist of markers
    val nextWithUpDown =
        remember {
            playlistPager != null &&
                playlistPager.filter.dataType == DataType.MARKER &&
                playlistPager.size > 1 &&
                prefs.getBoolean(
                    context.getString(R.string.pref_key_playback_next_up_down),
                    false,
                )
        }
    val useVideoFilters =
        remember { prefs.getBoolean(context.getString(R.string.pref_key_video_filters), false) }

    val controllerViewState =
        remember {
            ControllerViewState(
                prefs.getInt(
                    "controllerShowTimeoutMs",
                    PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS,
                ),
                controlsEnabled,
            )
        }.also {
            LaunchedEffect(it) {
                it.observe()
            }
        }

    val retryMediaItemIds = remember { mutableSetOf<String>() }

    LaunchedEffect(Unit) {
        viewModel.init(server, markersEnabled, uiConfig.persistVideoFilters, useVideoFilters)
        viewModel.changeScene(playlist[currentPlaylistIndex].localConfiguration!!.tag as PlaylistFragment.MediaItemTag)
        maybeMuteAudio(context, false, player)
        player.setMediaItems(playlist, startIndex, savedStartPosition)
        if (playlistPager == null) {
            player.setupFinishedBehavior(context, navigationManager) {
                controllerViewState.showControls()
            }
        }
        StashExoPlayer.addListener(
            object : Player.Listener {
                override fun onCues(cueGroup: CueGroup) {
//                    val cues =
//                        cueGroup.cues
//                            .mapNotNull { it.text }
//                            .joinToString("\n")
//                    Log.v(TAG, "onCues: \n$cues")
                    subtitles = cueGroup.cues.ifEmpty { null }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    val trackInfo = checkForSupport(tracks)
                    val audioTracks =
                        trackInfo
                            .filter { it.type == TrackType.AUDIO && it.supported == TrackSupportReason.HANDLED }
                    audioIndex = audioTracks.indexOfFirstOrNull { it.selected }
                    audioOptions =
                        audioTracks.map { it.labels.joinToString(", ").ifBlank { "Default" } }
                    captions =
                        trackInfo.filter { it.type == TrackType.TEXT && it.supported == TrackSupportReason.HANDLED }

                    val captionsByDefault =
                        prefs.getBoolean(
                            context.getString(R.string.pref_key_captions_on_by_default),
                            true,
                        )
                    if (captionsByDefault && captions.isNotEmpty() && mediaIndexSubtitlesActivated != currentPlaylistIndex) {
                        // Captions will be empty when transitioning to new media item
                        // Only want to activate subtitles once in case the user turns them off
                        mediaIndexSubtitlesActivated = currentPlaylistIndex
                        val languageCode = Locale.getDefault().language
                        captions.indexOfFirstOrNull { it.format.language == languageCode }?.let {
                            Log.v(TAG, "Found default subtitle track for $languageCode: $it")
                            if (toggleSubtitles(player, null, it)) {
                                subtitleIndex = it
                            }
                        }
                    }
                }

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    if (mediaItem != null) {
                        viewModel.changeScene(mediaItem.localConfiguration!!.tag as PlaylistFragment.MediaItemTag)
                    }
                    subtitles = null
                    subtitleIndex = null
                    currentPlaylistIndex = player.currentMediaItemIndex
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(
                        TAG,
                        "PlaybackException on scene ${currentScene.item.id}, errorCode=${error.errorCode}",
                        error,
                    )
                    val showError =
                        when (error.errorCode) {
                            PlaybackException.ERROR_CODE_DECODING_FAILED,
                            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                            -> {
                                val current = player.currentMediaItem
                                val currentPosition = player.currentMediaItemIndex
                                if (current != null) {
                                    val tag =
                                        (current.localConfiguration!!.tag as PlaylistFragment.MediaItemTag)
                                    val id = tag.item.id
                                    val isTranscoding =
                                        tag.streamDecision.transcodeDecision == TranscodeDecision.Transcode ||
                                            tag.streamDecision.transcodeDecision is TranscodeDecision.ForcedTranscode
                                    if (id !in retryMediaItemIds && !isTranscoding) {
                                        retryMediaItemIds.add(id)
                                        val newMediaItem = switchToTranscode(context, current)
                                        val newTag =
                                            newMediaItem.localConfiguration!!.tag as PlaylistFragment.MediaItemTag
                                        Log.d(
                                            TAG,
                                            "Using new transcoding media item: ${newTag.streamDecision}",
                                        )
                                        viewModel.changeScene(newTag)
                                        player.replaceMediaItem(currentPosition, newMediaItem)
                                        player.prepare()
                                        if (savedStartPosition != C.TIME_UNSET) {
                                            player.seekTo(savedStartPosition)
                                        }
                                        player.play()
                                        false
                                    } else {
                                        true
                                    }
                                } else {
                                    true
                                }
                            }

                            else -> true
                        }
                    if (showError) {
                        Toast
                            .makeText(
                                context,
                                "Play error: ${error.localizedMessage}",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                }
            },
        )

        player.prepare()
        if (useVideoFilters) {
            Log.d(TAG, "Enabling video effects")
            player.setVideoEffects(listOf())
        }
    }

    LaunchedEffect(server, currentScene, player) {
        trackActivityListener?.apply {
            release()
            StashExoPlayer.removeListener(this)
        }
        currentScene?.let {
            val appTracking =
                PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getBoolean(context.getString(R.string.pref_key_playback_track_activity), true)
            trackActivityListener =
                if (appTracking && server.serverPreferences.trackActivity && markersEnabled) {
                    TrackActivityPlaybackListener(
                        context = context,
                        server = server,
                        scene = currentScene.item,
                        getCurrentPosition = {
                            player.currentPosition
                        },
                    )
                } else {
                    null
                }
            trackActivityListener?.let { StashExoPlayer.addListener(it) }
        }
    }

    var skipIndicatorDuration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(controllerViewState.controlsVisible) {
        // If controller shows/hides, immediately cancel the skip indicator
        skipIndicatorDuration = 0L
    }
    var skipPosition by remember { mutableLongStateOf(0L) }
    val updateSkipIndicator = { delta: Long ->
        if (skipIndicatorDuration > 0 && delta < 0 || skipIndicatorDuration < 0 && delta > 0) {
            skipIndicatorDuration = 0
        }
        skipIndicatorDuration += delta
        skipPosition = player.currentPosition
    }
    val scope = rememberCoroutineScope()
    val playPauseState = rememberPlayPauseButtonState(player)
    val previousState = rememberPreviousButtonState(player)
    val nextState = rememberNextButtonState(player)
    val seekBarState = rememberSeekBarState(player, scope)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.tryRequestFocus()
    }
    val playbackKeyHandler =
        remember {
            PlaybackKeyHandler(
                player = player,
                controlsEnabled = controlsEnabled,
                skipWithLeftRight = skipWithLeftRight,
                nextWithUpDown = nextWithUpDown,
                controllerViewState = controllerViewState,
                updateSkipIndicator = updateSkipIndicator,
            )
        }
    Box(
        modifier
            .background(Color.Black)
            .onKeyEvent(playbackKeyHandler::onKeyEvent)
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = scaledModifier.clickable(enabled = false) { showControls = !showControls },
        )
        if (presentationState.coverSurface) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black),
            )
        }
        if (!controllerViewState.controlsVisible && skipIndicatorDuration != 0L) {
            SkipIndicator(
                durationMs = skipIndicatorDuration,
                onFinish = {
                    skipIndicatorDuration = 0L
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 70.dp),
            )
            if (showSkipProgress) {
                currentScene.item.duration?.let {
                    val percent =
                        skipPosition.toFloat() / (it.toLongMilliseconds).toFloat()
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .background(MaterialTheme.colorScheme.border)
                                .clip(RectangleShape)
                                .height(3.dp)
                                .fillMaxWidth(percent),
                    ) {}
                }
            }
        }

        if (!controllerViewState.controlsVisible && subtitleIndex != null && skipIndicatorDuration == 0L) {
            // TODO style
            subtitles?.let { cues ->
                val text = cues.mapNotNull { it.text }.joinToString("\n")
                val bitmaps =
                    cues.mapNotNull { cue ->
                        cue.bitmap?.let { Pair(it, cue.bitmapHeight) }
                    }
                val background =
                    if (text.isNotNullOrBlank()) {
                        AppColors.TransparentBlack50
                    } else {
                        Color.Transparent
                    }
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .background(background),
                ) {
                    if (text.isNotNullOrBlank()) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 24.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.padding(4.dp),
                        )
                    } else if (bitmaps.isNotEmpty()) {
                        Column {
                            bitmaps.forEach {
                                Image(
                                    bitmap = it.first.asImageBitmap(),
                                    contentDescription = null,
//                                    modifier = Modifier.height(100.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        currentScene?.let {
            AnimatedVisibility(
                controllerViewState.controlsVisible,
                Modifier,
                slideInVertically { it },
                slideOutVertically { it },
            ) {
                PlaybackOverlay(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Transparent),
                    uiConfig = uiConfig,
                    scene = currentScene.item,
                    captions = captions,
                    markers = markers,
                    streamDecision = currentScene.streamDecision,
                    oCounter = oCount,
                    playerControls = PlayerControlsImpl(player),
                    onPlaybackActionClick = {
                        when (it) {
                            PlaybackAction.CreateMarker -> {
                                if (markersEnabled) {
                                    playingBeforeDialog = player.isPlaying
                                    player.pause()
                                    controllerViewState.hideControls()
                                    createMarkerPosition = player.currentPosition
                                }
                            }

                            PlaybackAction.OCount -> {
                                viewModel.incrementOCount(currentScene.item.id)
                            }

                            PlaybackAction.ShowDebug -> {
                                showDebugInfo = !showDebugInfo
                            }

                            PlaybackAction.ShowVideoFilterDialog -> showFilterDialog = true

                            PlaybackAction.ShowPlaylist -> {
                                if (playlistPager != null && playlistPager.size > 1) {
                                    showPlaylist = true
                                    controllerViewState.hideControls()
                                }
                            }

                            is PlaybackAction.ToggleCaptions -> {
                                if (toggleSubtitles(player, subtitleIndex, it.index)) {
                                    subtitleIndex = it.index
                                } else {
                                    subtitleIndex = null
                                    subtitles = null
                                }
                                controllerViewState.hideControls()
                            }

                            is PlaybackAction.PlaybackSpeed -> playbackSpeed = it.value
                            is PlaybackAction.Scale -> contentScale = it.scale
                            is PlaybackAction.ToggleAudio -> {
                                if (toggleAudio(player, audioIndex, it.index)) {
                                    audioIndex = it.index
                                } else {
                                    audioIndex = null
                                }
                            }

                            PlaybackAction.ShowSceneDetails -> {
                                showSceneDetails = true
                            }
                        }
                    },
                    onSeekBarChange = seekBarState::onValueChange,
                    controllerViewState = controllerViewState,
                    showPlay = playPauseState.showPlay,
                    previousEnabled = previousState.isEnabled,
                    nextEnabled = nextState.isEnabled,
                    seekEnabled = seekBarState.isEnabled,
                    showDebugInfo = showDebugInfo,
                    spriteImageLoaded = spriteImageLoaded,
                    moreButtonOptions =
                        MoreButtonOptions(
                            buildMap {
                                if (markersEnabled) {
                                    put("Create Marker", PlaybackAction.CreateMarker)
                                }
                                if (playlistPager != null && playlistPager.size > 1) {
                                    put("Show Playlist", PlaybackAction.ShowPlaylist)
                                }
                                if (useVideoFilters) {
                                    put("Set video filters", PlaybackAction.ShowVideoFilterDialog)
                                }
                                put("Details", PlaybackAction.ShowSceneDetails)
                            },
                        ),
                    subtitleIndex = subtitleIndex,
                    audioIndex = audioIndex,
                    audioOptions = audioOptions,
                    playbackSpeed = playbackSpeed,
                    scale = contentScale,
                )
            }
        }
        val dismiss = {
            createMarkerPosition = -1
            if (playingBeforeDialog) {
                player.play()
            }
        }
        SearchForDialog(
            show = markersEnabled && createMarkerPosition >= 0,
            uiConfig = uiConfig,
            dataType = DataType.TAG,
            onItemClick = { item ->
                viewModel.addMarker(createMarkerPosition, item.id)
                dismiss.invoke()
            },
            onDismissRequest = dismiss,
            dialogTitle = "Create marker at ${createMarkerPosition.milliseconds}?",
            dismissOnClick = false,
        )
        if (playlistPager != null && onClickPlaylistItem != null) {
            PlaylistListDialog(
                show = showPlaylist,
                onDismiss = { showPlaylist = false },
                player = player,
                pager = playlistPager,
                onClickPlaylistItem = onClickPlaylistItem,
                modifier = Modifier,
            )
        }
        videoFilter?.let {
            val effectList = it.createEffectList()
            Log.d(TAG, "Applying ${effectList.size} effects")
            player.setVideoEffects(effectList)

            AnimatedVisibility(showFilterDialog) {
                ImageFilterDialog(
                    filter = it,
                    showVideoOptions = false,
                    uiConfig = uiConfig,
                    onChange = viewModel::updateVideoFilter,
                    onClickSave = viewModel::saveVideoFilter,
                    onDismissRequest = {
                        showFilterDialog = false
                    },
                )
            }
        }
        AnimatedVisibility(showSceneDetails && scene != null) {
            LaunchedEffect(Unit) {
                playingBeforeDialog = player.isPlaying
                player.pause()
                controllerViewState.hideControls()
            }
            scene?.let { scene ->
                Dialog(
                    onDismissRequest = {
                        showSceneDetails = false
                        if (playingBeforeDialog) {
                            player.play()
                        }
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    SceneDetailsOverlay(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = .75f)),
                        server = server,
                        scene = scene,
                        performers = performers,
                        uiConfig = uiConfig,
                        itemOnClick = itemOnClick,
                        rating100 = rating100,
                        onRatingChange = { viewModel.updateRating(scene.id, it) },
                    )
                }
            }
        }
    }
}

fun Player.setupFinishedBehavior(
    context: Context,
    navigationManager: NavigationManager,
    showController: () -> Unit,
) {
    val finishedBehavior =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(
                "playbackFinishedBehavior",
                context.getString(R.string.playback_finished_do_nothing),
            )
    when (finishedBehavior) {
        context.getString(R.string.playback_finished_repeat) -> {
            repeatMode = Player.REPEAT_MODE_ONE
        }

        context.getString(R.string.playback_finished_return) ->
            StashExoPlayer.addListener(
                object :
                    Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            navigationManager.goBack()
                        }
                    }
                },
            )

        context.getString(R.string.playback_finished_do_nothing) -> {
            StashExoPlayer.addListener(
                object :
                    Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            showController()
                        }
                    }
                },
            )
        }

        else ->
            Log.w(
                PlaybackSceneFragment.TAG,
                "Unknown playbackFinishedBehavior: $finishedBehavior",
            )
    }
}

class PlaybackKeyHandler(
    private val player: Player,
    private val controlsEnabled: Boolean,
    private val skipWithLeftRight: Boolean,
    private val nextWithUpDown: Boolean,
    private val controllerViewState: ControllerViewState,
    private val updateSkipIndicator: (Long) -> Unit,
) {
    fun onKeyEvent(it: KeyEvent): Boolean {
        var result = true
        if (!controlsEnabled) {
            result = false
        } else if (it.type != KeyEventType.KeyUp) {
            result = false
        } else if (isDpad(it)) {
            if (!controllerViewState.controlsVisible) {
                if (skipWithLeftRight && it.key == Key.DirectionLeft) {
                    updateSkipIndicator(-player.seekBackIncrement)
                    player.seekBack()
                } else if (skipWithLeftRight && it.key == Key.DirectionRight) {
                    player.seekForward()
                    updateSkipIndicator(player.seekForwardIncrement)
                } else if (nextWithUpDown && it.key == Key.DirectionUp) {
                    player.seekToPreviousMediaItem()
                } else if (nextWithUpDown && it.key == Key.DirectionDown) {
                    player.seekToNextMediaItem()
                } else {
                    controllerViewState.showControls()
                }
            } else {
                // When controller is visible, its buttons will handle pulsing
            }
        } else if (isMedia(it)) {
            when (it.key) {
                Key.MediaPlay -> {
                    Util.handlePlayButtonAction(player)
                }

                Key.MediaPause -> {
                    Util.handlePauseButtonAction(player)
                    controllerViewState.showControls()
                }

                Key.MediaPlayPause -> {
                    Util.handlePlayPauseButtonAction(player)
                    if (!player.isPlaying) {
                        controllerViewState.showControls()
                    }
                }

                Key.MediaFastForward, Key.MediaSkipForward -> {
                    player.seekForward()
                    updateSkipIndicator(player.seekForwardIncrement)
                }

                Key.MediaRewind, Key.MediaSkipBackward -> {
                    player.seekBack()
                    updateSkipIndicator(-player.seekBackIncrement)
                }

                Key.MediaNext -> if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT)) player.seekToNext()
                Key.MediaPrevious -> if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS)) player.seekToPrevious()
                else -> result = false
            }
        } else if (it.key == Key.Enter && !controllerViewState.controlsVisible) {
            controllerViewState.showControls()
        } else if (it.key == Key.Back && controllerViewState.controlsVisible) {
            controllerViewState.hideControls()
        } else {
            controllerViewState.pulseControls()
            result = false
        }
        return result
    }
}

@OptIn(UnstableApi::class)
fun toggleSubtitles(
    player: Player,
    currentActiveSubtitleIndex: Int?,
    index: Int,
): Boolean {
    val subtitleTracks =
        player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
    if (index !in subtitleTracks.indices || currentActiveSubtitleIndex != null && currentActiveSubtitleIndex == index) {
        Log.v(
            TAG,
            "Deactivating subtitles",
        )
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        return false
    } else {
        Log.v(
            TAG,
            "Activating subtitle ${subtitleTracks[index].mediaTrackGroup.id}",
        )
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(
                    TrackSelectionOverride(
                        subtitleTracks[index].mediaTrackGroup,
                        0,
                    ),
                ).build()
        return true
    }
}

@OptIn(UnstableApi::class)
fun toggleAudio(
    player: Player,
    audioIndex: Int?,
    index: Int,
): Boolean {
    val audioTracks =
        player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
    if (index !in audioTracks.indices || audioIndex != null && audioIndex == index) {
        Log.v(
            TAG,
            "Deactivating audio",
        )
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
        return false
    } else {
        Log.v(
            TAG,
            "Activating audio ${audioTracks[index].mediaTrackGroup.id}",
        )
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setOverrideForType(
                    TrackSelectionOverride(
                        audioTracks[index].mediaTrackGroup,
                        0,
                    ),
                ).build()
        return true
    }
}
