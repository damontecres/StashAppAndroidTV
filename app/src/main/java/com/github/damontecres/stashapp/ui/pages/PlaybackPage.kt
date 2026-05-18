package com.github.damontecres.stashapp.ui.pages

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.di.server.CurrentServer
import com.github.damontecres.stashapp.playback.CodecSupport
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.playback.PlaylistFragment
import com.github.damontecres.stashapp.playback.buildMediaItem
import com.github.damontecres.stashapp.playback.getStreamDecision
import com.github.damontecres.stashapp.proto.PlaybackBackend
import com.github.damontecres.stashapp.proto.PlaybackPreferences
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FilterViewModel
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.playback.PlaybackPageContent
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.SkipParams
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlaybackPage(
    uiConfig: ComposeUiConfig,
    sceneId: String,
    startPosition: Long,
    playbackMode: PlaybackMode,
    itemOnClick: ItemOnClicker<Any>,
    modifier: Modifier = Modifier,
    viewModel: PlaybackPageViewModel =
        koinViewModel {
            parametersOf(sceneId)
        },
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val currentServer by viewModel.currentServer.collectAsState()

    val playbackMode =
        remember(playbackMode, uiConfig) {
            if (uiConfig.preferences.playbackPreferences.playbackBackend == PlaybackBackend.MPV) {
                PlaybackMode.ForcedDirectPlay
            } else {
                playbackMode
            }
        }
    state?.let { state ->
        val player =
            remember {
                TODO()
                val skipParams =
                    uiConfig.preferences.playbackPreferences.let {
                        SkipParams.Values(
                            it.skipForwardMs,
                            it.skipBackwardMs,
                        )
                    }
                val httpClient = uiConfig.preferences.playbackPreferences.playbackHttpClient
                val debugLogging = uiConfig.preferences.playbackPreferences.debugLoggingEnabled
                val backend = uiConfig.preferences.playbackPreferences.playbackBackend
                StashExoPlayer
                    .getInstance(
                        context,
                        server,
                        uiConfig.preferences.playbackPreferences,
                    ).apply {
                        repeatMode = Player.REPEAT_MODE_OFF
                        playWhenReady = true
                    }
            }
        val playbackScene = state.scene
        val decision =
            remember {
                getStreamDecision(
                    context,
                    playbackScene,
                    playbackMode,
                    uiConfig.preferences.playbackPreferences.streamChoice,
                    uiConfig.preferences.playbackPreferences.transcodeAboveResolution,
                    CodecSupport.getSupportedCodecs(uiConfig.preferences.playbackPreferences),
                )
            }
        val media =
            remember {
                buildMediaItem(context, decision, playbackScene) {
                    setTag(PlaylistFragment.MediaItemTag(playbackScene, decision))
                }
            }

        PlaybackPageContent(
            currentServer = currentServer,
            player = player,
            playlist = listOf(media),
            startIndex = 0,
            uiConfig = uiConfig,
            markersEnabled = true,
            playlistPager = null,
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
            controlsEnabled = true,
            startPosition = startPosition,
            onClickPlaylistItem = null,
            itemOnClick = itemOnClick,
        )
    }
}

private fun adjustFilter(filter: FilterArgs): FilterArgs =
    if (filter.dataType == DataType.SCENE) {
        val objectFilter =
            AlphabetSearchUtils.findNullAndFilter(
                filter.objectFilter as SceneFilterType? ?: SceneFilterType(),
            )
        // Playlist cannot contain scenes with no files, so modify the filter
        val newObjectFilter =
            objectFilter.copy(
                AND =
                    Optional.present(
                        SceneFilterType(
                            file_count =
                                Optional.present(
                                    IntCriterionInput(
                                        modifier = CriterionModifier.GREATER_THAN,
                                        value = 0,
                                    ),
                                ),
                        ),
                    ),
            )

        filter.copy(
            objectFilter = newObjectFilter,
            override = DataSupplierOverride.Playlist,
        )
    } else {
        filter.copy(override = DataSupplierOverride.Playlist)
    }

const val MAX_PLAYLIST_SIZE = 50
const val PLAYLIST_THRESHOLD = 10
const val PLAYLIST_PREFETCH = 15

