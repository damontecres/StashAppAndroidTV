package com.github.damontecres.stashapp.ui.components.playback

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.webvtt.WebvttParser
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberNextButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.media3.ui.compose.state.rememberPreviousButtonState
import androidx.tv.material3.MaterialTheme
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Scale
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.VideoFilter
import com.github.damontecres.stashapp.data.room.PlaybackEffect
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.playback.PlaylistFragment
import com.github.damontecres.stashapp.playback.TrackActivityPlaybackListener
import com.github.damontecres.stashapp.playback.TrackSupport
import com.github.damontecres.stashapp.playback.TrackSupportReason
import com.github.damontecres.stashapp.playback.TrackType
import com.github.damontecres.stashapp.playback.TranscodeDecision
import com.github.damontecres.stashapp.playback.checkForSupport
import com.github.damontecres.stashapp.playback.maybeMuteAudio
import com.github.damontecres.stashapp.playback.switchToTranscode
import com.github.damontecres.stashapp.proto.PlaybackFinishBehavior
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.compat.detectTvDevice
import com.github.damontecres.stashapp.ui.compat.isNotTvDevice
import com.github.damontecres.stashapp.ui.compat.isTvDevice
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.image.DRAG_THROTTLE_DELAY
import com.github.damontecres.stashapp.ui.components.image.ImageFilterDialog
import com.github.damontecres.stashapp.ui.indexOfFirstOrNull
import com.github.damontecres.stashapp.ui.pages.SearchForDialog
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.findActivity
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.launchDefault
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.util.toLongMilliseconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import timber.log.Timber
import java.util.Locale
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

const val TAG = "PlaybackPageContent"

