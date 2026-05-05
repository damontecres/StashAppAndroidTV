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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.github.damontecres.stashapp.ui.util.OneTimeLaunchedEffect
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.SkipParams
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

import android.widget.Toast
import com.github.damontecres.stashapp.playback.StreamDecision
import com.github.damontecres.stashapp.playback.buildUnresolvedMediaItem
import java.util.concurrent.ConcurrentHashMap

@Composable
fun PlaybackPage(
    server: StashServer,
    uiConfig: ComposeUiConfig,
    sceneId: String,
    startPosition: Long,
    playbackMode: PlaybackMode,
    itemOnClick: ItemOnClicker<Any>,
    modifier: Modifier = Modifier,
    viewModel: PlaybackPageViewModel = viewModel(),
) {
    OneTimeLaunchedEffect { viewModel.init(server, sceneId) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

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
                        skipParams,
                        httpClient.name,
                        debugLogging,
                        backend,
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
            server = server,
            player = player,
            playlist = listOf(media),
            startIndex = 0,
            uiConfig = uiConfig,
            markersEnabled = true,
            playlistPager = null,
            modifier =
                Modifier
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
const val PLAYLIST_THRESHOLD = 15
// Window of items loaded around the startIndex on first load
const val PLAYLIST_WINDOW = 10

@Composable
fun PlaylistPlaybackPage(
    server: StashServer,
    uiConfig: ComposeUiConfig,
    filterArgs: FilterArgs,
    startIndex: Int,
    itemOnClick: ItemOnClicker<Any>,
    modifier: Modifier = Modifier,
    clipDuration: Duration = 30.seconds,
    viewModel: FilterViewModel = viewModel(key = "main"),
    playlistViewModel: FilterViewModel = viewModel(key = "playlist"),
) {
    val scope = rememberCoroutineScope()
    Log.v("PlaybackPageContent", "startIndex=$startIndex")
    val context = LocalContext.current

    LaunchedEffect(server, filterArgs) {
        val adjustedFilter = adjustFilter(filterArgs)
        viewModel.setFilter(server, adjustedFilter, uiConfig.cardSettings.columns)
        playlistViewModel.setFilter(server, adjustedFilter, uiConfig.cardSettings.columns)
    }
    val pager by viewModel.pager.observeAsState()
    val playlistPager by playlistViewModel.pager.observeAsState()

    // Caches in ViewModel survive recomposition
    val streamDecisionCache = viewModel.streamDecisionCache
    val codecSupport = viewModel.codecSupport ?: run {
        val cs = CodecSupport.getSupportedCodecs(context)
        viewModel.codecSupport = cs
        cs
    }

    // isBuildingPlaylist is true from the start until playlist is ready
    var isBuildingPlaylist by remember { mutableStateOf(true) }

    val player = remember {
        val skipForward = uiConfig.preferences.playbackPreferences.skipForwardMs.milliseconds
        val skipBack = uiConfig.preferences.playbackPreferences.skipBackwardMs.milliseconds
        val skipParams = if (viewModel.dataType == DataType.MARKER) {
            SkipParams.Values(
                (clipDuration / 4).coerceAtMost(skipForward).inWholeMilliseconds,
                (clipDuration / 4).coerceAtMost(skipBack).inWholeMilliseconds,
            )
        } else {
            SkipParams.Values(
                skipForward.inWholeMilliseconds,
                skipBack.inWholeMilliseconds,
            )
        }
        val httpClient = uiConfig.preferences.playbackPreferences.playbackHttpClient
        val debugLogging = uiConfig.preferences.playbackPreferences.debugLoggingEnabled
        StashExoPlayer.getInstance(context, server, skipParams, httpClient.name, debugLogging).apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    // playlist stores only the loaded window; playerOffset tracks where in the full list this window starts
    val playlist = remember(pager) { mutableStateListOf<MediaItem>() }
    // playerOffset: the absolute index in pager[] that corresponds to player.mediaItem[0]
    var playerOffset by remember(pager) { mutableStateOf(0) }

    val resolveMediaItemAt: (Int) -> Unit = remember(player, codecSupport, streamDecisionCache) {
        { playerIndex: Int ->
            if (playerIndex >= 0 && playerIndex < player.mediaItemCount) {
                scope.launch {
                    val mediaItem = player.getMediaItemAt(playerIndex)
                    val tag = mediaItem.localConfiguration?.tag as? PlaylistFragment.MediaItemTag
                    if (tag != null && tag.streamDecision == null) {
                        val scene = tag.item
                        val cached = streamDecisionCache[scene.id]
                        val decision = if (cached != null) {
                            Log.v("PlaybackPage", "Cache hit for sceneId=${scene.id}")
                            cached
                        } else {
                            Log.v("PlaybackPage", "Cache miss for sceneId=${scene.id}")
                            val d = getStreamDecision(
                                context, scene, PlaybackMode.Choose,
                                uiConfig.preferences.playbackPreferences.streamChoice,
                                uiConfig.preferences.playbackPreferences.transcodeAboveResolution,
                                codecSupport
                            )
                            streamDecisionCache[scene.id] = d
                            d
                        }
                        val resolvedItem = buildMediaItem(context, decision, scene) {
                            setMediaMetadata(mediaItem.mediaMetadata)
                            val config = mediaItem.clippingConfiguration
                            setClipStartPositionMs(config.startPositionMs)
                            setClipEndPositionMs(config.endPositionMs)
                            setClipRelativeToDefaultPosition(config.relativeToDefaultPosition)
                            setClipStartsAtKeyFrame(config.startsAtKeyFrame)
                            setTag(PlaylistFragment.MediaItemTag(scene, decision))
                        }
                        if (playerIndex < player.mediaItemCount &&
                            player.getMediaItemAt(playerIndex).mediaId == resolvedItem.mediaId) {
                            player.replaceMediaItem(playerIndex, resolvedItem)
                        }
                    }
                }
            }
        }
    }

    // KEY FIX: Load only a small window around startIndex instead of all items from 0.
    // This reduces initial network queries from O(startIndex/pageSize) to O(1).
    LaunchedEffect(pager) {
        val p = pager ?: return@LaunchedEffect
        val total = p.size
        if (total == 0) return@LaunchedEffect

        // Window: load [windowStart, windowEnd) around startIndex
        val windowStart = maxOf(0, startIndex - PLAYLIST_WINDOW)
        val windowEnd = minOf(total, startIndex + PLAYLIST_WINDOW + 1)

        playerOffset = windowStart

        val items = buildList {
            for (i in windowStart until windowEnd) {
                p.getBlocking(i)?.let { item ->
                    add(convertToUnresolvedMediaItem(context, filterArgs.dataType, item))
                }
            }
        }
        playlist.addAll(items)

        // Resolve only the first playback item immediately
        val playerStartIndex = startIndex - windowStart
        resolveMediaItemAt(playerStartIndex)
        isBuildingPlaylist = false
    }

    if (playlist.isNotEmpty()) {
        val mutex = remember { Mutex() }

        // Append next items when approaching end
        LaunchedEffect(Unit) {
            StashExoPlayer.addListener(
                object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val currentPlayerIndex = player.currentMediaItemIndex
                        val currentAbsoluteIndex = playerOffset + currentPlayerIndex

                        // Resolve stream for current + neighbours
                        scope.launch {
                            resolveMediaItemAt(currentPlayerIndex)
                            resolveMediaItemAt(currentPlayerIndex + 1)
                            if (currentPlayerIndex > 0) resolveMediaItemAt(currentPlayerIndex - 1)
                        }

                        // Append forward if near end of loaded window
                        scope.launch(LoggingCoroutineExceptionHandler(server, scope)) {
                            mutex.withLock {
                                val count = player.mediaItemCount
                                pager?.let { pager ->
                                    val absoluteEnd = playerOffset + count
                                    if (count - currentPlayerIndex < PLAYLIST_THRESHOLD && absoluteEnd < pager.size) {
                                        val fetchEnd = minOf(absoluteEnd + PLAYLIST_WINDOW + 1, pager.size)
                                        val newItems = (absoluteEnd until fetchEnd).mapNotNull { i ->
                                            pager.getBlocking(i)?.let { item ->
                                                convertToUnresolvedMediaItem(context, filterArgs.dataType, item)
                                            }
                                        }
                                        if (newItems.isNotEmpty()) {
                                            playlist.addAll(newItems)
                                            player.addMediaItems(newItems)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            )
        }

        val playerStartIndex = (startIndex - playerOffset).coerceAtLeast(0)
        PlaybackPageContent(
            server = server,
            player = player,
            playlist = playlist,
            startIndex = playerStartIndex,
            uiConfig = uiConfig,
            markersEnabled = filterArgs.dataType == DataType.SCENE,
            playlistPager = playlistPager,
            itemOnClick = itemOnClick,
            onClickPlaylistItem = { index ->
                val absoluteIndex = playerOffset + index
                if (index < player.mediaItemCount) {
                    player.seekTo(index, C.TIME_UNSET)
                } else {
                    scope.launch(LoggingCoroutineExceptionHandler(server, scope)) {
                        mutex.withLock {
                            val count = player.mediaItemCount
                            pager?.let { pager ->
                                val fetchEnd = minOf(absoluteIndex + PLAYLIST_WINDOW + 1, pager.size)
                                val fetchStart = playerOffset + count
                                if (fetchStart < fetchEnd) {
                                    val newItems = (fetchStart until fetchEnd).mapNotNull { i ->
                                        pager.getBlocking(i)?.let { item ->
                                            convertToUnresolvedMediaItem(context, filterArgs.dataType, item)
                                        }
                                    }
                                    player.addMediaItems(newItems)
                                }
                                player.seekTo(index, C.TIME_UNSET)
                            }
                        }
                    }
                }
            },
            modifier = modifier,
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            CircularProgress()
            if (isBuildingPlaylist) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Building playlist...", color = Color.White)
            }
        }
    }
}

/**
 * Converts a [VideoSceneData] or [FullMarkerData] to an unresolved [MediaItem]
 */
private fun convertToUnresolvedMediaItem(
    context: Context,
    dataType: DataType,
    item: StashData,
): MediaItem {
    if (dataType == DataType.SCENE) {
        item as VideoSceneData
        val scene = Scene.fromVideoSceneData(item)
        return buildUnresolvedMediaItem(context, scene) {
            setTag(PlaylistFragment.MediaItemTag(scene, null))
        }
    } else {
        // Markers
        item as FullMarkerData
        val scene = Scene.fromMarkerData(item)
        return buildUnresolvedMediaItem(context, scene) {
            setTag(PlaylistFragment.MediaItemTag(scene, null))
            val startPos = item.seconds.seconds.inWholeMilliseconds.coerceAtLeast(0L)
            // Note: clipping end is not strictly needed for unresolved items but kept for consistency
            val clipConfig = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startPos)
                .build()
            setClippingConfiguration(clipConfig)
        }
    }
}
