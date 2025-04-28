package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.FullSceneData
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
import com.github.damontecres.stashapp.ui.components.playback.PlaybackPageContent
import com.github.damontecres.stashapp.ui.components.playback.PlaylistPlaybackPageContent
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlaybackPage(
    server: StashServer,
    uiConfig: ComposeUiConfig,
    sceneId: String,
    startPosition: Long,
    playbackMode: PlaybackMode,
    modifier: Modifier = Modifier,
) {
    var scene by remember { mutableStateOf<FullSceneData?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(server, sceneId) {
        scope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
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
        PlaybackPageContent(
            server = server,
            scene = it,
            startPosition = startPosition,
            playbackMode = playbackMode,
            uiConfig = uiConfig,
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
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

@Composable
fun PlaylistPlaybackPage(
    server: StashServer,
    uiConfig: ComposeUiConfig,
    filterArgs: FilterArgs,
    startIndex: Int,
    modifier: Modifier = Modifier,
    clipDuration: Duration = 30.seconds,
    viewModel: FilterViewModel = viewModel(key = "main"),
    playlistViewModel: FilterViewModel = viewModel(key = "playlist"),
) {
    val context = LocalContext.current
    val maxPlaylistSize = 100 // TODO
    LaunchedEffect(server, filterArgs) {
        // TODO switch to single query
        viewModel.setFilter(server, adjustFilter(filterArgs))
        playlistViewModel.setFilter(server, filterArgs)
    }
    val pager by viewModel.pager.observeAsState()
    var playlist by remember(pager) { mutableStateOf<List<MediaItem>>(listOf()) }
    val playlistPager by playlistViewModel.pager.observeAsState()
    LaunchedEffect(pager) {
        playlist = pager?.let {
            buildList {
                for (i in startIndex..<(it.size).coerceAtMost(maxPlaylistSize)) {
                    it.getBlocking(i)?.let { item ->
                        if (filterArgs.dataType == DataType.SCENE) {
                            val scene = Scene.fromVideoSceneData(item as VideoSceneData)
                            val decision = getStreamDecision(context, scene, PlaybackMode.Choose)
                            val mediaItem =
                                buildMediaItem(context, decision, scene) {
                                    setTag(PlaylistFragment.MediaItemTag(scene, decision))
                                }
                            add(mediaItem)
                        } else {
                            // Markers
                            item as FullMarkerData
                            val scene = Scene.fromVideoSceneData(item.scene.videoSceneData)
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
                            add(mediaItem)
                        }
                    }
                }
            }
        } ?: listOf()
    }
    if (playlist.isNotEmpty()) {
        PlaylistPlaybackPageContent(
            server = server,
            playlist = playlist,
            uiConfig = uiConfig,
            markersEnabled = filterArgs.dataType == DataType.SCENE,
            playlistPager = playlistPager,
            modifier = modifier,
        )
    } else {
        CircularProgress()
    }
}
