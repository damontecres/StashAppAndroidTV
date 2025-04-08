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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.playback.PlaylistFragment
import com.github.damontecres.stashapp.playback.TrackActivityPlaybackListener
import com.github.damontecres.stashapp.playback.maybeMuteAudio
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.pages.SearchForDialog
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.views.models.EqualityMutableLiveData
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.milliseconds

class PlaylistViewModel : ViewModel() {
    private lateinit var server: StashServer
    private var markersEnabled by Delegates.notNull<Boolean>()

    val mediaItemTag = EqualityMutableLiveData<PlaylistFragment.MediaItemTag>()
    val markers = MutableLiveData<List<MarkerData>>(listOf())

    fun init(
        server: StashServer,
        markersEnabled: Boolean,
    ) {
        this.server = server
        this.markersEnabled = markersEnabled
    }

    fun changeScene(tag: PlaylistFragment.MediaItemTag) {
        this.mediaItemTag.value = tag
        this.markers.value = listOf()
        if (markersEnabled) {
            viewModelScope.launch {
                val queryEngine = QueryEngine(server)
                markers.value =
                    queryEngine
                        .findMarkers(
                            markerFilter =
                                SceneMarkerFilterType(
                                    scenes =
                                        Optional.present(
                                            MultiCriterionInput(
                                                value = Optional.present(listOf(tag.item.id)),
                                                modifier = CriterionModifier.INCLUDES,
                                            ),
                                        ),
                                ),
                        ).sortedBy { it.seconds }
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
                // TODO use newMarker instead of querying again?
                // Refresh markers
                changeScene(it)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlaylistPlaybackPageContent(
    server: StashServer,
    playlist: List<MediaItem>,
    uiConfig: ComposeUiConfig,
    markersEnabled: Boolean,
    playlistPager: ComposePager<StashData>?,
    modifier: Modifier = Modifier,
    controlsEnabled: Boolean = true,
    viewModel: PlaylistViewModel = viewModel(),
) {
    if (playlist.isEmpty()) {
        return
    }
    val context = LocalContext.current
    val currentScene by viewModel.mediaItemTag.observeAsState(playlist[0].localConfiguration!!.tag as PlaylistFragment.MediaItemTag)
    val markers by viewModel.markers.observeAsState(listOf())
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
    LaunchedEffect(Unit) {
        viewModel.init(server, markersEnabled)
        maybeMuteAudio(context, player)
        player.setMediaItems(playlist)
        player.prepare()
        StashExoPlayer.addListener(
            object : Player.Listener {
                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    if (mediaItem != null) {
                        viewModel.changeScene(mediaItem.localConfiguration!!.tag as PlaylistFragment.MediaItemTag)
                    }
                }
            },
        )
    }

    var showControls by remember { mutableStateOf(true) }
    var showPlaylist by remember { mutableStateOf(false) }
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

    val controllerViewState =
        remember { ControllerViewState(3, controlsEnabled) }.also {
            LaunchedEffect(it) {
                it.observe()
            }
        }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
        currentScene?.let {
            PlaybackOverlay(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                uiConfig = uiConfig,
                server = server,
                scene = currentScene.item,
                markers = markers,
                streamDecision = currentScene.streamDecision,
                oCounter = 0, // TODO
                player = player,
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
                        }

                        PlaybackAction.ShowDebug -> {
                            showDebugInfo = !showDebugInfo
                        }

                        PlaybackAction.ShowPlaylist -> {
                            showPlaylist = true
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
