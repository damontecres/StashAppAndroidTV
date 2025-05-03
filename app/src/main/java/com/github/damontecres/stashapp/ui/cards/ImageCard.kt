package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.presenters.ImagePresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.titleOrFilename
import java.util.EnumMap

@Composable
fun ImageCard(
    uiConfig: ComposeUiConfig,
    item: ImageData,
    onClick: (() -> Unit),
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
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
        item = item,
        modifier =
            modifier
                .padding(0.dp),
        onClick = onClick,
        longClicker = longClicker,
        getFilterAndPosition = getFilterAndPosition,
        uiConfig = uiConfig,
        imageWidth = ImagePresenter.CARD_WIDTH.dp / 2,
        imageHeight = ImagePresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        defaultImageDrawableRes = R.drawable.default_image,
        videoUrl = item.paths.preview,
        title = item.titleOrFilename ?: "",
        subtitle = {
            Text(concatIfNotBlank(" - ", details))
        },
        description = {
            IconRowText(
                dataTypeMap,
                item.o_counter ?: -1,
                Modifier
                    .enableMarquee(it)
                    .align(Alignment.Center),
            )
        },
        imageOverlay = {
            ImageOverlay(uiConfig.ratingAsStars, rating100 = item.rating100)
        },
    )
}
