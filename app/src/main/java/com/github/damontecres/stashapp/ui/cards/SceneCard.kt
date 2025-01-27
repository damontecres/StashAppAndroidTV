package com.github.damontecres.stashapp.ui.cards

import android.util.Log
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
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter.PopUpItem
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.resume_position
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import java.util.EnumMap

@Composable
fun SceneCard(
    uiConfig: ComposeUiConfig,
    item: SlimSceneData,
    onClick: (() -> Unit),
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.TAG] = item.tags.size
    dataTypeMap[DataType.PERFORMER] = item.performers.size
    dataTypeMap[DataType.GROUP] = item.groups.size
    dataTypeMap[DataType.MARKER] = item.scene_markers.size
    dataTypeMap[DataType.GALLERY] = item.galleries.size

    longClicker
        .addAction(
            PopUpItem(2, "Play Scene"),
            {
                Log.v("Compose", "Checking for play scene")
                it is SlimSceneData && (it.resume_position == null || it.resume_position!! <= 0)
            },
        ) {
            StashApplication.navigationManager.navigate(
                Destination.Playback(
                    (it as SlimSceneData).id,
                    0L,
                    PlaybackMode.CHOOSE,
                ),
            )
        }.addAction(
            PopUpItem(3, "Resume Scene"),
            { it is SlimSceneData && (it.resume_position != null && it.resume_position!! > 0) },
        ) {
            StashApplication.navigationManager.navigate(
                Destination.Playback(
                    (it as SlimSceneData).id,
                    (it as SlimSceneData).resume_position ?: 0L,
                    PlaybackMode.CHOOSE,
                ),
            )
        }.addAction(
            PopUpItem(4, "Restart Scene"),
            { it is SlimSceneData && (it.resume_position != null && it.resume_position!! > 0) },
        ) {
            StashApplication.navigationManager.navigate(
                Destination.Playback(
                    (it as SlimSceneData).id,
                    0L,
                    PlaybackMode.CHOOSE,
                ),
            )
        }

    RootCard(
        item = item,
        modifier =
            modifier
                .padding(0.dp)
                .width(ScenePresenter.CARD_WIDTH.dp / 2),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
        longClicker = longClicker,
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
            ImageOverlay(uiConfig.ratingAsStars, rating100 = item.rating100) {
                val videoFile = item.files.firstOrNull()?.videoFile
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
                                    ).clip(RectangleShape)
                                    .height(4.dp)
                                    .width((ScenePresenter.CARD_WIDTH * percentWatched).dp / 2),
                        )
                    }
                }
            }
        },
    )
}
