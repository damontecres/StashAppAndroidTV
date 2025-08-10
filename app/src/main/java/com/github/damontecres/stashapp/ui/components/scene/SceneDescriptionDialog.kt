package com.github.damontecres.stashapp.ui.components.scene

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.ui.components.ScrollableDialog
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.formatBytes

@Composable
fun SceneDescriptionDialog(
    show: Boolean,
    scene: FullSceneData,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (show) {
        ScrollableDialog(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
        ) {
            scene.titleOrFilename?.let {
                item {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            scene.details?.let {
                item {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                    )
                }
            }
            if (scene.files.isNotEmpty()) {
                item {
                    HorizontalDivider()
                    Text(
                        stringResource(R.string.stashapp_files) + ":",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            items(scene.files.map { it.videoFile }) {
                val size =
                    it.size
                        .toString()
                        .toIntOrNull()
                        ?.let(::formatBytes)
                Text(
                    text = listOf(it.path, size).joinNotNullOrBlank(" - "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
