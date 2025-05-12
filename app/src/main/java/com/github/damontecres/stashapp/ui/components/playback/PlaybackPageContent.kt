package com.github.damontecres.stashapp.ui.components.playback

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
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
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.playback.PlaybackSceneFragment
import com.github.damontecres.stashapp.playback.PlaylistFragment
import com.github.damontecres.stashapp.playback.TrackActivityPlaybackListener
import com.github.damontecres.stashapp.playback.TrackSupportReason
import com.github.damontecres.stashapp.playback.TrackType
import com.github.damontecres.stashapp.playback.checkForSupport
import com.github.damontecres.stashapp.playback.maybeMuteAudio
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.indexOfFirstOrNull
import com.github.damontecres.stashapp.ui.pages.SearchForDialog
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.toLongMilliseconds
import com.github.damontecres.stashapp.views.models.EqualityMutableLiveData
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.milliseconds

const val TAG = "PlaybackPageContent"

class PlaybackViewModel : ViewModel() {
    private lateinit var server: StashServer
    private var markersEnabled by Delegates.notNull<Boolean>()

    val mediaItemTag = EqualityMutableLiveData<PlaylistFragment.MediaItemTag>()
    val markers = MutableLiveData<List<BasicMarker>>(listOf())
    val oCount = MutableLiveData(0)
    val spriteImageLoaded = MutableLiveData(false)

    fun init(
        server: StashServer,
        markersEnabled: Boolean,
    ) {
        this.server = server
        this.markersEnabled = markersEnabled
    }

    fun changeScene(tag: PlaylistFragment.MediaItemTag) {
        this.mediaItemTag.value = tag
        this.oCount.value = 0
        this.markers.value = listOf()
        this.spriteImageLoaded.value = false

        refreshScene(tag.item.id)

        // Fetch preview sprites
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
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
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val queryEngine = QueryEngine(server)
            val scene = queryEngine.getScene(sceneId)
            if (scene != null) {
                oCount.value = scene.o_counter ?: 0
                markers.value =
                    scene.scene_markers
                        .sortedBy { it.seconds }
                        .map(::BasicMarker)
            }
        }
    }

    fun addMarker(
        position: Long,
        tagId: String,
    ) {
        mediaItemTag.value?.let {
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
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
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val mutationEngine = MutationEngine(server)
            oCount.value = mutationEngine.incrementOCounter(sceneId).count
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
    playlist: List<MediaItem>,
    startIndex: Int,
    uiConfig: ComposeUiConfig,
    markersEnabled: Boolean,
    playlistPager: ComposePager<StashData>?,
    modifier: Modifier = Modifier,
    controlsEnabled: Boolean = true,
    viewModel: PlaybackViewModel = viewModel(),
    startPosition: Long = C.TIME_UNSET,
) {
    if (playlist.isEmpty() || playlist.size < startIndex) {
        return
    }
    val context = LocalContext.current
    val navigationManager = LocalGlobalContext.current.navigationManager
    val currentScene by viewModel.mediaItemTag.observeAsState(
        playlist[startIndex].localConfiguration!!.tag as PlaylistFragment.MediaItemTag,
    )
    val markers by viewModel.markers.observeAsState(listOf())
    val oCount by viewModel.oCount.observeAsState(0)
    val spriteImageLoaded by viewModel.spriteImageLoaded.observeAsState(false)
    var subtitles by remember { mutableStateOf<String?>(null) }
    var subtitleIndex by remember { mutableStateOf<Int?>(null) }
    var audioIndex by remember { mutableStateOf<Int?>(null) }
    var audioOptions by remember { mutableStateOf<List<String>>(listOf()) }

    var trackActivityListener = remember<TrackActivityPlaybackListener?>(server) { null }
    val player =
        remember {
            StashExoPlayer.getInstance(context, server).apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
        }
    AmbientPlayerListener(player)

    LifecycleStartEffect(Unit) {
        onStopOrDispose {
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
    var playingBeforeCreateMarker by remember { mutableStateOf(false) }

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
    val skipWithLeftRight = remember { prefs.getBoolean("skipWithDpad", true) }

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

    LaunchedEffect(Unit) {
        viewModel.init(server, markersEnabled)
        viewModel.changeScene(playlist[startIndex].localConfiguration!!.tag as PlaylistFragment.MediaItemTag)
        maybeMuteAudio(context, false, player)
        player.setMediaItems(playlist, startIndex, startPosition)
        if (playlistPager == null) {
            player.setupFinishedBehavior(context, navigationManager) {
                controllerViewState.showControls()
            }
        }
        StashExoPlayer.addListener(
            object : Player.Listener {
                override fun onCues(cueGroup: CueGroup) {
                    val cues =
                        cueGroup.cues
                            .mapNotNull { it.text }
                            .joinToString("\n")
//                    Log.v(TAG, "onCues: \n$cues")
                    subtitles = cues
                }

                override fun onTracksChanged(tracks: Tracks) {
                    val audioTracks =
                        checkForSupport(tracks).filter { it.type == TrackType.AUDIO && it.supported == TrackSupportReason.HANDLED }
                    audioIndex = audioTracks.indexOfFirstOrNull { it.selected }
                    audioOptions =
                        audioTracks.map { it.labels.joinToString(", ").ifBlank { "Default" } }
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
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "PlaybackException on scene ${currentScene.item.id}", error)
                    Toast
                        .makeText(
                            context,
                            "Play error: ${error.localizedMessage}",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            },
        )

        player.prepare()
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
                if (appTracking && server.serverPreferences.trackActivity) {
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
            subtitles?.let { text ->
                if (text.isNotNullOrBlank()) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 48.dp)
                                .background(AppColors.TransparentBlack50),
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 24.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.padding(4.dp),
                        )
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
                    markers = markers,
                    streamDecision = currentScene.streamDecision,
                    oCounter = oCount,
                    playerControls = PlayerControlsImpl(player),
                    onPlaybackActionClick = {
                        when (it) {
                            PlaybackAction.CreateMarker -> {
                                if (markersEnabled) {
                                    playingBeforeCreateMarker = player.isPlaying
                                    player.pause()
                                    createMarkerPosition = player.currentPosition
                                }
                            }

                            PlaybackAction.OCount -> {
                                viewModel.incrementOCount(currentScene.item.id)
                            }

                            PlaybackAction.ShowDebug -> {
                                showDebugInfo = !showDebugInfo
                            }

                            PlaybackAction.ShowPlaylist -> {
                                if (playlistPager != null && playlistPager.size > 1) {
                                    showPlaylist = true
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
                                put("Create Marker", PlaybackAction.CreateMarker)
                                if (playlistPager != null && playlistPager.size > 1) {
                                    put("Show Playlist", PlaybackAction.ShowPlaylist)
                                }
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
            if (playingBeforeCreateMarker) {
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
        PlaylistListDialog(
            show = showPlaylist,
            onDismiss = { showPlaylist = false },
            player = player,
            pager = playlistPager,
            modifier = Modifier,
        )
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
    subtitleIndex: Int?,
    index: Int,
): Boolean {
    val subtitleTracks =
        player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
    if (index !in subtitleTracks.indices || subtitleIndex != null && subtitleIndex == index) {
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
