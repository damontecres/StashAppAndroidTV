package com.github.damontecres.stashapp.ui.components.image

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.ui.components.CreatedTimestamp
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.ui.components.UpdatedTimestamp
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.formatBytes

@Composable
fun ImageDetailsFooter(
    image: ImageData,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier =
            modifier
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { CreatedTimestamp(image.created_at) }
        item { UpdatedTimestamp(image.updated_at) }
        item {
            TitleValueText(stringResource(R.string.id), image.id)
        }
        image.visual_files.firstOrNull()?.onVideoFile?.let {
            if (it.video_codec.isNotNullOrBlank()) {
                item {
                    TitleValueText(
                        stringResource(R.string.stashapp_video_codec),
                        it.video_codec,
                    )
                }
            }
            if (it.audio_codec.isNotNullOrBlank()) {
                item {
                    TitleValueText(
                        stringResource(R.string.stashapp_audio_codec),
                        it.audio_codec,
                    )
                }
            }
            if (it.format.isNotNullOrBlank()) {
                item {
                    TitleValueText(
                        stringResource(R.string.format),
                        it.format,
                    )
                }
            }
        }
        image.visual_files.firstOrNull()?.onBaseFile?.let {
            item {
                TitleValueText(
                    stringResource(R.string.stashapp_filesize),
                    it.size
                        .toString()
                        .toLongOrNull()
                        ?.let { bytes -> formatBytes(bytes) } ?: it.size.toString(),
                )
            }
        }
    }
}
