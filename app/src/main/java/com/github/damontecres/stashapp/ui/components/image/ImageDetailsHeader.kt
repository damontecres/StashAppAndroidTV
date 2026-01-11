package com.github.damontecres.stashapp.ui.components.image

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.compat.isTvDevice
import com.github.damontecres.stashapp.ui.components.DotSeparatedRow
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.Rating100
import com.github.damontecres.stashapp.ui.components.ScrollableDialog
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.ui.components.ratingBarHeight
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.listOfNotNullOrBlank
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ImageDetailsHeader(
    player: Player,
    image: ImageData,
    position: Int,
    count: Int,
    rating100: Int,
    oCount: Int,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    moreOnClick: () -> Unit,
    oCounterOnClick: () -> Unit,
    oCounterOnLongClick: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onZoom: (Float) -> Unit,
    onRotate: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
                .ifElse(isTvDevice, Modifier.fillMaxWidth(), Modifier.fillMaxWidth())
                .height(440.dp)
                .bringIntoViewRequester(bringIntoViewRequester),
    ) {
        // Title
        Text(
            text = image.titleOrFilename ?: "",
//                        color = MaterialTheme.colorScheme.onBackground,
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
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.alpha(0.75f),
        ) {
            // Rating
            Rating100(
                rating100 = rating100,
                uiConfig = uiConfig,
                onRatingChange = onRatingChange,
                enabled = true,
                modifier =
                    Modifier
                        .height(ratingBarHeight),
            )
            // Quick info
            val imageFile = image.visual_files.firstOrNull()?.onImageFile
            val videoFile = image.visual_files.firstOrNull()?.onVideoFile

            val imageRes = imageFile?.let { "${it.width}x${it.height}" }

            DotSeparatedRow(
                modifier = Modifier.padding(top = 8.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                texts =
                    listOfNotNullOrBlank(
                        image.date,
                        imageRes,
                        videoFile?.let { durationToString(it.duration) },
                        videoFile?.resolutionName(),
                    ),
            )
            // Description
            if (image.details.isNotNullOrBlank()) {
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
                            }.clickable(
                                enabled = true,
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                            ) { showDetailsDialog = true },
                ) {
                    Text(
                        text = image.details,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
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
                                text = image.details,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            // Key-Values
            Row(
                modifier =
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val navigationManager = LocalGlobalContext.current.navigationManager
                if (image.studio != null) {
                    TitleValueText(
                        stringResource(R.string.stashapp_studio),
                        image.studio.name,
                        playSoundOnFocus = uiConfig.playSoundOnFocus,
                        modifier =
                            Modifier.onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch(StashCoroutineExceptionHandler()) { bringIntoViewRequester.bringIntoView() }
                                }
                            },
                        onClick = {
                            navigationManager.navigate(
                                Destination.Item(
                                    DataType.STUDIO,
                                    image.studio.id,
                                ),
                            )
                        },
                    )
                }
                if (image.code.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_scene_code),
                        image.code,
                    )
                }
                if (image.photographer.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_photographer),
                        image.photographer,
                        playSoundOnFocus = uiConfig.playSoundOnFocus,
                        modifier =
                            Modifier.onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch(StashCoroutineExceptionHandler()) { bringIntoViewRequester.bringIntoView() }
                                }
                            },
                        onClick = {
                            navigationManager.navigate(
                                Destination.Filter(
                                    FilterArgs(
                                        DataType.IMAGE,
                                        objectFilter =
                                            ImageFilterType(
                                                photographer =
                                                    Optional.present(
                                                        StringCriterionInput(
                                                            value = image.photographer,
                                                            modifier = CriterionModifier.EQUALS,
                                                        ),
                                                    ),
                                            ),
                                    ),
                                ),
                            )
                        },
                    )
                }
                if (position >= 0) {
                    val str =
                        if (count > 0) {
                            "${position + 1} of $count"
                        } else {
                            "${position + 1}"
                        }
                    TitleValueText(
                        stringResource(R.string.stashapp_image_count),
                        str,
                    )
                }
            }
        }
        val playPauseState = rememberPlayPauseButtonState(player)
        ImageControlsOverlay(
            isImageClip = image.isImageClip,
            oCount = oCount,
            bringIntoViewRequester = bringIntoViewRequester,
            onZoom = onZoom,
            onRotate = onRotate,
            onReset = onReset,
            moreOnClick = moreOnClick,
            oCounterEnabled = uiConfig.readOnlyModeDisabled,
            oCounterOnClick = oCounterOnClick,
            oCounterOnLongClick = oCounterOnLongClick,
            playPauseOnClick = playPauseState::onClick,
            isPlaying = playPauseState.showPlay,
            sfwMode = uiConfig.sfwMode,
            modifier =
                Modifier
                    .fillMaxWidth(),
        )
    }
}
