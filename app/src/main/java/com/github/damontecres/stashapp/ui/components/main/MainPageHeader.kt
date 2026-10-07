package com.github.damontecres.stashapp.ui.components.main

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
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.enableMarquee

@Composable
fun MainPageHeader(
    item: Any?,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(bottom = 4.dp)
                .height(200.dp),
    ) {
        val headerModifier =
            Modifier.fillMaxWidth()
        when (item) {
            is SlimSceneData -> {
                MainPageSceneDetails(
                    scene = item,
                    uiConfig = uiConfig,
                    modifier = headerModifier,
                )
            }

            is PerformerData -> {
                MainPagePerformerDetails(
                    perf = item,
                    uiConfig = uiConfig,
                    modifier = headerModifier,
                )
            }

            is ImageData -> {
                MainPageImageDetails(
                    image = item,
                    uiConfig = uiConfig,
                    modifier = headerModifier,
                )
            }

            is GroupData -> {
                MainPageGroupDetails(item, uiConfig, headerModifier)
            }

            is TagData -> {
                MainPageTagDetails(item, uiConfig, headerModifier)
            }

            is MarkerData -> {
                MainPageMarkerDetails(item, uiConfig, headerModifier)
            }

            is GalleryData -> {
                MainPageGalleryDetails(item, uiConfig, headerModifier)
            }

            is StudioData -> {
                MainPageStudioDetails(item, uiConfig, headerModifier)
            }

            is FilterArgs -> {
                FilterHeader(item, headerModifier)
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
