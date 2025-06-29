package com.github.damontecres.stashapp.ui.components.scene

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.playback.displayString
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.util.bitRateString
import com.github.damontecres.stashapp.views.formatBytes
import java.util.Locale

@Composable
fun SceneDetailsFooter(
    scene: FullSceneData,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (scene.created_at.toString().length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_created_at),
                scene.created_at.toString().substring(0..<10),
            )
        }
        if (scene.updated_at.toString().length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_updated_at),
                scene.updated_at.toString().substring(0..<10),
            )
        }
        TitleValueText(stringResource(R.string.stashapp_scene_id), scene.id)
        val file = scene.files.firstOrNull()?.videoFile
        if (file != null) {
            TitleValueText(
                stringResource(R.string.stashapp_video_codec),
                file.video_codec.ifBlank { stringResource(R.string.stashapp_none) },
            )
            TitleValueText(
                stringResource(R.string.stashapp_audio_codec),
                file.audio_codec.ifBlank { stringResource(R.string.stashapp_none) },
            )
            TitleValueText(
                stringResource(R.string.format),
                file.format,
            )
            TitleValueText(
                stringResource(R.string.stashapp_bitrate),
                file.bitRateString().toString(),
            )
            TitleValueText(
                stringResource(R.string.stashapp_framerate),
                String.format(Locale.getDefault(), "%.2f", file.frame_rate),
            )
            TitleValueText(
                stringResource(R.string.stashapp_filesize),
                file.size
                    .toString()
                    .toLongOrNull()
                    ?.let { bytes -> formatBytes(bytes) } ?: file.size.toString(),
            )
        }
        if (!scene.captions.isNullOrEmpty()) {
            val str =
                buildString {
                    append(
                        scene.captions
                            .first()
                            .caption
                            .displayString(LocalContext.current),
                    )
                    if (scene.captions.size > 1) {
                        append(", +${scene.captions.size - 1} more")
                    }
                }
            TitleValueText(
                stringResource(R.string.stashapp_captions),
                str,
            )
        }
        TitleValueText(
            stringResource(R.string.stashapp_organized),
            scene.organized.toString(),
        )
    }
}
