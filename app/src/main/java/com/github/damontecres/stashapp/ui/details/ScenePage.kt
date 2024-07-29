package com.github.damontecres.stashapp.ui.details

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SceneDetailsActivity
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.playback.CodecSupport
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.asVideoSceneData
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.parseTimeToString
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class SceneUiState {
    data class Success(val scene: FullSceneData) : SceneUiState()

    data object Loading : SceneUiState()

    data class Error(val message: String, val cause: Exception? = null) : SceneUiState()
}

@HiltViewModel
class SceneViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val queryEngine = QueryEngine(context)

        val uiState =
            savedStateHandle
                .getStateFlow<String?>(SceneDetailsActivity.MOVIE, null)
                .map { id ->
                    if (id == null) {
                        SceneUiState.Error("sceneId cannot be null")
                    } else {
                        val scene = queryEngine.getScene(id)
                        if (scene != null) {
                            SceneUiState.Success(scene)
                        } else {
                            SceneUiState.Error("No scene with id=$id")
                        }
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = SceneUiState.Loading,
                )

        private val _performers = mutableStateListOf<PerformerData>()
        val performers: SnapshotStateList<PerformerData> get() = _performers

        suspend fun fetchPerformers(scene: FullSceneData) {
            val ids = scene.performers.map { it.id }
            val results = queryEngine.findPerformers(performerIds = ids)
            _performers.addAll(results)
        }

        private val _galleries = mutableStateListOf<GalleryData>()
        val galleries: SnapshotStateList<GalleryData> get() = _galleries

        suspend fun fetchGalleries(scene: FullSceneData) {
            val ids = scene.galleries.map { it.id }
            val results = queryEngine.getGalleries(ids)
            _galleries.addAll(results)
        }
    }

