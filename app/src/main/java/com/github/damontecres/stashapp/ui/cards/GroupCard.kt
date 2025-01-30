package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.GroupPresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.LongClicker
import java.util.EnumMap

@Composable
fun GroupCard(
    uiConfig: ComposeUiConfig,
    item: GroupData,
    onClick: (() -> Unit),
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.SCENE] = item.scene_count
    dataTypeMap[DataType.TAG] = item.tags.size

    val title = item.name
    val imageUrl = item.front_image_path
    val details = subtitle ?: item.date ?: ""

    RootCard(
        item = item,
        modifier =
            modifier
                .padding(0.dp)
                .width(GroupPresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        longClicker = longClicker,
        imageWidth = GroupPresenter.CARD_WIDTH.dp / 2,
        imageHeight = GroupPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        videoUrl = null,
        title = title,
        subtitle = {
            Text(details)
        },
        description = {
            IconRowText(dataTypeMap, null)
        },
        imageOverlay = {
            ImageOverlay(uiConfig.ratingAsStars, rating100 = item.rating100)
        },
    )
}
