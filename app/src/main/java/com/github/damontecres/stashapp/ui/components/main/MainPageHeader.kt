package com.github.damontecres.stashapp.ui.components.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.enableMarquee

@Composable
fun MainPageHeader(
    item: Any,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    val datatype =
        if (item is StashData) {
            Destination.getDataType(item)
        } else if (item is FilterArgs) {
            item.dataType
        } else {
            null
        }
    val visible =
        when (datatype) {
            DataType.SCENE, DataType.PERFORMER -> true
            else -> false
        }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
    ) {
        Box(
            modifier =
                modifier
                    .padding(bottom = 4.dp),
        ) {
            if (item is SlimSceneData) {
                MainPageSceneDetails(
                    scene = item,
                    uiConfig = uiConfig,
                    modifier =
                        Modifier
                            .height(200.dp)
                            .fillMaxWidth(),
                )
            } else if (item is PerformerData) {
                MainPagePerformerDetails(
                    perf = item,
                    uiConfig = uiConfig,
                    modifier =
                        Modifier
                            .height(160.dp)
                            .fillMaxWidth(),
                )
            } else if (item is FilterArgs && datatype == DataType.SCENE) {
                Box(
                    Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        modifier = Modifier.enableMarquee(true),
                        text =
                            item.name
                                ?: stringResource(item.dataType.pluralStringId),
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
                        maxLines = 1,
                    )
                }
            } else if (item is FilterArgs && datatype == DataType.PERFORMER) {
                Box(
                    Modifier
                        .height(160.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        modifier = Modifier.enableMarquee(true),
                        text =
                            item.name
                                ?: stringResource(item.dataType.pluralStringId),
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
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
