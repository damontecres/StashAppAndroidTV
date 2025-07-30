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
import com.github.damontecres.stashapp.api.fragment.ImageData
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
    val dataType =
        when (item) {
            is StashData -> Destination.getDataType(item)
            is FilterArgs -> item.dataType
            else -> null
        }
    val height =
        when (dataType) {
            DataType.SCENE -> 200.dp
            DataType.PERFORMER -> 160.dp
            DataType.IMAGE -> 200.dp
            else -> 0.dp
        }

    AnimatedVisibility(
        visible = height != 0.dp,
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
                            .height(height)
                            .fillMaxWidth(),
                )
            } else if (item is PerformerData) {
                MainPagePerformerDetails(
                    perf = item,
                    uiConfig = uiConfig,
                    modifier =
                        Modifier
                            .height(height)
                            .fillMaxWidth(),
                )
            } else if (item is ImageData) {
                MainPageImageDetails(
                    image = item,
                    uiConfig = uiConfig,
                    modifier =
                        Modifier
                            .height(height)
                            .fillMaxWidth(),
                )
            } else if (item is FilterArgs) {
                FilterHeader(item, Modifier.height(height))
            }
        }
    }
}

@Composable
fun FilterHeader(
    item: FilterArgs,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
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
