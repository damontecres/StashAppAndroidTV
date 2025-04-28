package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.titleOrFilename
import java.util.EnumMap
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun MarkerCard(
    uiConfig: ComposeUiConfig,
    item: MarkerData,
    onClick: (() -> Unit),
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
    modifier: Modifier = Modifier,
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.TAG] = item.tags.size

    val title =
        item.title.ifBlank {
            item.primary_tag.slimTagData.name
        }
    val startTime =
        item.seconds
            .toInt()
            .toDuration(DurationUnit.SECONDS)
            .toString()
    val subtitle =
        if (item.end_seconds != null) {
            "$startTime - ${item.end_seconds.toInt().toDuration(DurationUnit.SECONDS)}"
        } else {
            startTime
        }

    val imageUrl = item.screenshot
    val videoUrl = item.stream

    RootCard(
        item = item,
        modifier =
            modifier
                .padding(0.dp),
        onClick = onClick,
        longClicker = longClicker,
        getFilterAndPosition = getFilterAndPosition,
        uiConfig = uiConfig,
        imageWidth = MarkerPresenter.CARD_WIDTH.dp / 2,
        imageHeight = MarkerPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        defaultImageDrawableRes = R.drawable.default_scene,
        videoUrl = videoUrl,
        title = title,
        subtitle = {
            Column {
                Text(
                    text = subtitle,
                    maxLines = 1,
                    modifier = Modifier.enableMarquee(it),
                )
                Text(
                    text = item.scene.minimalSceneData.titleOrFilename ?: "",
                    maxLines = 1,
                    modifier = Modifier.enableMarquee(it),
                )
            }
        },
        description = {
            IconRowText(
                dataTypeMap,
                null,
                Modifier
                    .enableMarquee(it)
                    .align(Alignment.Center),
            )
        },
        imageOverlay = {},
    )
}
