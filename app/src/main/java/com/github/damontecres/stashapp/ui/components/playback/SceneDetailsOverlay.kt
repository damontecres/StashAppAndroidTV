package com.github.damontecres.stashapp.ui.components.playback

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.scene.SceneDetailsFooter
import com.github.damontecres.stashapp.ui.components.scene.SceneDetailsHeaderInfo
import com.github.damontecres.stashapp.ui.components.scene.sceneDetailsBody
import com.github.damontecres.stashapp.util.StashServer

@Composable
fun SceneDetailsOverlay(
    server: StashServer,
    scene: FullSceneData,
    performers: List<PerformerData>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    modifier: Modifier = Modifier,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val navigationManager = LocalGlobalContext.current.navigationManager
    val itemClick =
        ItemOnClicker<Any> { item, fp ->
            navigationManager.goBack()
            itemOnClick.onClick(item, fp)
        }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 135.dp),
        modifier = modifier.focusRequester(focusRequester),
    ) {
        item {
            SceneDetailsHeaderInfo(
                scene = scene,
                studio = scene.studio?.studioData,
                rating100 = scene.rating100 ?: 0,
                oCount = scene.o_counter ?: 0,
                uiConfig = uiConfig.copy(readOnlyModeEnabled = true),
                itemOnClick = itemClick,
                playOnClick = { _, _ -> },
                editOnClick = {},
                moreOnClick = {},
                oCounterOnClick = {},
                oCounterOnLongClick = {},
                onRatingChange = {},
                focusRequester = focusRequester,
                bringIntoViewRequester = bringIntoViewRequester,
                removeLongClicker = { _, _ -> },
                showEditButton = false,
                alwaysStartFromBeginning = false,
                modifier = Modifier,
                showRatingBar = true,
            )
        }
        sceneDetailsBody(
            scene = scene,
            tags = scene.tags.map { it.tagData },
            performers = performers,
            galleries = listOf(),
            groups = scene.groups.map { it.group.groupData },
            markers = listOf(),
            suggestions = listOf(),
            uiConfig = uiConfig,
            itemOnClick = itemClick,
            removeLongClicker = { _, _ -> },
            defaultLongClicker = { _, _ -> },
            cardOnFocus = { _, _, _ -> },
            createFocusPair = { null },
        )
        item {
            SceneDetailsFooter(
                scene = scene,
                modifier = Modifier,
            )
        }
    }
}
