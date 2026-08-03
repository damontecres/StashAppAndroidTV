package com.github.damontecres.stashapp.ui.components.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.PreviewTheme
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.ui.slimScenePreview
import com.github.damontecres.stashapp.ui.uiConfigPreview
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun MainPageMarkerDetails(
    marker: MarkerData,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        // Title
        Text(
            modifier = Modifier.enableMarquee(true),
            text = marker.title.takeIf { it.isNotNullOrBlank() } ?: marker.primary_tag.slimTagData.name,
//                        color = MaterialTheme.colorScheme.onBackground,
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
            maxLines = 1,
        )

        Column(
            modifier = Modifier.alpha(0.75f),
        ) {
            marker.scene.minimalSceneData.titleOrFilename?.let {
                Text(
                    modifier = Modifier.enableMarquee(true),
                    text = marker.title.takeIf { it.isNotNullOrBlank() } ?: marker.primary_tag.slimTagData.name,
//                        color = MaterialTheme.colorScheme.onBackground,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
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
                TitleValueText(
                    stringResource(R.string.stashapp_resume_time),
                    marker.seconds.seconds.inWholeMilliseconds.milliseconds
                        .toString(),
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
        }
    }
}

@Preview(device = "spec:parent=tv_1080p", backgroundColor = 0xFF383535)
@Composable
private fun MainPageSceneDetailsPreview() {
    PreviewTheme {
        MainPageSceneDetails(
            scene = slimScenePreview,
            uiConfig = uiConfigPreview,
            modifier =
                Modifier
                    .fillMaxSize(.7f)
                    .height(200.dp)
                    .padding(8.dp),
        )
    }
}
