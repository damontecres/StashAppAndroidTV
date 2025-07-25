package com.github.damontecres.stashapp.ui.components.playback

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.toLongMilliseconds
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import kotlin.time.Duration.Companion.seconds

private const val TAG = "VlcPlaybackPage"

@Composable
fun VlcPlaybackPage(
    server: StashServer,
    uiConfig: ComposeUiConfig,
    sceneId: String,
    startPosition: Long,
    playbackMode: PlaybackMode,
    itemOnClick: ItemOnClicker<Any>,
    modifier: Modifier = Modifier,
) {
    var scene by remember { mutableStateOf<FullSceneData?>(null) }
    var currentScene by remember { mutableStateOf<Scene?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(server, sceneId) {
        scope.launch(
            LoggingCoroutineExceptionHandler(
                server,
                scope,
                toastMessage = "Error fetching scene",
            ),
        ) {
            val fullScene = QueryEngine(server).getScene(sceneId)
            if (fullScene != null) {
                scene = fullScene
                currentScene = Scene.fromFullSceneData(fullScene)
            } else {
                Log.w("PlaybackPage", "Scene $sceneId not found")
                Toast.makeText(context, "Scene $sceneId not found", Toast.LENGTH_LONG).show()
            }
        }
    }

    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val controllerViewState =
        remember {
            ControllerViewState(
                prefs.getInt(
                    "controllerShowTimeoutMs",
                    3_000,
                ),
                true,
            )
        }.also {
            LaunchedEffect(it) {
                it.observe()
            }
        }

    val libVlc = remember { LibVLC(context, mutableListOf("-v")) }
    val mediaPlayer = remember(libVlc) { MediaPlayer(libVlc) }

    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(mediaPlayer) {
        mediaPlayer.setEventListener(
            object : MediaPlayer.EventListener {
                override fun onEvent(event: MediaPlayer.Event) {
                    Log.v(TAG, "onEvent: 0x${event.type.toString(16)}")
                    when (event.type) {
                        MediaPlayer.Event.Playing -> isPlaying = true
                        MediaPlayer.Event.Paused -> isPlaying = false
                    }
                }
            },
        )
    }

    val focusRequester = remember { FocusRequester() }

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
        skipPosition = mediaPlayer.time
    }
    val playbackKeyHandler =
        remember {
            VlcPlaybackKeyHandler(
                player = mediaPlayer,
                controlsEnabled = true,
                skipWithLeftRight = true,
                nextWithUpDown = false,
                controllerViewState = controllerViewState,
                updateSkipIndicator = updateSkipIndicator,
                seekBackIncrement = 10_000,
                seekForwardIncrement = 30_000,
            )
        }

    scene?.let {
        val media = remember { Media(libVlc, currentScene!!.streamUrl!!.toUri()) }
        LaunchedEffect(Unit) {
            focusRequester.tryRequestFocus()
        }
        Box(
            modifier =
                modifier
                    .background(Color.Black)
                    .onKeyEvent(playbackKeyHandler::onKeyEvent)
                    .focusRequester(focusRequester)
                    .focusable(),
        ) {
            AndroidView(
                factory = {
                    val vlcLayout = VLCVideoLayout(it)
                    mediaPlayer.attachViews(vlcLayout, null, true, false)
                    vlcLayout
                },
                update = {
                    mediaPlayer.media = media
                    media.release()
                    mediaPlayer.play()

                    // TODO this doesn't work?
                    if (startPosition > 0) mediaPlayer.time = startPosition
                },
                modifier = Modifier.fillMaxSize(),
            )

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
                val showSkipProgress = true
                if (showSkipProgress) {
                    currentScene?.duration?.let {
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
                        scene = it,
                        tracks = listOf(),
                        captions = listOf(),
                        markers = listOf(),
                        streamDecision = null,
                        oCounter = it.oCounter ?: 0,
                        playerControls = VlcPlayerControls(mediaPlayer, 10_000, 30_000),
                        onPlaybackActionClick = {
                            when (it) {
                                PlaybackAction.CreateMarker -> {
                                }

                                PlaybackAction.OCount -> {
                                }

                                PlaybackAction.ShowDebug -> {
                                }

                                PlaybackAction.ShowVideoFilterDialog -> {}

                                PlaybackAction.ShowPlaylist -> {
                                }

                                is PlaybackAction.ToggleCaptions -> {
                                }

                                is PlaybackAction.PlaybackSpeed -> {}
                                is PlaybackAction.Scale -> {}
                                is PlaybackAction.ToggleAudio -> {
                                }

                                PlaybackAction.ShowSceneDetails -> {
                                }
                            }
                        },
                        onSeekBarChange = {
                        },
                        controllerViewState = controllerViewState,
                        showPlay = !isPlaying,
                        previousEnabled = false,
                        nextEnabled = false,
                        seekEnabled = mediaPlayer.isSeekable,
                        seekPreviewEnabled = true,
                        showDebugInfo = true,
                        spriteImageLoaded = false,
                        moreButtonOptions =
                            MoreButtonOptions(
                                buildMap {
//                                    if (markersEnabled) {
//                                        put("Create Marker", PlaybackAction.CreateMarker)
//                                    }
//                                    if (playlistPager != null && playlistPager.size > 1) {
//                                        put("Show Playlist", PlaybackAction.ShowPlaylist)
//                                    }
//                                    if (useVideoFilters) {
//                                        put("Set video filters", PlaybackAction.ShowVideoFilterDialog)
//                                    }
                                    put("Details", PlaybackAction.ShowSceneDetails)
                                },
                            ),
                        subtitleIndex = null,
                        audioIndex = null,
                        audioOptions = listOf(),
                        playbackSpeed = 1.0f,
                        scale = ContentScale.Fit,
                        playlistInfo = null,
                    )
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.stop()
            mediaPlayer.release()
            libVlc.release()
        }
    }
}

class VlcPlaybackKeyHandler(
    private val player: MediaPlayer,
    private val controlsEnabled: Boolean,
    private val skipWithLeftRight: Boolean,
    private val nextWithUpDown: Boolean,
    private val controllerViewState: ControllerViewState,
    private val updateSkipIndicator: (Long) -> Unit,
    private val seekBackIncrement: Long = 10.seconds.inWholeMilliseconds,
    private val seekForwardIncrement: Long = 30.seconds.inWholeMilliseconds,
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
                    updateSkipIndicator(-seekBackIncrement)
                    player.time = player.time - seekBackIncrement
                } else if (skipWithLeftRight && it.key == Key.DirectionRight) {
                    player.time = player.time + seekForwardIncrement
                    updateSkipIndicator(seekForwardIncrement)
                } else if (nextWithUpDown && it.key == Key.DirectionUp) {
//                    player.seekToPreviousMediaItem()
                    // TODO
                } else if (nextWithUpDown && it.key == Key.DirectionDown) {
//                    player.seekToNextMediaItem()
                    // TODO
                } else {
                    controllerViewState.showControls()
                }
            } else {
                // When controller is visible, its buttons will handle pulsing
            }
        } else if (isMedia(it)) {
            when (it.key) {
                Key.MediaPlay -> {
                    player.play()
                }

                Key.MediaPause -> {
                    player.pause()
                    controllerViewState.showControls()
                }

                Key.MediaPlayPause -> {
                    if (player.isPlaying) player.pause() else player.play()
                    if (!player.isPlaying) {
                        controllerViewState.showControls()
                    }
                }

                Key.MediaFastForward, Key.MediaSkipForward -> {
                    player.time = player.time + seekForwardIncrement
                    updateSkipIndicator(seekForwardIncrement)
                }

                Key.MediaRewind, Key.MediaSkipBackward -> {
                    player.time = player.time - seekBackIncrement
                    updateSkipIndicator(-seekBackIncrement)
                }

                // TODO
//                Key.MediaNext -> if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT)) player.seekToNext()
//                Key.MediaPrevious -> if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS)) player.seekToPrevious()
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

class VlcPlayerControls(
    private val player: MediaPlayer,
    private val seekBackIncrement: Long,
    private val seekForwardIncrement: Long,
) : PlayerControls {
    override val duration: Long
        get() = player.length
    override val currentPosition: Long
        get() = player.time
    override val bufferedPosition: Long
        get() = player.time

    override fun seekTo(position: Long) {
        player.time = position
    }

    override fun seekBack() {
        player.time = player.time - seekBackIncrement
    }

    override fun seekForward() {
        player.time = player.time + seekForwardIncrement
    }

    override fun seekToPrevious() {
        TODO("Not yet implemented")
    }

    override fun seekToNext() {
        TODO("Not yet implemented")
    }

    override fun hasNextMediaItem(): Boolean = false

    override fun playOrPause() {
        if (player.isPlaying) player.pause() else player.play()
    }
}