@Composable
fun PlaylistPlaybackPage(
    currentServer: CurrentServer,
    uiConfig: ComposeUiConfig,
    filterArgs: FilterArgs,
    startIndex: Int,
    itemOnClick: ItemOnClicker<Any>,
    modifier: Modifier = Modifier,
    clipDuration: Duration = 30.seconds,
    viewModel: FilterViewModel = koinViewModel(key = "main"),
    playlistViewModel: FilterViewModel = koinViewModel(key = "playlist"),
) {
    val scope = rememberCoroutineScope()
    Log.v("PlaybackPageContent", "startIndex=$startIndex")
    val context = LocalContext.current

    LaunchedEffect(currentServer, filterArgs) {
        // TODO switch to single query
        viewModel.setFilter(adjustFilter(filterArgs), uiConfig.cardSettings.columns)
        playlistViewModel.setFilter(filterArgs, uiConfig.cardSettings.columns)
    }
    val pager by viewModel.pager.observeAsState()
//    var playlist by remember(pager) { mutableStateOf<List<MediaItem>>(listOf()) }
    val playlist = remember(pager) { mutableStateListOf<MediaItem>() }
    val playlistPager by playlistViewModel.pager.observeAsState()
    LaunchedEffect(pager) {
        val items =
            pager?.let {
                buildList {
                    for (i in 0..<(it.size).coerceAtMost(MAX_PLAYLIST_SIZE)) {
                        it.getBlocking(i)?.let { item ->
                            add(
                                convertToMediaItem(
                                    context,
                                    uiConfig.preferences.playbackPreferences,
                                    filterArgs.dataType,
                                    clipDuration,
                                    item,
                                ),
                            )
                        }
                    }
                }
            } ?: listOf()
        playlist.addAll(items)
    }
    if (playlist.isNotEmpty()) {
        val player =
            remember {
                StashExoPlayer
                    .getInstance(context, currentServer, uiConfig.preferences.playbackPreferences)
                    .apply {
                        repeatMode = Player.REPEAT_MODE_OFF
                        playWhenReady = true
                    }
            }
        val mutex = remember { Mutex() }
        LaunchedEffect(Unit) {
            StashExoPlayer.addListener(
                object : Player.Listener {
                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int,
                    ) {
                        scope.launch(LoggingCoroutineExceptionHandler(currentServer, scope)) {
                            mutex.withLock {
                                val currentIndex = player.currentMediaItemIndex
                                val count = player.mediaItemCount
                                pager?.let { pager ->
                                    if (count - currentIndex < PLAYLIST_THRESHOLD && pager.size > count) {
                                        val maxIndex =
                                            (count + PLAYLIST_PREFETCH)
                                                .coerceAtMost(pager.size)
                                        val newMediaItems =
                                            (count..<maxIndex).mapNotNull { index ->
                                                pager.getBlocking(index)?.let { item ->
                                                    convertToMediaItem(
                                                        context,
                                                        uiConfig.preferences.playbackPreferences,
                                                        filterArgs.dataType,
                                                        clipDuration,
                                                        item,
                                                    )
                                                }
                                            }
                                        playlist.addAll(newMediaItems)
                                        player.addMediaItems(newMediaItems)
                                    }
                                }
                            }
                        }
                    }
                },
            )
        }

        PlaybackPageContent(
            currentServer = currentServer,
            player = player,
            playlist = playlist,
            startIndex = startIndex,
            uiConfig = uiConfig,
            markersEnabled = filterArgs.dataType == DataType.SCENE,
            playlistPager = playlistPager,
            itemOnClick = itemOnClick,
            onClickPlaylistItem = { index ->
                if (index < player.mediaItemCount) {
                    player.seekTo(index, C.TIME_UNSET)
                } else {
                    scope.launch(LoggingCoroutineExceptionHandler(currentServer, scope)) {
                        mutex.withLock {
                            val count = player.mediaItemCount
                            pager?.let { pager ->
                                val newMediaItems =
                                    (count..<(index + PLAYLIST_PREFETCH).coerceAtMost(pager.size)).mapNotNull { index ->
                                        pager.getBlocking(index)?.let { item ->
                                            convertToMediaItem(
                                                context,
                                                uiConfig.preferences.playbackPreferences,
                                                filterArgs.dataType,
                                                clipDuration,
                                                item,
                                            )
                                        }
                                    }
                                player.addMediaItems(newMediaItems)
                                player.seekTo(index, C.TIME_UNSET)
                            }
                        }
                    }
                }
            },
            modifier = modifier,
        )
    } else {
        CircularProgress()
    }
}

/**
 * Converts a [VideoSceneData] or [FullMarkerData] to a [MediaItem]
 */
private fun convertToMediaItem(
    context: Context,
    prefs: PlaybackPreferences,
    dataType: DataType,
    clipDuration: Duration,
    item: StashData,
): MediaItem {
    if (dataType == DataType.SCENE) {
        item as VideoSceneData
        val scene = Scene.fromVideoSceneData(item)
        val decision =
            getStreamDecision(
                context,
                scene,
                PlaybackMode.Choose,
                prefs.streamChoice,
                prefs.transcodeAboveResolution,
                CodecSupport.getSupportedCodecs(prefs),
            )
        return buildMediaItem(context, decision, scene) {
            setTag(PlaylistFragment.MediaItemTag(scene, decision))
        }
    } else {
        // Markers
        item as FullMarkerData
        val scene = Scene.fromMarkerData(item)
        val decision =
            getStreamDecision(
                context,
                scene,
                PlaybackMode.Choose,
                prefs.streamChoice,
                prefs.transcodeAboveResolution,
                CodecSupport.getSupportedCodecs(prefs),
            )
        val mediaItem =
            buildMediaItem(context, decision, scene) {
                setTag(PlaylistFragment.MediaItemTag(scene, decision))
                val startPos =
                    item.seconds.seconds.inWholeMilliseconds
                        .coerceAtLeast(0L)
                val endPos =
                    item.end_seconds?.seconds?.inWholeMilliseconds
                        ?: (startPos + clipDuration.inWholeMilliseconds)
                val clipConfig =
                    MediaItem.ClippingConfiguration
                        .Builder()
                        .setStartPositionMs(startPos)
                        .setEndPositionMs(endPos)
                        .build()
                setClippingConfiguration(clipConfig)
            }
        return mediaItem
    }
}
