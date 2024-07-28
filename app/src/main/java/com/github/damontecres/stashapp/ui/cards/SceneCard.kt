package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import java.util.EnumMap

@Suppress("ktlint:standard:function-naming")
@Composable
fun SceneCard(
    item: SlimSceneData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.TAG] = item.tags.size
    dataTypeMap[DataType.PERFORMER] = item.performers.size
    dataTypeMap[DataType.MOVIE] = item.movies.size
    dataTypeMap[DataType.MARKER] = item.scene_markers.size
    dataTypeMap[DataType.GALLERY] = item.galleries.size

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(ScenePresenter.CARD_WIDTH.dp / 2),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
        imageWidth = ScenePresenter.CARD_WIDTH.dp / 2,
        imageHeight = ScenePresenter.CARD_HEIGHT.dp / 2,
        imageUrl = item.paths.screenshot,
        videoUrl = item.paths.preview,
        title = item.titleOrFilename ?: "",
        subtitle = { Text(item.date ?: "") },
        description = {
            IconRowText(dataTypeMap, item.o_counter ?: -1)
        },
        imageOverlay = {
            ImageOverlay(item.rating100) {
                val videoFile = item.files.firstOrNull()?.videoFileData
                if (videoFile != null) {
                    val duration = durationToString(videoFile.duration)
                    Text(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp),
                        text = duration,
                    )
                    Text(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp),
                        style = TextStyle(fontWeight = FontWeight.Bold),
                        text = videoFile.resolutionName().toString(),
                    )
                    if (item.resume_time != null) {
                        val percentWatched = item.resume_time / videoFile.duration
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .background(
                                        Color.White,
                                    )
                                    .clip(RectangleShape)
                                    .height(4.dp)
                                    .width((ScenePresenter.CARD_WIDTH * percentWatched).dp / 2),
                        )
                    }
                }
            }
        },
    )
}
