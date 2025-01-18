package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.ImagePresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import java.util.EnumMap

@Composable
fun ImageCard(
    uiConfig: ComposeUiConfig,
    item: ImageData,
    onClick: (() -> Unit),
    modifier: Modifier = Modifier,
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.TAG] = item.tags.size
    dataTypeMap[DataType.PERFORMER] = item.performers.size
    dataTypeMap[DataType.GALLERY] = item.galleries.size

    val imageUrl =
        if (item.paths.thumbnail.isNotNullOrBlank()) {
            item.paths.thumbnail
        } else if (item.paths.image.isNotNullOrBlank() && !item.isImageClip) {
            item.paths.image
        } else {
            null
        }

    val details = mutableListOf<String?>()
    details.add(item.studio?.name)
    details.add(item.date)

    RootCard(
        modifier =
            modifier
                .padding(0.dp)
                .width(ImagePresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        imageWidth = ImagePresenter.CARD_WIDTH.dp / 2,
        imageHeight = ImagePresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        videoUrl = item.paths.preview,
        title = item.title ?: "",
        subtitle = {
            Text(concatIfNotBlank(" - ", details))
        },
        description = {
            IconRowText(dataTypeMap, item.o_counter ?: -1)
        },
        imageOverlay = {
            ImageOverlay(uiConfig.ratingAsStars, rating100 = item.rating100)
        },
    )
}
