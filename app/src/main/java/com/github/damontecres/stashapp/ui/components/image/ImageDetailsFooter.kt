package com.github.damontecres.stashapp.ui.components.image

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.formatBytes

@Composable
fun ImageDetailsFooter(
    image: ImageData,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (image.created_at.toString().length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_created_at),
                image.created_at.toString().substring(0..<10),
            )
        }
        if (image.updated_at.toString().length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_updated_at),
                image.updated_at.toString().substring(0..<10),
            )
        }
        TitleValueText(stringResource(R.string.id), image.id)
        val baseFile = image.visual_files.firstOrNull()?.onBaseFile
//        val imageFile = image.visual_files.firstOrNull()?.onImageFile
        val videoFile = image.visual_files.firstOrNull()?.onVideoFile
        videoFile?.let {
            if (it.video_codec.isNotNullOrBlank()) {
                TitleValueText(
                    stringResource(R.string.stashapp_video_codec),
                    it.video_codec,
                )
            }
            if (it.audio_codec.isNotNullOrBlank()) {
                TitleValueText(
                    stringResource(R.string.stashapp_audio_codec),
                    it.audio_codec,
                )
            }
            if (it.format.isNotNullOrBlank()) {
                TitleValueText(
                    stringResource(R.string.format),
                    it.format,
                )
            }
        }
        baseFile?.let {
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
