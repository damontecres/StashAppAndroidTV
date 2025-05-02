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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
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
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.playback.PlaybackSceneFragment
import com.github.damontecres.stashapp.playback.StreamDecision
import com.github.damontecres.stashapp.playback.TrackActivityPlaybackListener
import com.github.damontecres.stashapp.playback.buildMediaItem
import com.github.damontecres.stashapp.playback.getStreamDecision
import com.github.damontecres.stashapp.playback.maybeMuteAudio
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.pages.SearchForDialog
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.asMarkerData
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.toLongMilliseconds
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

const val TAG = "PlaybackPageContent"

class PlaybackViewModel : ViewModel() {
    private lateinit var server: StashServer
    private lateinit var scene: FullSceneData

    val markers = MutableLiveData<List<BasicMarker>>(listOf())
    val oCount = MutableLiveData(0)
    val previewImageLoader = MutableLiveData(false)

    fun init(
        context: Context,
        server: StashServer,
        scene: FullSceneData,
    ) {
        this.server = server
        this.scene = scene
        markers.value = scene.scene_markers.map { BasicMarker(it.asMarkerData(scene)) }
        oCount.value = scene.o_counter ?: 0
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            val imageLoader = SingletonImageLoader.get(context)
            if (scene.paths.sprite.isNotNullOrBlank()) {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(scene.paths.sprite)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .scale(Scale.FILL)
                        .build()
                val result = imageLoader.enqueue(request).job.await()
                previewImageLoader.value = result.image != null
            }
        }
    }

    fun addMarker(
        position: Long,
        tagId: String,
    ) {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val mutationEngine = MutationEngine(server)
            val newMarker = mutationEngine.createMarker(scene.id, position, tagId)
            newMarker?.let {
                val list = markers.value!!.toMutableList()
                list.add(BasicMarker(it.asMarkerData(scene)))
                markers.value = list.sortedBy { m -> m.seconds }
                Toast
                    .makeText(
                        StashApplication.getApplication(),
                        "Created marker at ${position.milliseconds}",
                        Toast.LENGTH_SHORT,
                    ).show()
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

@OptIn(UnstableApi::class)
@Composable
fun PlaybackPageContent(
    server: StashServer,
    scene: FullSceneData,
    startPosition: Long,
    playbackMode: PlaybackMode,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
    controlsEnabled: Boolean = true,
    viewModel: PlaybackViewModel = viewModel(),
) {
    val context = LocalContext.current
    val navigationManager = LocalGlobalContext.current.navigationManager
    val player =
        remember {
            StashExoPlayer.getInstance(context, server).apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
        }
    AmbientPlayerListener(player)
    var trackActivityListener = remember<TrackActivityPlaybackListener?>(server, scene) { null }
    LifecycleStartEffect(Unit) {
        onStopOrDispose {
            trackActivityListener?.release(player.currentPosition)
            StashExoPlayer.releasePlayer()
        }
    }
    LaunchedEffect(server, scene.id) {
        viewModel.init(context, server, scene)
    }
    val playbackScene = remember { Scene.fromFullSceneData(scene) }
    val markers by viewModel.markers.observeAsState(listOf())
    val oCount by viewModel.oCount.observeAsState(0)
    val imageLoaded by viewModel.previewImageLoader.observeAsState(false)
    var subtitles by remember { mutableStateOf<String?>(null) }
    var subtitleIndex by remember { mutableStateOf<Int?>(null) }

    var showControls by remember { mutableStateOf(true) }
    var currentContentScaleIndex by remember { mutableIntStateOf(0) }
    val contentScale = ContentScale.Fit

    val presentationState = rememberPresentationState(player)
    val scaledModifier =
        Modifier.resizeWithContentScale(contentScale, presentationState.videoSizeDp)

    var contentCurrentPosition by remember { mutableLongStateOf(0L) }

    var createMarkerPosition by remember { mutableLongStateOf(-1L) }
    var playingBeforeCreateMarker by remember { mutableStateOf(false) }

    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

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
    var streamDecision by remember { mutableStateOf<StreamDecision?>(null) }

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
    var skipIndicatorDuration by remember { mutableLongStateOf(0L) }
    var skipPosition by remember { mutableLongStateOf(0L) }
    val updateSkipIndicator = { delta: Long ->
        if (skipIndicatorDuration > 0 && delta < 0 || skipIndicatorDuration < 0 && delta > 0) {
            skipIndicatorDuration = 0
        }
        skipIndicatorDuration += delta
        skipPosition = player.currentPosition
    }
    if (controllerViewState.controlsVisible) {
        // If controls become visible, cancel the skip indicator
        skipIndicatorDuration = 0L
    }

    val scope = rememberCoroutineScope()
    val playPauseState = rememberPlayPauseButtonState(player)
    val previousState = rememberPreviousButtonState(player)
    val nextState = rememberNextButtonState(player)
    val seekBarState = rememberSeekBarState(player, scope)

    LaunchedEffect(server, scene, player) {
        trackActivityListener?.apply {
            release()
            StashExoPlayer.removeListener(this)
        }
        player.setupFinishedBehavior(context, navigationManager) {
            controllerViewState.showControls()
        }

        StashExoPlayer.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "PlaybackException on ${scene.id}", error)
                    Toast
                        .makeText(
                            context,
                            "Play error: ${error.localizedMessage}",
                            Toast.LENGTH_LONG,
                        ).show()
                }

                override fun onCues(cueGroup: CueGroup) {
                    val cues =
                        cueGroup.cues
                            .mapNotNull { it.text }
                            .joinToString("\n")
//                    Log.v(TAG, "onCues: \n$cues")
                    subtitles = cues
                }
            },
        )

        val appTracking =
            prefs.getBoolean(context.getString(R.string.pref_key_playback_track_activity), true)
        trackActivityListener =
            if (appTracking && server.serverPreferences.trackActivity) {
                TrackActivityPlaybackListener(
                    context = context,
                    server = server,
                    scene = playbackScene,
                    getCurrentPosition = {
                        player.currentPosition
                    },
                )
            } else {
                null
            }
        maybeMuteAudio(context, false, player)
        val decision = getStreamDecision(context, playbackScene, playbackMode)
        Log.d(TAG, "streamDecision=$decision")
        val media = buildMediaItem(context, decision, playbackScene)
        player.setMediaItem(media, startPosition.coerceAtLeast(0L))
        player.prepare()
        streamDecision = decision
        trackActivityListener?.let { StashExoPlayer.addListener(it) }
    }

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
            if (showSkipProgress && playbackScene.duration != null) {
                val percent =
                    skipPosition.toFloat() / (playbackScene.duration.toLongMilliseconds).toFloat()
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
                scene = playbackScene,
                markers = markers,
                streamDecision = streamDecision,
                oCounter = oCount,
                playerControls = PlayerControlsImpl(player),
                onPlaybackActionClick = {
                    when (it) {
                        PlaybackAction.CreateMarker -> {
                            playingBeforeCreateMarker = player.isPlaying
                            player.pause()
                            createMarkerPosition = player.currentPosition
                        }

                        PlaybackAction.OCount -> {
                            viewModel.incrementOCount(scene.id)
                        }

                        PlaybackAction.ShowDebug -> {
                            showDebugInfo = !showDebugInfo
                        }

                        PlaybackAction.ShowPlaylist -> {
                            // no-op
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
                    }
                },
                onSeekBarChange = seekBarState::onValueChange,
                controllerViewState = controllerViewState,
                showPlay = playPauseState.showPlay,
                previousEnabled = previousState.isEnabled,
                nextEnabled = nextState.isEnabled,
                seekEnabled = seekBarState.isEnabled,
                showDebugInfo = showDebugInfo,
                spriteImageLoaded = imageLoaded,
                moreButtonOptions =
                    MoreButtonOptions(
                        mapOf("Create Marker" to PlaybackAction.CreateMarker),
                    ),
                subtitleIndex = subtitleIndex,
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
        show = createMarkerPosition >= 0,
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
                Key.MediaPlay -> player.play()
                Key.MediaPause -> {
                    player.pause()
                    controllerViewState.showControls()
                }

                Key.MediaPlayPause -> {
                    if (player.isPlaying) {
                        player.pause()
                        controllerViewState.showControls()
                    } else {
                        player.play()
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

                Key.MediaNext -> player.seekToNext()
                Key.MediaPrevious -> player.seekToPrevious()
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
