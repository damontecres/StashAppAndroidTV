package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.util.concatIfNotBlank
import java.util.EnumMap

@Suppress("ktlint:standard:function-naming")
@Composable
fun GalleryCard(
    item: GalleryData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.TAG] = item.tags.size
    dataTypeMap[DataType.PERFORMER] = item.performers.size
    dataTypeMap[DataType.SCENE] = item.scenes.size
    dataTypeMap[DataType.IMAGE] = item.image_count

    val imageUrl = item.cover?.paths?.thumbnail
    val videoUrl = item.cover?.paths?.preview

    val details = mutableListOf<String?>()
    details.add(item.studio?.name)
    details.add(item.date)

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(GalleryPresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        imageWidth = GalleryPresenter.CARD_WIDTH.dp / 2,
        imageHeight = GalleryPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        title = item.title ?: "",
        subtitle = {
            Text(concatIfNotBlank(" - ", details))
        },
        description = {
            IconRowText(dataTypeMap, null)
        },
        imageOverlay = {
            ImageOverlay(rating100 = item.rating100)
        },
    )
}
