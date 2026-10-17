package com.github.damontecres.stashapp.ui.components.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.isNotNullOrBlank

@Composable
fun MainPageTagDetails(
    tag: TagData,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        // Title
        Text(
            modifier = Modifier.enableMarquee(true),
            text = tag.name,
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
            // Description
            if (tag.description.isNotNullOrBlank()) {
                Text(
                    text = tag.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}
