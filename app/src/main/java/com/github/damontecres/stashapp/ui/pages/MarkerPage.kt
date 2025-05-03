package com.github.damontecres.stashapp.ui.pages

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.cards.TagCard
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.ItemsRow
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.models.MarkerDetailsViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun MarkerPage(
    server: StashServer,
    markerId: String,
    itemOnClick: ItemOnClicker<Any>,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
    viewModel: MarkerDetailsViewModel = viewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.init(server, markerId)
    }

    val marker by viewModel.item.observeAsState()
    val primaryTag by viewModel.primaryTag.observeAsState()
    val tags by viewModel.tags.observeAsState(listOf())

    var showDialog by remember { mutableStateOf<DialogParams?>(null) }
    val removeLongClicker =
        LongClicker<Any> { item, filterAndPosition ->
            item as StashData
            showDialog =
                DialogParams(
                    title = extractTitle(item) ?: "",
                    fromLongClick = true,
                    items =
                        buildList {
                            add(
                                DialogItem("Go to", Icons.Default.PlayArrow) {
                                    itemOnClick.onClick(
                                        item,
                                        filterAndPosition,
                                    )
                                },
                            )
                            add(
                                DialogItem(
                                    onClick = { viewModel.removeTag(item.id) },
                                    headlineContent = {
                                        Text(stringResource(R.string.stashapp_actions_remove))
                                    },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.stashapp_actions_remove),
                                            tint = Color.Red,
                                        )
                                    },
                                ),
                            )
                        },
                )
        }

    if (marker != null && primaryTag != null) {
        MarkerPageContent(
            server = server,
            marker = marker!!,
            primaryTag = primaryTag!!,
            tags = tags,
            itemOnClick = itemOnClick,
            longClicker = removeLongClicker,
            uiConfig = uiConfig,
            setPrimaryTag = viewModel::setPrimaryTag,
            addTag = viewModel::addTag,
            removeTag = viewModel::removeTag,
            modifier = modifier.fillMaxSize(),
        )
    }
    showDialog?.let { params ->
        DialogPopup(
            showDialog = true,
            title = params.title,
            dialogItems = params.items,
            onDismissRequest = { showDialog = null },
            waitToLoad = params.fromLongClick,
        )
    }
}

@Composable
fun MarkerPageContent(
    server: StashServer,
    marker: FullMarkerData,
    primaryTag: TagData,
    tags: List<TagData>,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    setPrimaryTag: (String) -> Unit,
    addTag: (String) -> Unit,
    removeTag: (String) -> Unit,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    val navigationManager = LocalGlobalContext.current.navigationManager
    var searchForPrimary by remember { mutableStateOf<Boolean?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.tryRequestFocus()
    }
    Box(
        modifier = modifier,
    ) {
        val gradientColor = MaterialTheme.colorScheme.background
        AsyncImage(
            model = marker.screenshot,
            contentDescription = "",
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxSize(.8f)
                    .align(Alignment.TopEnd)
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
                    },
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                modifier = Modifier,
                text =
                    if (marker.title.isNotNullOrBlank()) {
                        marker.title
                    } else {
                        primaryTag.name
                    },
                color = MaterialTheme.colorScheme.onBackground,
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

            val interactionSource = remember { MutableInteractionSource() }
            val isFocused = interactionSource.collectIsFocusedAsState().value
            val bgColor =
                if (isFocused) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f)
                } else {
                    Color.Unspecified
                }
            Text(
                modifier =
                    Modifier
                        .background(bgColor, shape = RoundedCornerShape(8.dp))
                        .clickable(
                            enabled = true,
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                        ) {
                            navigationManager.navigate(
                                Destination.Item(
                                    DataType.SCENE,
                                    marker.scene.videoSceneData.id,
                                ),
                            )
                        },
                text = marker.scene.videoSceneData.titleOrFilename ?: "",
                color = MaterialTheme.colorScheme.onBackground,
                style =
                    MaterialTheme.typography.displaySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                TitleValueText(
                    stringResource(R.string.stashapp_resume_time),
                    marker.seconds.seconds.toString(),
                )
                if (marker.end_seconds != null) {
                    TitleValueText(
                        stringResource(R.string.stashapp_time_end),
                        marker.end_seconds.seconds.toString(),
                    )

                    TitleValueText(
                        stringResource(R.string.stashapp_duration),
                        (marker.end_seconds.seconds - marker.seconds.seconds).toString(),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier,
            ) {
                ControlButton(
                    title = stringResource(R.string.play),
                    icon = Icons.Filled.PlayArrow,
                    onClick = {
                        navigationManager.navigate(
                            Destination.Playback(
                                marker.scene.videoSceneData.id,
                                marker.seconds.seconds.inWholeMilliseconds,
                                PlaybackMode.Choose,
                            ),
                        )
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                )
                ControlButton(
                    title = stringResource(R.string.change_primary_tag),
                    icon = Icons.Filled.Info,
                    onClick = {
                        searchForPrimary = true
                    },
                    modifier = Modifier,
                )
                ControlButton(
                    title = stringResource(R.string.add_tag),
                    icon = Icons.Filled.Add,
                    onClick = {
                        searchForPrimary = false
                    },
                    modifier = Modifier,
                )
                ControlButton(
                    title = stringResource(R.string.timestamps),
                    icon = Icons.Filled.Edit,
                    onClick = {
                        navigationManager.navigate(
                            Destination.UpdateMarker(marker.id),
                        )
                    },
                    modifier = Modifier,
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Column(
                    modifier = Modifier,
                ) {
                    Text(
                        text = stringResource(R.string.stashapp_primary_tag),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    TagCard(
                        modifier = Modifier.padding(top = 16.dp),
                        uiConfig = uiConfig,
                        item = primaryTag,
                        onClick = { itemOnClick.onClick(primaryTag, null) },
                        longClicker = { _, _ -> },
                        getFilterAndPosition = null,
                    )
                }
                if (tags.isNotEmpty()) {
                    ItemsRow(
                        title = stringResource(DataType.TAG.pluralStringId),
                        items = tags,
                        uiConfig = uiConfig,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                    )
                }
            }
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (marker.created_at.toString().length >= 10) {
                    TitleValueText(
                        stringResource(R.string.stashapp_created_at),
                        marker.created_at.toString().substring(0..<10),
                    )
                }
                if (marker.updated_at.toString().length >= 10) {
                    TitleValueText(
                        stringResource(R.string.stashapp_updated_at),
                        marker.updated_at.toString().substring(0..<10),
                    )
                }
                TitleValueText(
                    stringResource(R.string.id),
                    marker.id,
                )
                TitleValueText(
                    stringResource(R.string.stashapp_scene_id),
                    marker.scene.videoSceneData.id,
                )
            }
        }
        if (searchForPrimary != null) {
            SearchForDialog(
                show = true,
                dataType = DataType.TAG,
                onItemClick = { item ->
                    if (searchForPrimary == true) {
                        setPrimaryTag.invoke(item.id)
                    } else {
                        addTag.invoke(item.id)
                    }
                    searchForPrimary = null
                },
                onDismissRequest = { searchForPrimary = null },
                dialogTitle =
                    if (searchForPrimary == true) {
                        stringResource(R.string.change_primary_tag)
                    } else {
                        stringResource(R.string.add_tag)
                    },
                dismissOnClick = false,
                uiConfig = uiConfig,
            )
        }
    }
}

@Composable
fun ControlButton(
    onClick: () -> Unit,
    title: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
