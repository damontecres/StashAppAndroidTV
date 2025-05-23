package com.github.damontecres.stashapp.ui.components.scene

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.cards.PerformerCard
import com.github.damontecres.stashapp.ui.components.FocusPair
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.ItemsRow
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.titleCount

fun LazyListScope.sceneDetailsBody(
    scene: FullSceneData,
    tags: List<TagData>,
    performers: List<PerformerData>,
    galleries: List<GalleryData>,
    groups: List<GroupData>,
    markers: List<MarkerData>,
    suggestions: List<SlimSceneData>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    removeLongClicker: LongClicker<Any>,
    defaultLongClicker: LongClicker<Any>,
    cardOnFocus: (Boolean, Int, Int) -> Unit,
    createFocusPair: (Int) -> FocusPair?,
    startPadding: Dp = 24.dp,
    bottomPadding: Dp = 16.dp,
) {
    if (markers.isNotEmpty()) {
        item {
            ItemsRow(
                title = titleCount(R.string.stashapp_markers, markers),
                items = markers,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                longClicker = removeLongClicker,
                cardOnFocus = { isFocused, index ->
                    cardOnFocus.invoke(isFocused, 0, index)
                },
                focusPair = createFocusPair(0),
                modifier =
                    androidx.compose.ui.Modifier
                        .padding(start = startPadding, bottom = bottomPadding),
            )
        }
    }
    if (groups.isNotEmpty()) {
        item {
            ItemsRow(
                title = titleCount(R.string.stashapp_groups, groups),
                items = groups,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                longClicker = removeLongClicker,
                cardOnFocus = { isFocused, index ->
                    cardOnFocus.invoke(isFocused, 1, index)
                },
                focusPair = createFocusPair(1),
                modifier =
                    androidx.compose.ui.Modifier
                        .padding(start = startPadding, bottom = bottomPadding),
            )
        }
    }
    if (performers.isNotEmpty()) {
        item {
            ItemsRow(
                title = titleCount(R.string.stashapp_performers, performers),
                items = performers,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                longClicker = removeLongClicker,
                modifier =
                    androidx.compose.ui.Modifier
                        .padding(start = startPadding, bottom = bottomPadding),
                cardOnFocus = { isFocused, index ->
                    cardOnFocus.invoke(isFocused, 2, index)
                },
                focusPair = createFocusPair(2),
                itemContent = { uiConfig, item, itemOnClick, longClicker, getFilterAndPosition, cardModifier ->
                    PerformerCard(
                        uiConfig = uiConfig,
                        item = item,
                        onClick = {
                            itemOnClick.onClick(
                                item,
                                getFilterAndPosition?.invoke(item),
                            )
                        },
                        longClicker = longClicker,
                        getFilterAndPosition = getFilterAndPosition,
                        ageOnDate = scene.date,
                        modifier = cardModifier,
                    )
                },
            )
        }
    }
    if (tags.isNotEmpty()) {
        item {
            ItemsRow(
                title = titleCount(R.string.stashapp_tags, tags),
                items = tags,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                longClicker = removeLongClicker,
                cardOnFocus = { isFocused, index ->
                    cardOnFocus.invoke(isFocused, 3, index)
                },
                focusPair = createFocusPair(3),
                modifier =
                    androidx.compose.ui.Modifier
                        .padding(start = startPadding, bottom = bottomPadding),
            )
        }
    }
    if (galleries.isNotEmpty()) {
        item {
            ItemsRow(
                title = titleCount(R.string.stashapp_galleries, galleries),
                items = galleries,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                longClicker = removeLongClicker,
                cardOnFocus = { isFocused, index ->
                    cardOnFocus.invoke(isFocused, 4, index)
                },
                focusPair = createFocusPair(4),
                modifier =
                    androidx.compose.ui.Modifier
                        .padding(start = startPadding, bottom = bottomPadding),
            )
        }
    }
    if (suggestions.isNotEmpty()) {
        item {
            ItemsRow(
                title = titleCount(R.string.suggestions, suggestions),
                items = suggestions,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                longClicker = defaultLongClicker,
                cardOnFocus = { isFocused, index ->
                    cardOnFocus.invoke(isFocused, 5, index)
                },
                focusPair = createFocusPair(5),
                modifier =
                    androidx.compose.ui.Modifier
                        .padding(start = startPadding, bottom = bottomPadding),
            )
        }
    }
}
