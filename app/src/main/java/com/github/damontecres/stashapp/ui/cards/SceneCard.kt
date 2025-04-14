package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.durationToString
import java.util.EnumMap

@Composable
fun SceneCard(
    uiConfig: ComposeUiConfig,
    item: SlimSceneData?,
    onClick: (() -> Unit),
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
    modifier: Modifier = Modifier,
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    item?.let {
        dataTypeMap[DataType.TAG] = item.tags.size
        dataTypeMap[DataType.PERFORMER] = item.performers.size
        dataTypeMap[DataType.GROUP] = item.groups.size
        dataTypeMap[DataType.MARKER] = item.scene_markers.size
        dataTypeMap[DataType.GALLERY] = item.galleries.size
    }

    RootCard(
        item = item,
        modifier =
            modifier
                .padding(0.dp),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
        longClicker = longClicker,
        getFilterAndPosition = getFilterAndPosition,
        uiConfig = uiConfig,
        imageWidth = ScenePresenter.CARD_WIDTH.dp / 2,
        imageHeight = ScenePresenter.CARD_HEIGHT.dp / 2,
        imageUrl = item?.paths?.screenshot,
        defaultImageDrawableRes = R.drawable.default_scene,
        videoUrl = item?.paths?.preview,
        title = item?.titleOrFilename ?: "",
        subtitle = { Text(item?.date ?: "") },
        description = {
            IconRowText(
                dataTypeMap,
                item?.o_counter ?: -1,
                Modifier
                    .enableMarquee(it)
                    .align(Alignment.Center),
            )
        },
        imageOverlay = {
            ImageOverlay(uiConfig.ratingAsStars, rating100 = item?.rating100) {
                val videoFile = item?.files?.firstOrNull()?.videoFile
                if (videoFile != null) {
                    val duration = durationToString(videoFile.duration)
                    Text(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp),
                        text = duration,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        text = videoFile.resolutionName().toString(),
                    )
                    if (item.resume_time != null) {
                        val percentWatched = item.resume_time / videoFile.duration
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .background(
                                        MaterialTheme.colorScheme.tertiary, // TODO?
                                    ).clip(RectangleShape)
                                    .height(4.dp)
                                    .width((ScenePresenter.CARD_WIDTH * percentWatched).dp / 2),
                        )
                    }
                }
                if (item?.studio != null) {
                    val imageUrl = item.studio.image_path
                    if (!uiConfig.showStudioAsText &&
                        imageUrl.isNotNullOrBlank() &&
                        !imageUrl.contains(
                            "default=true",
                        )
                    ) {
                        AsyncImage(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .fillMaxWidth(.4f),
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            text = item.studio.name,
                        )
                    }
                }
            }
        },
    )
}
