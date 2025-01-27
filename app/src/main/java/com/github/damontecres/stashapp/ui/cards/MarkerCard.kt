package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.LongClicker
import java.util.EnumMap
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun MarkerCard(
    uiConfig: ComposeUiConfig,
    item: MarkerData,
    onClick: (() -> Unit),
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.TAG] = item.tags.size

    val title =
        item.title.ifBlank {
            item.primary_tag.slimTagData.name
        } + " - ${item.seconds.toInt().toDuration(DurationUnit.SECONDS)}"

    val imageUrl = item.screenshot
    val videoUrl = item.preview

    val details = if (item.title.isNotBlank()) item.primary_tag.slimTagData.name else ""

    RootCard(
        item = item,
        modifier =
            modifier
                .padding(0.dp)
                .width(MarkerPresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        longClicker = longClicker,
        imageWidth = MarkerPresenter.CARD_WIDTH.dp / 2,
        imageHeight = MarkerPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        title = title,
        subtitle = {
            Text(details)
        },
        description = {
            IconRowText(dataTypeMap, null)
        },
        imageOverlay = {},
    )
}