@Suppress("ktlint:standard:function-naming")
@Composable
fun ScenePage(
    itemOnClick: (Any) -> Unit,
    viewModel: SceneViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is SceneUiState.Loading -> {
            Text(text = "Loading...")
        }

        is SceneUiState.Error -> {
            Text(text = "Error: ${s.message}")
        }

        is SceneUiState.Success -> {
            LaunchedEffect(Unit) {
                viewModel.fetchPerformers(s.scene)
                viewModel.fetchGalleries(s.scene)
            }
            SceneDetails(
                s.scene,
                itemOnClick,
                Modifier
                    .fillMaxSize()
                    .animateContentSize(),
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
private fun SceneDetails(
    scene: FullSceneData,
    itemOnClick: (Any) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SceneViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val createdAt =
        stringResource(R.string.stashapp_created_at) + ": " +
            parseTimeToString(
                scene.created_at,
            )
    val updatedAt =
        stringResource(R.string.stashapp_updated_at) + ": " +
            parseTimeToString(
                scene.updated_at,
            )
    val file = scene.files.firstOrNull()
    val subtitle =
        if (file != null) {
            val resolution = file.videoFileData.resolutionName()
            val duration = durationToString(file.videoFileData.duration)
            concatIfNotBlank(
                " - ",
                scene.studio?.studioData?.name,
                scene.date,
                duration,
                resolution,
            )
        } else {
            null
        }
    val debugInfo =
        if (file != null &&
            PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                stringResource(R.string.pref_key_show_playback_debug_info),
                false,
            )
        ) {
            val videoFile = file.videoFileData
            val supportedCodecs = CodecSupport.getSupportedCodecs(context)
            val videoSupported = supportedCodecs.isVideoSupported(videoFile.video_codec)
            val audioSupported = supportedCodecs.isAudioSupported(videoFile.audio_codec)
            val containerSupported =
                supportedCodecs.isContainerFormatSupported(videoFile.format)

            val video =
                if (videoSupported) {
                    "Video: ${videoFile.video_codec}"
                } else {
                    "Video: ${videoFile.video_codec} (unsupported)"
                }
            val audio =
                if (audioSupported) {
                    "Audio: ${videoFile.audio_codec}"
                } else {
                    "Audio: ${videoFile.audio_codec} (unsupported)"
                }
            val format =
                if (containerSupported) {
                    "Format: ${videoFile.format}"
                } else {
                    "Format: ${videoFile.format} (unsupported)"
                }

            listOf(video, audio, format).joinToString(", ")
        } else {
            null
        }
    val playCount =
        if (scene.play_count != null && scene.play_count > 0) {
            stringResource(R.string.stashapp_play_count) + ": " + scene.play_count.toString()
        } else {
            null
        }
    val playDuration =
        if (scene.play_duration != null && scene.play_duration >= 1.0) {
            stringResource(R.string.stashapp_play_duration) + ": " + durationToString(scene.play_duration)
        } else {
            null
        }

    val playHistory =
        if (playCount != null || playDuration != null) {
            concatIfNotBlank(
                ", ",
                playCount,
                playDuration,
            )
        } else {
            null
        }

    val body =
        listOfNotNull(debugInfo, playHistory, createdAt, updatedAt)
            .joinToString("\n")

    TvLazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            modifier
                .focusGroup(),
    ) {
        item {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(350.dp),
            ) {
                if (scene.paths.screenshot != null) {
                    GlideImage(
                        model = scene.paths.screenshot,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxSize(),
                    )
                }
            }
        }

        item {
            Button(onClick = { /*TODO*/ }) {
                Text(text = "Play")
            }
        }

        item {
            ProvideTextStyle(MaterialTheme.typography.headlineLarge) {
                Text(
                    text = scene.titleOrFilename ?: "",
                )
            }
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                if (subtitle != null) {
                    Text(modifier = Modifier, text = subtitle)
                }
            }
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                if (scene.details != null) {
                    Text(modifier = Modifier, text = scene.details)
                }
            }
            ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                if (body.isNotNullOrBlank()) {
                    Text(modifier = Modifier, text = body)
                }
            }
        }
        // Markers
        item {
            ItemRow(
                name = stringResource(R.string.stashapp_markers),
                items = scene.scene_markers.map { convertMarker(scene, it) },
                itemOnClick,
            )
        }

        // Studio
        if (scene.studio != null) {
            item {
                ItemRow(
                    name = stringResource(R.string.stashapp_studio),
                    items = listOf(scene.studio.studioData),
                    itemOnClick,
                )
            }
        }

        // Movies
        item {
            ItemRow(
                name = stringResource(R.string.stashapp_movies),
                items = scene.movies.sortedBy { it.scene_index }.map { it.movie.movieData },
                itemOnClick,
            )
        }

        // Performers
        item {
            ItemRow(
                name = stringResource(R.string.stashapp_performers),
                items = viewModel.performers,
                itemOnClick,
            )
        }

        // Tags
        item {
            ItemRow(
                name = stringResource(R.string.stashapp_tags),
                items = scene.tags.map { it.tagData },
                itemOnClick,
            )
        }

        // Galleries
        item {
            ItemRow(
                name = stringResource(R.string.stashapp_galleries),
                items = viewModel.galleries,
                itemOnClick,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun ItemRow(
    name: String,
    items: List<Any>,
    itemOnClick: (Any) -> Unit,
) {
    if (items.isNotEmpty()) {
        Column {
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                Text(
                    modifier = Modifier.padding(top = 20.dp, bottom = 10.dp, start = 16.dp),
                    text = name,
                )
            }
            TvLazyRow(
                contentPadding = PaddingValues(start = 16.dp),
                modifier =
                    Modifier
                        .focusGroup()
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(items) { item ->
                    StashCard(item, itemOnClick)
                }
            }
        }
    }
}

fun convertMarker(
    scene: FullSceneData,
    m: FullSceneData.Scene_marker,
): MarkerData {
    return MarkerData(
        id = m.id,
        title = m.title,
        created_at = "",
        updated_at = "",
        stream = m.stream,
        screenshot = m.screenshot,
        seconds = m.seconds,
        preview = "",
        primary_tag = MarkerData.Primary_tag("", m.primary_tag.tagData),
        scene = MarkerData.Scene(scene.id, scene.asVideoSceneData),
        tags = m.tags.map { MarkerData.Tag("", it.tagData) },
        __typename = "",
    )
}
