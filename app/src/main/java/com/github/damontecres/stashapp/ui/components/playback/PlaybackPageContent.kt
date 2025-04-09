package com.github.damontecres.stashapp.ui.components.playback

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.playback.StreamDecision
import com.github.damontecres.stashapp.playback.TrackActivityPlaybackListener
import com.github.damontecres.stashapp.playback.TranscodeDecision
import com.github.damontecres.stashapp.playback.buildMediaItem
import com.github.damontecres.stashapp.playback.getStreamDecision
import com.github.damontecres.stashapp.playback.maybeMuteAudio
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.pages.SearchForDialog
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.asMarkerData
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

const val TAG = "PlaybackPageContent"

class PlaybackViewModel : ViewModel() {
    private lateinit var server: StashServer
    private lateinit var scene: FullSceneData

    val markers = MutableLiveData<List<MarkerData>>(listOf())
    val oCount = MutableLiveData(0)

    fun init(
        server: StashServer,
        scene: FullSceneData,
    ) {
        this.server = server
        this.scene = scene
        markers.value = scene.scene_markers.map { it.asMarkerData(scene) }
        oCount.value = scene.o_counter ?: 0
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
                list.add(it.asMarkerData(scene))
                markers.value = list.sortedBy { m -> m.seconds }
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
        viewModel.init(server, scene)
    }
    val playbackScene = remember { Scene.fromFullSceneData(scene) }
    val markers by viewModel.markers.observeAsState(listOf())
    val oCount by viewModel.oCount.observeAsState(0)

    var showControls by remember { mutableStateOf(true) }
    var currentContentScaleIndex by remember { mutableIntStateOf(0) }
    val contentScale = ContentScale.Fit

    val presentationState = rememberPresentationState(player)
    val scaledModifier =
        Modifier.resizeWithContentScale(contentScale, presentationState.videoSizeDp)

    var contentCurrentPosition by remember { mutableLongStateOf(0L) }

    var createMarkerPosition by remember { mutableLongStateOf(-1L) }
    var playingBeforeCreateMarker by remember { mutableStateOf(false) }

    var showDebugInfo by remember {
        mutableStateOf(
            PreferenceManager
                .getDefaultSharedPreferences(
                    context,
                ).getBoolean(context.getString(R.string.pref_key_show_playback_debug_info), false),
        )
    }
    var streamDecision by remember {
        mutableStateOf<StreamDecision>(
            StreamDecision(
                sceneId = scene.id,
                transcodeDecision = TranscodeDecision.DirectPlay,
                videoSupported = false,
                audioSupported = false,
                containerSupported = false,
            ),
        )
    }

    LaunchedEffect(server, scene, player) {
        trackActivityListener?.apply {
            release()
            StashExoPlayer.removeListener(this)
        }
        val appTracking =
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_key_playback_track_activity), true)
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
        maybeMuteAudio(context, player)
        val decision = getStreamDecision(context, playbackScene, playbackMode)
        val media = buildMediaItem(context, streamDecision, playbackScene)
        player.setMediaItem(media, startPosition.coerceAtLeast(0L))
        player.prepare()
        streamDecision = decision
        trackActivityListener?.let { StashExoPlayer.addListener(it) }
    }

    val controllerViewState =
        remember { ControllerViewState(3, controlsEnabled) }.also {
            LaunchedEffect(it) {
                it.observe()
            }
        }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.tryRequestFocus()
    }
    Box(
        modifier
            .background(Color.Black)
            .onKeyEvent {
                var result = true
                if (!controlsEnabled) {
                    result = false
                } else if (it.type != KeyEventType.KeyUp) {
                    result = false
                } else if (isDpad(it)) {
                    if (!controllerViewState.controlsVisible) {
                        if (it.key == Key.DirectionLeft) {
                            player.seekBack()
                        } else if (it.key == Key.DirectionRight) {
                            player.seekForward()
                        } else {
                            controllerViewState.showControls()
                        }
                    } else {
                        // When controller is visible, its buttons will handle pulsing
                    }
                } else if (isMedia(it)) {
                    when (it.key) {
                        Key.MediaPlay -> player.play()
                        Key.MediaPause -> player.pause()
                        Key.MediaPlayPause -> if (player.isPlaying) player.pause() else player.play()
                        Key.MediaFastForward, Key.MediaSkipForward -> player.seekForward()
                        Key.MediaRewind, Key.MediaSkipBackward -> player.seekBack()
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
                result
            }.focusRequester(focusRequester)
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

        PlaybackOverlay(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
            uiConfig = uiConfig,
            server = server,
            scene = playbackScene,
            markers = markers,
            streamDecision = streamDecision,
            oCounter = oCount,
            player = player,
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
                }
            },
            controllerViewState = controllerViewState,
            showDebugInfo = showDebugInfo,
        )
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
