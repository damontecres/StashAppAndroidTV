package com.github.damontecres.stashapp.ui.components.main

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.PreviewTheme
import com.github.damontecres.stashapp.ui.components.DotSeparatedRow
import com.github.damontecres.stashapp.ui.components.Rating100
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.ui.imagePreview
import com.github.damontecres.stashapp.ui.uiConfigPreview
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.listOfNotNullOrBlank
import com.github.damontecres.stashapp.util.name

@Composable
fun MainPageGalleryDetails(
    gallery: GalleryData,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        // Title
        Text(
            modifier = Modifier.enableMarquee(true),
            text = gallery.name ?: "",
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Rating
                Rating100(
                    rating100 = gallery.rating100 ?: 0,
                    uiConfig = uiConfig,
                    onRatingChange = {},
                    enabled = false,
                    modifier =
                        Modifier
                            // Not using ratingBarHeight intentionally
                            .height(24.dp),
                )
                // Quick info
                DotSeparatedRow(
                    modifier = Modifier,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    texts =
                        listOfNotNullOrBlank(
                            gallery.date,
                        ),
                )
            }
            // Description
            if (gallery.details.isNotNullOrBlank()) {
                Text(
                    text = gallery.details,
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
                if (gallery.studio != null) {
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
                        gallery.studio.name,
                        modifier =
                            Modifier
                                .background(bgColor, shape = RoundedCornerShape(8.dp)),
                    )
                }
                if (gallery.photographer.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_photographer),
                        gallery.photographer,
                    )
                }
                if (gallery.performers.isNotEmpty()) {
                    TitleValueText(
                        if (gallery.performers.size == 1) {
                            stringResource(R.string.stashapp_performer)
                        } else {
                            stringResource(R.string.stashapp_performers)
                        },
                        gallery.performers.joinToString(", ") { it.slimPerformerData.name },
                    )
                }
            }
        }
    }
}

@Preview(device = "spec:parent=tv_1080p", backgroundColor = 0xFF383535)
@Composable
private fun MainPageImageDetailsPreview() {
    PreviewTheme {
        MainPageImageDetails(
            image = imagePreview,
            uiConfig = uiConfigPreview,
            modifier =
                Modifier
                    .fillMaxSize(.7f)
                    .height(200.dp)
                    .padding(8.dp),
        )
    }
}
