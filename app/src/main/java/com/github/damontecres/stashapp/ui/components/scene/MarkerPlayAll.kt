package com.github.damontecres.stashapp.ui.components.scene

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.cards.RootCard
import com.github.damontecres.stashapp.ui.components.MarkerDurationDialog

fun LazyListScope.markerPlayAll(
    markers: List<MarkerData>,
    sceneId: String,
    uiConfig: ComposeUiConfig,
    navigationManager: NavigationManager,
    modifier: Modifier = Modifier,
) {
    if (markers.size > 1) {
        fun playAll(durationMs: Long) {
            val objectFilter =
                SceneMarkerFilterType(
                    scenes =
                        Optional.present(
                            MultiCriterionInput(
                                value = Optional.present(listOf(sceneId)),
                                modifier = CriterionModifier.INCLUDES,
                            ),
                        ),
                )
            val filterArgs =
                FilterArgs(
                    dataType = DataType.MARKER,
                    objectFilter = objectFilter,
                    findFilter =
                        StashFindFilter(
                            SortAndDirection(
                                SortOption.Seconds,
                                SortDirectionEnum.ASC,
                            ),
                        ),
                )
            val destination =
                Destination.Playlist(
                    filterArgs = filterArgs,
                    position = 0,
                    duration = durationMs,
                )
            navigationManager.navigate(destination)
        }

        item {
            var showDialog by remember { mutableStateOf(false) }
            RootCard(
                modifier = modifier,
                item = null,
                title = stringResource(R.string.play_all),
                uiConfig = uiConfig,
                imageWidth = MarkerPresenter.CARD_WIDTH.dp / 2,
                imageHeight = MarkerPresenter.CARD_HEIGHT.dp / 2,
                longClicker = { _, _ -> },
                getFilterAndPosition = null,
                imageContent = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.play_all),
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                subtitle = {
                    Column {
                        Text(text = "")
                        Text(text = "")
                    }
                },
                description = {
                    Text(text = "")
                },
                onClick = {
                    if (markers.firstOrNull { it.end_seconds == null } != null) {
                        showDialog = true
                    } else {
                        playAll(0)
                    }
                },
            )
            if (showDialog) {
                MarkerDurationDialog(
                    onDismissRequest = { showDialog = false },
                    onClick = { playAll(it) },
                )
            }
        }
    }
}
