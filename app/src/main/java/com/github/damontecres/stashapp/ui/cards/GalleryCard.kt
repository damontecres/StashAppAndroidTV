package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.name
import java.util.EnumMap

@Composable
fun GalleryCard(
    uiConfig: ComposeUiConfig,
    item: GalleryData?,
    onClick: (() -> Unit),
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
    modifier: Modifier = Modifier,
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    item?.let {
        dataTypeMap[DataType.TAG] = item.tags.size
        dataTypeMap[DataType.PERFORMER] = item.performers.size
        dataTypeMap[DataType.SCENE] = item.scenes.size
        dataTypeMap[DataType.IMAGE] = item.image_count
    }

    val imageUrl = item?.paths?.cover
    val videoUrl = item?.paths?.preview

    val details = mutableListOf<String?>()
    details.add(item?.studio?.name)
    details.add(item?.date)

    RootCard(
        item = item,
        modifier =
            modifier
                .padding(0.dp),
        onClick = onClick,
        longClicker = longClicker,
        getFilterAndPosition = getFilterAndPosition,
        uiConfig = uiConfig,
        imageWidth = GalleryPresenter.CARD_WIDTH.dp / 2,
        imageHeight = GalleryPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        defaultImageDrawableRes = R.drawable.default_gallery,
        videoUrl = videoUrl,
        title = item?.name ?: "",
        subtitle = {
            Text(concatIfNotBlank(" - ", details))
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
        imageOverlay = {
            ImageOverlay(uiConfig.ratingAsStars, rating100 = item?.rating100)
        },
    )
}
