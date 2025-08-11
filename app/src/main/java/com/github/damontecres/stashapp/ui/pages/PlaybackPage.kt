package com.github.damontecres.stashapp.ui.pages

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.playback.PlaylistFragment
import com.github.damontecres.stashapp.playback.buildMediaItem
import com.github.damontecres.stashapp.playback.getStreamDecision
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FilterViewModel
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.playback.PlaybackPageContent
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.SkipParams
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlaybackPage(
    server: StashServer,
    uiConfig: ComposeUiConfig,
    sceneId: String,
    startPosition: Long,
    playbackMode: PlaybackMode,
    itemOnClick: ItemOnClicker<Any>,
    modifier: Modifier = Modifier,
) {
    var scene by remember { mutableStateOf<FullSceneData?>(null) }
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
            } else {
                Log.w("PlaybackPage", "Scene $sceneId not found")
                Toast.makeText(context, "Scene $sceneId not found", Toast.LENGTH_LONG).show()
            }
        }
    }
    Log.d("PlaybackPage", "scene=${scene?.id}")
    scene?.let {
        val player =
            remember {
                val skipParams =
                    uiConfig.preferences.playbackPreferences.let {
                        SkipParams.Values(
                            it.skipForwardMs,
                            it.skipBackwardMs,
                        )
                    }

                StashExoPlayer.getInstance(context, server, skipParams).apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = true
                }
            }
        val playbackScene = remember { Scene.fromFullSceneData(it) }
        val decision = remember { getStreamDecision(context, playbackScene, playbackMode) }
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
const val PLAYLIST_THRESHOLD = 10
const val PLAYLIST_PREFETCH = 15

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
        // TODO switch to single query
        viewModel.setFilter(server, adjustFilter(filterArgs), uiConfig.cardSettings.columns)
        playlistViewModel.setFilter(server, filterArgs, uiConfig.cardSettings.columns)
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
                val skipForward =
                    uiConfig.preferences.playbackPreferences.skipForwardMs.milliseconds
                val skipBack =
                    uiConfig.preferences.playbackPreferences.skipBackwardMs.milliseconds
                val skipParams =
                    if (viewModel.dataType == DataType.MARKER) {
                        // Override the skip forward/back since many users will have default seeking values larger than the duration
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
                StashExoPlayer.getInstance(context, server, skipParams).apply {
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
                        scope.launch(LoggingCoroutineExceptionHandler(server, scope)) {
                            mutex.withLock {
                                val currentIndex = player.currentMediaItemIndex
                                val count = player.mediaItemCount
                                pager?.let { pager ->
                                    if (count - currentIndex < PLAYLIST_THRESHOLD && pager.size > count) {
                                        val maxIndex =
                                            (currentIndex + PLAYLIST_PREFETCH)
                                                .coerceAtMost(pager.size)
                                        val newMediaItems =
                                            (currentIndex..<maxIndex).mapNotNull { index ->
                                                pager.getBlocking(index)?.let { item ->
                                                    convertToMediaItem(
                                                        context,
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
            server = server,
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
                    scope.launch(LoggingCoroutineExceptionHandler(server, scope)) {
                        mutex.withLock {
                            val count = player.mediaItemCount
                            pager?.let { pager ->
                                val newMediaItems =
                                    (count..<(index + PLAYLIST_PREFETCH).coerceAtMost(pager.size)).mapNotNull { index ->
                                        pager.getBlocking(index)?.let { item ->
                                            convertToMediaItem(
                                                context,
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
    dataType: DataType,
    clipDuration: Duration,
    item: StashData,
): MediaItem {
    if (dataType == DataType.SCENE) {
        item as VideoSceneData
        val scene = Scene.fromVideoSceneData(item)
        val decision = getStreamDecision(context, scene, PlaybackMode.Choose)
        return buildMediaItem(context, decision, scene) {
            setTag(PlaylistFragment.MediaItemTag(scene, decision))
        }
    } else {
        // Markers
        item as FullMarkerData
        val scene = Scene.fromMarkerData(item)
        val decision = getStreamDecision(context, scene, PlaybackMode.Choose)
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
