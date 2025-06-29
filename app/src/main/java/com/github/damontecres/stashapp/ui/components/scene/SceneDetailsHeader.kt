package com.github.damontecres.stashapp.ui.components.scene

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.DotSeparatedRow
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.Rating100
import com.github.damontecres.stashapp.ui.components.ScrollableDialog
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.ui.util.playOnClickSound
import com.github.damontecres.stashapp.ui.util.playSoundOnFocus
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.listOfNotNullOrBlank
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.resume_position
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.formatBytes
import kotlinx.coroutines.launch

@Composable
fun SceneDetailsHeader(
    scene: FullSceneData,
    studio: StudioData?,
    rating100: Int,
    oCount: Int,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    editOnClick: () -> Unit,
    moreOnClick: () -> Unit,
    oCounterOnClick: () -> Unit,
    oCounterOnLongClick: () -> Unit,
    onRatingChange: (Int) -> Unit,
    focusRequester: FocusRequester,
    bringIntoViewRequester: BringIntoViewRequester,
    removeLongClicker: LongClicker<Any>,
    showEditButton: Boolean,
    alwaysStartFromBeginning: Boolean,
    modifier: Modifier = Modifier,
    showRatingBar: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier =
            modifier
                .fillMaxWidth()
//                .fillMaxHeight(.33f)
                .height(460.dp)
                .bringIntoViewRequester(bringIntoViewRequester),
    ) {
        if (scene.paths.screenshot.isNotNullOrBlank()) {
            val gradientColor = MaterialTheme.colorScheme.background
            AsyncImage(
                model = scene.paths.screenshot,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, gradientColor),
                                    startY = 500f,
                                ),
                            )
                            drawRect(
                                Brush.horizontalGradient(
                                    colors = listOf(gradientColor, Color.Transparent),
                                    endX = 400f,
                                    startX = 100f,
                                ),
                            )
//                            drawRect(
//                                Brush.linearGradient(
//                                    colors = listOf(gradientColor, Color.Transparent),
//                                    start = Offset(x = 500f, y = 500f),
//                                    end = Offset(x = 1000f, y = 0f),
//                                ),
//                            )
                        },
            )
        }
        Column(modifier = Modifier.fillMaxWidth(0.8f)) {
            Spacer(modifier = Modifier.height(60.dp))
            SceneDetailsHeaderInfo(
                scene = scene,
                studio = studio,
                rating100 = rating100,
                oCount = oCount,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                playOnClick = playOnClick,
                editOnClick = editOnClick,
                moreOnClick = moreOnClick,
                oCounterOnClick = oCounterOnClick,
                oCounterOnLongClick = oCounterOnLongClick,
                onRatingChange = onRatingChange,
                focusRequester = focusRequester,
                bringIntoViewRequester = bringIntoViewRequester,
                removeLongClicker = removeLongClicker,
                showEditButton = showEditButton,
                alwaysStartFromBeginning = alwaysStartFromBeginning,
                modifier = Modifier,
                showRatingBar = showRatingBar,
            )
            // Playback controls
            PlayButtons(
                resumePosition = scene.resume_position ?: 0,
                oCount = oCount,
                playOnClick = playOnClick,
                editOnClick = editOnClick,
                moreOnClick = moreOnClick,
                oCounterOnClick = oCounterOnClick,
                oCounterOnLongClick = oCounterOnLongClick,
                focusRequester = focusRequester,
                buttonOnFocusChanged = {
                    if (it.isFocused) {
                        scope.launch(StashCoroutineExceptionHandler()) { bringIntoViewRequester.bringIntoView() }
                    }
                },
                alwaysStartFromBeginning = alwaysStartFromBeginning,
                showEditButton = showEditButton,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp),
            )
        }
    }
}

