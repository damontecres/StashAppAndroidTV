package com.github.damontecres.stashapp.ui

import android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.VideoFile
import com.github.damontecres.stashapp.ui.pages.SceneDetailsHeader

@Preview(
    widthDp = 1920 / 2,
    heightDp = 1080 / 2,
    uiMode = UI_MODE_TYPE_TELEVISION,
)
@Composable
private fun SceneDetailsPreview() {
    val scene =
        FullSceneData(
            id = "123",
            title = "Scene Title",
            code = "Scene Code",
            details =
                """
                Scene details
                Scene details
                Scene details
                Scene details
                Scene details
                Scene details
                Scene details
                """.trimIndent(),
            director = null,
            urls = listOf(),
            date = "2025-02-01",
            rating100 = 50,
            o_counter = 1,
            organized = true,
            resume_time = 5.5,
            play_duration = 100.12,
            play_count = 4,
            created_at = "2025-02-01 00:00:00",
            updated_at = "2025-02-01 00:00:00",
            files =
                listOf(
                    FullSceneData.File(
                        __typename = "",
                        videoFile =
                            VideoFile(
                                id = "1234",
                                path = "/path/to/file.mp4",
                                size = 1_000_000,
                                mod_time = "2025-02-01 00:00:00",
                                duration = 15 * 1000.0,
                                video_codec = "h264",
                                audio_codec = "aac",
                                format = "mkv",
                                width = 1920,
                                height = 1080,
                                frame_rate = 29.97,
                                bit_rate = 2_100_000,
                                __typename = "",
                            ),
                    ),
                ),
            paths =
                FullSceneData.Paths(
                    screenshot = null,
                    preview = null,
                    stream = null,
                    webp = null,
                    vtt = null,
                    sprite = null,
                    funscript = null,
                    interactive_heatmap = null,
                    caption = null,
                    __typename = "",
                ),
            sceneStreams =
                listOf(
                    FullSceneData.SceneStream(
                        url = "http://localhost",
                        mime_type = null,
                        label = "MP4",
                    ),
                ),
            captions = null,
            scene_markers = listOf(),
            galleries = listOf(),
            studio = null,
            groups = listOf(),
            tags = listOf(),
            performers = listOf(),
            __typename = "",
        )
    MainTheme {
//        SceneDetails(
//            scene = scene,
//            performers = listOf(),
//            galleries = listOf(),
//            uiConfig = ComposeUiConfig(true, 1.0f, true),
//            itemOnClick = { items, filterAndPosition -> },
//            playOnClick = { position, mode -> },
//            removeItem = {},
//            modifier = Modifier,
//            showRatingBar = true,
//            server = StashServer("", null),
//            rating100 = 60,
//            oCount = 1,
//            tags = listOf(),
//            groups = listOf(),
//            markers = listOf(),
//            addItem = { },
//            oCountAction = {  },
//        )
        SceneDetailsHeader(
            scene = scene,
            rating100 = 60,
            oCount = 1,
            uiConfig = ComposeUiConfig(true, 1f, true),
            itemOnClick = { _, _ -> },
            playOnClick = { _, _ -> },
            moreOnClick = {},
            oCounterOnClick = { },
            oCounterOnLongClick = {},
            onRatingChange = {},
            modifier = Modifier,
            showRatingBar = true,
        )
    }
}
