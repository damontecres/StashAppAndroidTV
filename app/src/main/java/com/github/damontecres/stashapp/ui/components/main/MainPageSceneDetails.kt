package com.github.damontecres.stashapp.ui.components.main

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.DotSeparatedRow
import com.github.damontecres.stashapp.ui.components.StarRating
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.listOfNotNullOrBlank
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString

@Composable
fun MainPageSceneDetails(
    scene: SlimSceneData,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        // Title
        Text(
            modifier = Modifier.enableMarquee(true),
            text = scene.titleOrFilename ?: "",
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
            maxLines = 1,
        )

        Column(
            modifier = Modifier.alpha(0.75f),
        ) {
            // Rating
            StarRating(
                rating100 = scene.rating100 ?: 0,
                precision = uiConfig.starPrecision,
                onRatingChange = {},
                enabled = false,
                modifier =
                    Modifier
                        .height(24.dp),
            )
            // Quick info
            val file = scene.files.firstOrNull()?.videoFile
            DotSeparatedRow(
                modifier = Modifier.padding(top = 4.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                texts =
                    listOfNotNullOrBlank(
                        scene.date,
                        file?.let { durationToString(it.duration) },
                        file?.resolutionName(),
                    ),
            )
            // Description
            if (scene.details.isNotNullOrBlank()) {
                Text(
                    text = scene.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            // Key-Values
            Row(
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (scene.studio != null) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused = interactionSource.collectIsFocusedAsState().value
                    val bgColor =
                        if (isFocused) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .75f)
                        } else {
                            Color.Unspecified
                        }
                    TitleValueText(
                        stringResource(R.string.stashapp_studio),
                        scene.studio.name,
                        modifier =
                            Modifier
                                .background(bgColor, shape = RoundedCornerShape(8.dp)),
                    )
                }
                if (scene.code.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_scene_code),
                        scene.code,
                    )
                }
                if (scene.director.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_director),
                        scene.director,
                    )
                }
                TitleValueText(
                    stringResource(R.string.stashapp_play_count),
                    (scene.play_count ?: 0).toString(),
                )
                TitleValueText(
                    stringResource(R.string.stashapp_play_duration),
                    durationToString(scene.play_duration ?: 0.0),
                )
            }
        }
    }
}