@Composable
fun SceneDetailsHeaderInfo(
    scene: FullSceneData,
    studio: StudioData?,
    rating100: Int,
    oCount: Int,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    editOnClick: () -> Unit,
    moreOnClick: () -> Unit,
    oCounterOnClick: () -> Unit,
    oCounterOnLongClick: () -> Unit,
    onRatingChange: (Int) -> Unit,
    focusRequester: FocusRequester,
    bringIntoViewRequester: BringIntoViewRequester,
    removeLongClicker: LongClicker<Any>,
    showEditButton: Boolean,
    alwaysStartFromBeginning: Boolean,
    modifier: Modifier = Modifier,
    showRatingBar: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.padding(start = 16.dp),
    ) {
        // Title
        Text(
            text = scene.titleOrFilename ?: "",
            color = Color.LightGray,
            style =
                MaterialTheme.typography.displayMedium.copy(
                    shadow =
                        Shadow(
                            color = Color.DarkGray,
                            offset = Offset(5f, 2f),
                            blurRadius = 2f,
                        ),
                ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Column(
            modifier = Modifier.alpha(0.75f),
        ) {
            // Rating
            if (showRatingBar) {
                Rating100(
                    rating100 = rating100,
                    uiConfig = uiConfig,
                    onRatingChange = onRatingChange,
                    enabled = true,
                    modifier =
                        Modifier
                            .height(32.dp)
                            .padding(start = 12.dp),
                )
            }
            // Quick info
            val file = scene.files.firstOrNull()?.videoFile
            DotSeparatedRow(
                modifier = Modifier.padding(top = 6.dp, start = 8.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                texts =
                    listOfNotNullOrBlank(
                        scene.date,
                        file?.let { durationToString(it.duration) },
                        file?.resolutionName(),
                    ),
            )
            // Description
            if (scene.details.isNotNullOrBlank()) {
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused = interactionSource.collectIsFocusedAsState().value
                val bgColor =
                    if (isFocused) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f)
                    } else {
                        Color.Unspecified
                    }
                var showDetailsDialog by remember { mutableStateOf(false) }
                Box(
                    modifier =
                        Modifier
                            .background(bgColor, shape = RoundedCornerShape(8.dp))
                            .onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch(StashCoroutineExceptionHandler()) { bringIntoViewRequester.bringIntoView() }
                                }
                            }.playSoundOnFocus(uiConfig.playSoundOnFocus)
                            .clickable(
                                enabled = true,
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                            ) {
                                if (uiConfig.playSoundOnFocus) playOnClickSound(context)
                                showDetailsDialog = true
                            },
                ) {
                    Text(
                        text = scene.details,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                if (showDetailsDialog) {
                    ScrollableDialog(
                        onDismissRequest = { showDetailsDialog = false },
                        modifier = Modifier,
                    ) {
                        item {
                            Text(
                                text = scene.details,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                            )
                        }
                        if (scene.files.isNotEmpty()) {
                            item {
                                HorizontalDivider()
                                Text(
                                    stringResource(R.string.stashapp_files) + ":",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                        items(scene.files.map { it.videoFile }) {
                            val size =
                                it.size
                                    .toString()
                                    .toIntOrNull()
                                    ?.let(::formatBytes)
                            Text(
                                text = listOf(it.path, size).joinNotNullOrBlank(" - "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
            // Key-Values
            Row(
                modifier =
                    Modifier
                        .padding(top = 8.dp, start = 16.dp)
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (studio != null) {
                    TitleValueText(
                        stringResource(R.string.stashapp_studio),
                        studio.name,
                        playSoundOnFocus = uiConfig.playSoundOnFocus,
                        modifier =
                            Modifier.onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch(StashCoroutineExceptionHandler()) { bringIntoViewRequester.bringIntoView() }
                                }
                            },
                        onClick = {
                            itemOnClick.onClick(studio, null)
                        },
                        onLongClick = {
                            removeLongClicker.longClick(studio, null)
                        },
                    )
                }
                if (scene.code.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_scene_code),
                        scene.code,
                    )
                }
                if (scene.director.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_director),
                        scene.director,
                    )
                }
                TitleValueText(
                    stringResource(R.string.stashapp_play_count),
                    (scene.play_count ?: 0).toString(),
                )
                TitleValueText(
                    stringResource(R.string.stashapp_play_duration),
                    durationToString(scene.play_duration ?: 0.0),
                )
            }
        }
    }
}
