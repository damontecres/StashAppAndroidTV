package com.github.damontecres.stashapp.ui.components.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.Material3AppTheme
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.ItemsRow
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.pages.DialogParams
import com.github.damontecres.stashapp.ui.pages.SearchForPage
import com.github.damontecres.stashapp.ui.pages.SearchForParams
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.readOnlyModeDisabled

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ImageOverlay(
    server: StashServer,
    player: Player,
    slideshowControls: SlideshowControls,
    slideshowEnabled: Boolean,
    image: ImageData,
    tags: List<TagData>,
    performers: List<PerformerData>,
    rating100: Int,
    oCount: Int,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    oCountAction: (action: suspend MutationEngine.(String) -> OCounter) -> Unit,
    onRatingChange: (Int) -> Unit,
    longClicker: LongClicker<Any>,
    addItem: (item: StashData) -> Unit,
    removeItem: (item: StashData) -> Unit,
    onZoom: (Float) -> Unit,
    onRotate: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf<DialogParams?>(null) }
    var searchForDataType by remember { mutableStateOf<SearchForParams?>(null) }

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
                            if (readOnlyModeDisabled()) {
                                add(
                                    DialogItem(
                                        onClick = { removeItem(item) },
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
                            }
                        },
                )
        }

    val startStopSlideshow =
        DialogItem(
            headlineContent = {
                Text(
                    text =
                        if (slideshowEnabled) {
                            stringResource(R.string.stop_slideshow)
                        } else {
                            stringResource(R.string.play_slideshow)
                        },
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(if (slideshowEnabled) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24),
                    contentDescription = null,
                )
            },
            onClick = { if (slideshowEnabled) slideshowControls.stopSlideshow() else slideshowControls.startSlideshow() },
        )

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 135.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            ImageDetailsHeader(
                player = player,
                image = image,
                rating100 = rating100,
                oCount = oCount,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                moreOnClick = {
                    showDialog =
                        DialogParams(
                            title = context.getString(R.string.more) + "...",
                            fromLongClick = false,
                            items =
                                buildList {
                                    add(startStopSlideshow)
                                    if (readOnlyModeDisabled()) {
                                        add(
                                            DialogItem(
                                                context.getString(R.string.add_performer),
                                                DataType.PERFORMER.iconStringId,
                                            ) {
                                                searchForDataType =
                                                    SearchForParams(DataType.PERFORMER)
                                            },
                                        )
                                        add(
                                            DialogItem(
                                                context.getString(R.string.add_tag),
                                                DataType.TAG.iconStringId,
                                            ) {
                                                searchForDataType = SearchForParams(DataType.TAG)
                                            },
                                        )
                                    }
                                },
                        )
                },
                oCounterOnClick = { oCountAction.invoke(MutationEngine::incrementImageOCounter) },
                oCounterOnLongClick = {
                    showDialog =
                        DialogParams(
                            title = context.getString(R.string.stashapp_o_counter),
                            fromLongClick = true,
                            items =
                                listOf(
                                    DialogItem(context.getString(R.string.increment)) {
                                        oCountAction.invoke(MutationEngine::incrementImageOCounter)
                                    },
                                    DialogItem(context.getString(R.string.decrement)) {
                                        oCountAction.invoke(MutationEngine::decrementImageOCounter)
                                    },
                                    DialogItem(context.getString(R.string.reset)) {
                                        oCountAction.invoke(MutationEngine::resetImageOCounter)
                                    },
                                ),
                        )
                },
                onRatingChange = onRatingChange,
                onZoom = onZoom,
                onRotate = onRotate,
                onReset = onReset,
                modifier = Modifier,
            )
        }
        val startPadding = 24.dp
        val bottomPadding = 16.dp

        if (performers.isNotEmpty()) {
            item {
                ItemsRow(
                    title = stringResource(R.string.stashapp_performers),
                    items = performers,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = removeLongClicker,
                    modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
                )
            }
        }
        if (tags.isNotEmpty()) {
            item {
                ItemsRow(
                    title = stringResource(R.string.stashapp_tags),
                    items = tags,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = removeLongClicker,
                    modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
                )
            }
        }
        item {
            ImageDetailsFooter(
                image,
                Modifier.padding(start = startPadding, bottom = bottomPadding, top = 24.dp),
            )
        }
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
    searchForDataType?.let { params ->
        Material3AppTheme {
            Dialog(
                onDismissRequest = { searchForDataType = null },
                properties =
                    DialogProperties(
                        usePlatformDefaultWidth = false,
                    ),
            ) {
                val elevatedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                val color = MaterialTheme.colorScheme.secondaryContainer
                Box(
                    Modifier
                        .fillMaxSize(.9f)
                        .graphicsLayer {
                            this.clip = true
                            this.shape = RoundedCornerShape(28.0.dp)
                        }.drawBehind { drawRect(color = color) }
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(PaddingValues(12.dp)),
                    propagateMinConstraints = true,
                ) {
                    SearchForPage(
                        server = server,
                        title = "Add " + stringResource(params.dataType.stringId),
                        searchId = params.id,
                        dataType = params.dataType,
                        itemOnClick = { id, item ->
                            // Close dialog
                            searchForDataType = null
                            addItem.invoke(item)
                        },
                        uiConfig = uiConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