class PlaybackViewModel :
    ViewModel(),
    Player.Listener {
    private lateinit var server: StashServer
    private lateinit var exceptionHandler: CoroutineExceptionHandler
    private lateinit var uiConfig: ComposeUiConfig
    private var markersEnabled by Delegates.notNull<Boolean>()
    private var saveFilters = true
    private var videoFiltersEnabled = false

    private lateinit var player: Player
    private var trackActivity by Delegates.notNull<Boolean>()
    private var trackActivityListener: TrackActivityPlaybackListener? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private val _info = MutableStateFlow(PlaybackInfo())
    val info: StateFlow<PlaybackInfo> = _info

    val subtitles = MutableStateFlow<List<Cue>>(emptyList())

    @kotlin.OptIn(FlowPreview::class)
    val videoFilter =
        state
            .map { it.videoFilter ?: VideoFilter() }
            .debounce(if (detectTvDevice) 500L else DRAG_THROTTLE_DELAY)
            .stateIn(viewModelScope, SharingStarted.Lazily, VideoFilter())

    fun init(
        server: StashServer,
        player: Player,
        trackActivity: Boolean,
        markersEnabled: Boolean,
        saveFilters: Boolean,
        videoFiltersEnabled: Boolean,
        uiConfig: ComposeUiConfig,
    ) {
        this.server = server
        this.player = player
        this.trackActivity = trackActivity
        this.markersEnabled = markersEnabled
        this.saveFilters = saveFilters
        this.videoFiltersEnabled = videoFiltersEnabled
        this.exceptionHandler = LoggingCoroutineExceptionHandler(server, viewModelScope)
        player.addListener(this)
        addCloseable { player.removeListener(this@PlaybackViewModel) }
        if (trackActivity) {
            addCloseable("tracking") {
                trackActivityListener?.let {
                    it.release(player.currentPosition)
                    StashExoPlayer.removeListener(it)
                }
            }
        }
    }

    private var sceneJob: Job = Job()

    fun changeScene(tag: PlaylistFragment.MediaItemTag) {
        sceneJob.cancelChildren()
        _state.update {
            PlaybackState(
                mediaItemTag = tag,
            )
        }

        if (trackActivity) {
            Timber.v(
                "Setting up activity tracking scene %s, removing=%s",
                tag.item.id,
                trackActivityListener?.scene?.id,
            )
            trackActivityListener?.apply {
                release()
                StashExoPlayer.removeListener(this)
            }
            tag.item.let {
                trackActivityListener =
                    TrackActivityPlaybackListener(
                        server = server,
                        scene = it,
                        getCurrentPosition = {
                            player.currentPosition
                        },
                    )
            }
            trackActivityListener?.let { StashExoPlayer.addListener(it) }
        }

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
                        Timber.d("Loaded VideoFilter for scene %s", tag.item.id)
                        _state.update {
                            if (it.mediaItemTag?.item?.id == tag.item.id) {
                                it.copy(videoFilter = vf.videoFilter)
                            } else {
                                it
                            }
                        }
                    }
                }
            }
        }

        // Fetch preview sprites
        viewModelScope.launch(sceneJob + StashCoroutineExceptionHandler() + Dispatchers.IO) {
            val context = StashApplication.getApplication()
            val imageLoader = SingletonImageLoader.get(context)
            if (tag.item.spriteUrl.isNotNullOrBlank() && tag.item.vttUrl.isNotNullOrBlank()) {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(tag.item.spriteUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .scale(Scale.FILL)
                        .build()
                val result = imageLoader.enqueue(request).job.await()
                if (result.image != null) {
                    val spriteImageLoaded = fetchSprites(tag.item.id, tag.item.vttUrl)
                    _state.update {
                        if (it.mediaItemTag?.item?.id == tag.item.id) {
                            it.copy(spriteImageLoaded = spriteImageLoaded)
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun fetchSprites(
        sceneId: String,
        vttUrl: String,
    ): List<SpriteData> =
        withContext(Dispatchers.Default) {
            val res =
                withContext(Dispatchers.IO) {
                    server.okHttpClient
                        .newCall(
                            Request.Builder().url(vttUrl).build(),
                        ).execute()
                }
            if (res.isSuccessful) {
                res.body.use {
                    it?.bytes()?.let {
                        try {
                            val baseUrl = StashClient.getServerRoot(server.url)
                            val regex = Regex("(\\w+\\.\\w+)#xywh=(\\d+),(\\d+),(\\d+),(\\d+)")
                            val spriteData = mutableListOf<SpriteData>()
                            WebvttParser().parse(it, SubtitleParser.OutputOptions.allCues()) {
                                val start = it.startTimeUs.microseconds
                                val end = it.endTimeUs.microseconds
                                it.cues.firstOrNull()?.text?.let { cue ->
                                    val m = regex.matchEntire(cue)
                                    if (m != null) {
                                        val url = "$baseUrl/scene/${m.groupValues[1]}"
                                        val x = m.groupValues[2].toInt()
                                        val y = m.groupValues[3].toInt()
                                        val w = m.groupValues[4].toInt()
                                        val h = m.groupValues[5].toInt()
                                        val sprite =
                                            SpriteData(
                                                start = start,
                                                end = end,
                                                url = url,
                                                x = x,
                                                y = y,
                                                w = w,
                                                h = h,
                                            )
//                                        Log.v(TAG, "sprite=$sprite")
                                        spriteData.add(sprite)
                                    }
                                }
                            }
                            return@withContext spriteData
                        } catch (ex: Exception) {
                            Timber.w(ex, "Error parsing sprites for %s", sceneId)
                            return@withContext emptyList()
                        }
                    }
                    emptyList()
                }
            } else {
                Timber.d("No sprites for %s", sceneId)
                return@withContext emptyList()
            }
        }

    private fun refreshScene(sceneId: String) {
        // Fetch o-count & markers
        viewModelScope.launch(sceneJob + exceptionHandler) {
            val queryEngine = QueryEngine(server)
            val scene = queryEngine.getScene(sceneId)
            if (scene != null) {
                val markers =
                    if (markersEnabled) {
                        scene.scene_markers
                            .sortedBy { it.seconds }
                            .map(::BasicMarker)
                    } else {
                        emptyList()
                    }
                val performers =
                    if (scene.performers.isNotEmpty()) {
                        queryEngine.findPerformers(performerIds = scene.performers.map { it.id })
                    } else {
                        emptyList()
                    }
                _state.update {
                    if (it.mediaItemTag?.item?.id == sceneId) {
                        it.copy(
                            scene = scene,
                            oCount = scene.o_counter ?: 0,
                            rating100 = scene.rating100 ?: 0,
                            markers = markers,
                            performers = performers,
                        )
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun addMarker(
        position: Long,
        tagId: String,
    ) {
        state.value.mediaItemTag?.let {
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
        viewModelScope.launchIO(exceptionHandler) {
            val mutationEngine = MutationEngine(server)
            val result = mutationEngine.incrementOCounter(sceneId).count
            _state.update { it.copy(oCount = result) }
        }
    }

    fun updateVideoFilter(newFilter: VideoFilter?) {
        _state.update { it.copy(videoFilter = newFilter) }
    }

    fun saveVideoFilter() {
        state.value.mediaItemTag?.item?.let {
            viewModelScope.launchIO(StashCoroutineExceptionHandler(autoToast = true)) {
                val vf = state.value.videoFilter
                if (vf != null) {
                    StashApplication
                        .getDatabase()
                        .playbackEffectsDao()
                        .insert(PlaybackEffect(server.url, it.id, DataType.SCENE, vf))
                    Timber.d("Saved VideoFilter for scene %s", it.id)
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
            _state.update { it.copy(rating100 = newRating) }
            showSetRatingToast(StashApplication.getApplication(), newRating)
        }
    }

    override fun onCues(cueGroup: CueGroup) {
        subtitles.update { cueGroup.cues }
    }

    override fun onTracksChanged(tracks: Tracks) {
        viewModelScope.launchDefault {
            val trackInfo = checkForSupport(tracks)
            val audioTracks =
                trackInfo
                    .filter { it.type == TrackType.AUDIO && it.supported == TrackSupportReason.HANDLED }
            val audioIndex = audioTracks.indexOfFirstOrNull { it.selected }
            val audioOptions =
                audioTracks.map { it.labels.joinToString(", ").ifBlank { "Default" } }
            val captions =
                trackInfo.filter { it.type == TrackType.TEXT && it.supported == TrackSupportReason.HANDLED }
            _info.update {
                it.copy(
                    currentTracks = trackInfo,
                    captions = captions,
                    audioIndex = audioIndex,
                    audioOptions = audioOptions,
                )
            }

            val captionsByDefault =
                uiConfig.preferences.interfacePreferences.captionsByDefault
            if (captionsByDefault && captions.isNotEmpty() && info.value.sceneSubtitlesActivated != state.value.scene?.id) {
                // Captions will be empty when transitioning to new media item
                // Only want to activate subtitles once in case the user turns them off
                val sceneSubtitlesActivated = state.value.scene?.id
                val languageCode = Locale.getDefault().language
                val subtitleIndex =
                    captions.indexOfFirstOrNull { it.format.language == languageCode }?.let {
                        Timber.v("Found default subtitle track for $languageCode: $it")
                        withContext(Dispatchers.Main) {
                            if (toggleSubtitles(player, null, it)) {
                                it
                            } else {
                                null
                            }
                        }
                    }
                _info.update {
                    it.copy(
                        sceneSubtitlesActivated = sceneSubtitlesActivated,
                        subtitleIndex = subtitleIndex,
                    )
                }
            }
        }
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        _info.update { PlaybackInfo(currentPlaylistIndex = player.currentMediaItemIndex) }
        if (mediaItem != null) {
            changeScene(mediaItem.localConfiguration!!.tag as PlaylistFragment.MediaItemTag)
        }
    }

    fun toggleCaptions(index: Int) {
        if (toggleSubtitles(player, info.value.subtitleIndex, index)) {
            _info.update { it.copy(subtitleIndex = index) }
        } else {
            _info.update { it.copy(subtitleIndex = null) }
            subtitles.value = emptyList()
        }
    }

    fun toggleAudion(index: Int) {
        val audioIndex =
            if (toggleAudio(player, info.value.audioIndex, index)) {
                index
            } else {
                null
            }
        _info.update { it.copy(audioIndex = audioIndex) }
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

data class SpriteData(
    val start: Duration,
    val end: Duration,
    val url: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
)

data class PlaybackState(
    val mediaItemTag: PlaylistFragment.MediaItemTag? = null,
    val markers: List<BasicMarker> = emptyList(),
    val performers: List<PerformerData> = emptyList(),
    val oCount: Int = 0,
    val rating100: Int = 0,
    val scene: FullSceneData? = null,
    val videoFilter: VideoFilter? = null,
    val spriteImageLoaded: List<SpriteData> = emptyList(),
)

data class PlaybackInfo(
    val currentPlaylistIndex: Int = 0,
    val currentTracks: List<TrackSupport> = emptyList(),
    val captions: List<TrackSupport> = emptyList(),
    val subtitleIndex: Int? = null,
    val sceneSubtitlesActivated: String? = null,
    val audioIndex: Int? = null,
    val audioOptions: List<String> = emptyList(),
)

@OptIn(UnstableApi::class)
@Composable
fun PlaybackPageContent(
    server: StashServer,
    player: Player,
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
    val state by viewModel.state.collectAsState()
    val currentScene = state.mediaItemTag
    val markers = state.markers
    val oCount = state.oCount
    val rating100 = state.rating100
    val spriteImageLoaded = state.spriteImageLoaded
    val scene = state.scene
    val performers = state.performers

    val info by viewModel.info.collectAsState()
    val subtitles by viewModel.subtitles.collectAsState()
    val videoFilter by viewModel.videoFilter.collectAsState()
    val currentTracks = info.currentTracks
    val captions = info.captions
    val subtitleIndex = info.subtitleIndex
    val audioIndex = info.audioIndex
    val audioOptions = info.audioOptions

    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showSceneDetails by rememberSaveable { mutableStateOf(false) }

    AmbientPlayerListener(player)

    LifecycleStartEffect(Unit) {
        onStopOrDispose {
            savedStartPosition = player.currentPosition
            currentPlaylistIndex = player.currentMediaItemIndex
            StashExoPlayer.releasePlayer()
        }
    }

    var showPlaylist by remember { mutableStateOf(false) }
    var contentScale by remember { mutableStateOf(ContentScale.Fit) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    LaunchedEffect(playbackSpeed) { player.setPlaybackSpeed(playbackSpeed) }

    val presentationState = rememberPresentationState(player)
    val scaledModifier =
        Modifier.resizeWithContentScale(contentScale, presentationState.videoSizeDp)

    var createMarkerPosition by remember { mutableLongStateOf(-1L) }
    var playingBeforeDialog by remember { mutableStateOf(false) }

    var showDebugInfo by remember {
        mutableStateOf(
            uiConfig.preferences.playbackPreferences.showDebugInfo,
        )
    }
    val showSkipProgress = uiConfig.preferences.interfacePreferences.showProgressWhenSkipping
    val skipWithLeftRight = uiConfig.preferences.playbackPreferences.dpadSkipping
    // Enabled if the preference is enabled and playing a playlist of markers
    val nextWithUpDown =
        remember {
            playlistPager != null &&
                playlistPager.size > 1 &&
                uiConfig.preferences.interfacePreferences.useUpDownPreviousNext
        }
    val useVideoFilters = uiConfig.preferences.playbackPreferences.videoFiltersEnabled

    val controllerViewState =
        remember {
            ControllerViewState(
                uiConfig.preferences.playbackPreferences.controllerTimeoutMs,
                controlsEnabled,
            )
        }.also {
            LaunchedEffect(it) {
                it.observe()
            }
        }

    val retryMediaItemIds = remember { mutableSetOf<String>() }

    val isMarkerPlaylist = playlistPager?.filter?.dataType == DataType.MARKER

    val isTvDevice = isTvDevice

    var videoDecoder by remember { mutableStateOf<String?>(null) }
    var audioDecoder by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.init(
            server,
            player,
            uiConfig.preferences.playbackPreferences.savePlayHistory &&
                server.serverPreferences.trackActivity &&
                !isMarkerPlaylist,
            markersEnabled,
            uiConfig.persistVideoFilters,
            useVideoFilters,
            uiConfig,
        )
        viewModel.changeScene(playlist[currentPlaylistIndex].localConfiguration!!.tag as PlaylistFragment.MediaItemTag)
        maybeMuteAudio(uiConfig.preferences, false, player)
        player.setMediaItems(playlist, startIndex, savedStartPosition)
        if (playlistPager == null) {
            player.setupFinishedBehavior(
                uiConfig.preferences.playbackPreferences.playbackFinishBehavior,
                navigationManager,
            ) {
                controllerViewState.showControls()
            }
        }
        StashExoPlayer.addListener(
            StashAnalyticsListener { audio, video ->
                audioDecoder = audio
                videoDecoder = video
            },
        )
        StashExoPlayer.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Timber.e(
                        error,
                        "PlaybackException on scene %s, errorCode=%s",
                        currentScene?.item?.id,
                        error.errorCode,
                    )
                    val showError =
                        when (error.errorCode) {
                            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                            PlaybackException.ERROR_CODE_DECODING_FAILED,
                            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
                            PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED,
                            -> {
                                val current = player.currentMediaItem
                                val currentPosition = player.currentMediaItemIndex
                                if (current != null) {
                                    val tag =
                                        (current.localConfiguration!!.tag as PlaylistFragment.MediaItemTag)
                                    val id = tag.item.id
                                    val isTranscodingOrDirect =
                                        tag.streamDecision.transcodeDecision == TranscodeDecision.Transcode ||
                                            tag.streamDecision.transcodeDecision is TranscodeDecision.ForcedTranscode ||
                                            tag.streamDecision.transcodeDecision is TranscodeDecision.ForcedDirectPlay
                                    if (id !in retryMediaItemIds && !isTranscodingOrDirect) {
                                        retryMediaItemIds.add(id)
                                        val newMediaItem =
                                            switchToTranscode(
                                                context,
                                                current,
                                                uiConfig.preferences.playbackPreferences,
                                            )
                                        val newTag =
                                            newMediaItem.localConfiguration!!.tag as PlaylistFragment.MediaItemTag
                                        Timber.d(
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
                                    Timber.w(
                                        "No current media item, cannot fallback to transcoding",
                                    )
                                    true
                                }
                            }

                            else -> {
                                true
                            }
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
            Timber.d("Enabling video effects")
            (player as? ExoPlayer)?.setVideoEffects(emptyList())
        }
    }

    val windowInsetsController =
        remember {
            context
                .findActivity()
                ?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        }

    if (isNotTvDevice && windowInsetsController != null && controllerViewState.controlsEnabled) {
        if (controllerViewState.controlsVisible) {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    var skipIndicatorDuration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(controllerViewState.controlsVisible) {
        // If controller shows/hides, immediately cancel the skip indicator
        skipIndicatorDuration = 0L
    }
    var skipPosition by remember { mutableLongStateOf(0L) }
    val updateSkipIndicator = { delta: Long ->
        if ((skipIndicatorDuration > 0 && delta < 0) || (skipIndicatorDuration < 0 && delta > 0)) {
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
                isTvDevice = isTvDevice,
                player = player,
                controlsEnabled = controlsEnabled,
                skipWithLeftRight = skipWithLeftRight,
                nextWithUpDown = nextWithUpDown,
                controllerViewState = controllerViewState,
                updateSkipIndicator = updateSkipIndicator,
            )
        }
    val mobileTouchGesturesEnabled = isNotTvDevice && uiConfig.preferences.playbackPreferences.mobileTouchGestures

    Box(
        modifier
            .background(Color.Black)
            .onKeyEvent(playbackKeyHandler::onKeyEvent)
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        if (mobileTouchGesturesEnabled) {
            PlayerSurface(
                player = player,
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                modifier =
                    scaledModifier.then(
                        rememberMobileGestureModifier(
                            player = player,
                            controllerViewState = controllerViewState,
                            updateSkipIndicator = updateSkipIndicator,
                        ),
                    ),
            )
        } else {
            PlayerSurface(
                player = player,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                modifier =
                    scaledModifier.clickable(
                        enabled = !isTvDevice,
                        indication = null,
                        interactionSource = null,
                    ) {
                        controllerViewState.toggleControls()
                    },
            )
        }
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
                currentScene?.item?.duration?.let {
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
            AndroidView(
                factory = {
                    SubtitleView(context).apply {
                        setUserDefaultStyle()
                        setUserDefaultTextSize()
                    }
                },
                update = {
                    it.setCues(subtitles)
                },
                onReset = {
                    it.setCues(null)
                },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
            )
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
                            .padding(WindowInsets.systemBars.asPaddingValues())
                            .fillMaxSize()
                            .background(Color.Transparent),
                    uiConfig = uiConfig,
                    scene = currentScene.item,
                    tracks = currentTracks,
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

                            PlaybackAction.ShowVideoFilterDialog -> {
                                showFilterDialog = true
                            }

                            PlaybackAction.ShowPlaylist -> {
                                if (playlistPager != null && playlistPager.size > 1) {
                                    showPlaylist = true
                                    controllerViewState.hideControls()
                                }
                            }

                            is PlaybackAction.ToggleCaptions -> {
                                viewModel.toggleCaptions(it.index)
                                controllerViewState.hideControls()
                            }

                            is PlaybackAction.PlaybackSpeed -> {
                                playbackSpeed = it.value
                            }

                            is PlaybackAction.Scale -> {
                                contentScale = it.scale
                            }

                            is PlaybackAction.ToggleAudio -> {
                                viewModel.toggleAudion(it.index)
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
                    seekPreviewEnabled = !isMarkerPlaylist,
                    showDebugInfo = showDebugInfo,
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
                    playlistInfo =
                        playlistPager?.let {
                            PlaylistInfo(
                                currentPlaylistIndex,
                                it.size,
                                player.mediaItemCount,
                            )
                        },
                    videoDecoder = videoDecoder,
                    audioDecoder = audioDecoder,
                    spriteData = spriteImageLoaded,
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
        if (useVideoFilters) {
            LaunchedEffect(videoFilter) {
                val effectList = videoFilter.createEffectList()
                Timber.d("Applying %s effects", effectList.size)
                (player as? ExoPlayer)?.setVideoEffects(effectList)
            }
            AnimatedVisibility(showFilterDialog) {
                ImageFilterDialog(
                    filter = videoFilter,
                    showVideoOptions = true,
                    showSaveGalleryButton = false,
                    uiConfig = uiConfig,
                    onChange = viewModel::updateVideoFilter,
                    onClickSave = viewModel::saveVideoFilter,
                    onClickSaveGallery = {},
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
    finishedBehavior: PlaybackFinishBehavior,
    navigationManager: NavigationManager,
    showController: () -> Unit,
) {
    when (finishedBehavior) {
        PlaybackFinishBehavior.REPEAT -> {
            repeatMode = Player.REPEAT_MODE_ONE
        }

        PlaybackFinishBehavior.GO_BACK -> {
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
        }

        PlaybackFinishBehavior.DO_NOTHING -> {
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

        else -> {
            Timber.w("Unknown playbackFinishedBehavior: %s", finishedBehavior)
        }
    }
}

class PlaybackKeyHandler(
    private val isTvDevice: Boolean,
    private val player: Player,
    private val controlsEnabled: Boolean,
    private val skipWithLeftRight: Boolean,
    private val nextWithUpDown: Boolean,
    private val controllerViewState: ControllerViewState,
    private val updateSkipIndicator: (Long) -> Unit,
) {
    private var keyDownKey: Key? = null
    private var holdActionTriggered = false

    fun onKeyEvent(it: KeyEvent): Boolean {
        var result = true
        if (!controlsEnabled) {
            result = false
        } else if (it.type == KeyEventType.KeyDown) {
            if (nextWithUpDown && (it.key == Key.DirectionUp || it.key == Key.DirectionDown)) {
                val repeatCount = it.nativeKeyEvent.repeatCount
                if (keyDownKey == null) {
                    keyDownKey = it.key
                    holdActionTriggered = false
                } else if (keyDownKey == it.key && !holdActionTriggered && repeatCount >= 2) {
                    // Each repeat is roughly 250-300ms, so repeatCount==2 is ~500-600ms
                    holdActionTriggered = true
                    if (!controllerViewState.controlsVisible) {
                        if (it.key == Key.DirectionUp) {
                            player.seekToPreviousMediaItem()
                        } else if (it.key == Key.DirectionDown) {
                            player.seekToNextMediaItem()
                        }
                    }
                }
                result = true
            } else {
                result = false
            }
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
                } else if (nextWithUpDown && (it.key == Key.DirectionUp || it.key == Key.DirectionDown)) {
                    val wasHeld = keyDownKey == it.key && holdActionTriggered
                    keyDownKey = null
                    holdActionTriggered = false
                    if (!wasHeld) {
                        // Only show player controls if it was a short press (not a hold)
                        controllerViewState.showControls()
                    }
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
                    val wasPlaying = player.isPlaying
                    Util.handlePlayPauseButtonAction(player)
                    if (wasPlaying) {
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

                Key.MediaNext -> {
                    if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT)) player.seekToNext()
                }

                Key.MediaPrevious -> {
                    if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS)) player.seekToPrevious()
                }

                else -> {
                    result = false
                }
            }
        } else if (it.key == Key.Enter && !controllerViewState.controlsVisible) {
            controllerViewState.showControls()
        } else if (it.key == Key.Back && controllerViewState.controlsVisible) {
            // TODO change this to a BackHandler?
            controllerViewState.hideControls()
            if (!isTvDevice) {
                // Allow to propagate up
                result = false
            }
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
    if (index !in subtitleTracks.indices || (currentActiveSubtitleIndex != null && currentActiveSubtitleIndex == index)) {
        Timber.v("Deactivating subtitles")
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        return false
    } else {
        Timber.v("Activating subtitle %s", subtitleTracks[index].mediaTrackGroup.id)
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
    if (index !in audioTracks.indices || (audioIndex != null && audioIndex == index)) {
        Timber.v("Deactivating audio")
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
        return false
    } else {
        Timber.v("Activating audio %s", audioTracks[index].mediaTrackGroup.id)
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
