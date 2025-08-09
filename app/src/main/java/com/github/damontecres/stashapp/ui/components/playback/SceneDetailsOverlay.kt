package com.github.damontecres.stashapp.ui.components.playback

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.scene.SceneDescriptionDialog
import com.github.damontecres.stashapp.ui.components.scene.SceneDetailsFooter
import com.github.damontecres.stashapp.ui.components.scene.SceneDetailsHeaderInfo
import com.github.damontecres.stashapp.ui.components.scene.sceneDetailsBody
import com.github.damontecres.stashapp.util.StashServer

@Composable
fun SceneDetailsOverlay(
    server: StashServer,
    scene: FullSceneData,
    performers: List<PerformerData>,
    rating100: Int,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var showDetailsDialog by remember { mutableStateOf(false) }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 135.dp),
        modifier = modifier.focusRequester(focusRequester),
    ) {
        item {
            SceneDetailsHeaderInfo(
                scene = scene,
                studio = scene.studio?.studioData,
                rating100 = rating100,
                uiConfig = uiConfig,
                itemOnClick = itemOnClick,
                detailsOnClick = { showDetailsDialog = true },
                onRatingChange = onRatingChange,
                bringIntoViewRequester = bringIntoViewRequester,
                removeLongClicker = { _, _ -> },
                modifier = Modifier.padding(bottom = 80.dp),
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
            itemOnClick = itemOnClick,
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
    SceneDescriptionDialog(
        show = showDetailsDialog,
        scene = scene,
        onDismissRequest = { showDetailsDialog = false },
    )
}
