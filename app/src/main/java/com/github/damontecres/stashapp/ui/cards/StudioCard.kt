package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.enableMarquee
import java.util.EnumMap

@Composable
fun StudioCard(
    uiConfig: ComposeUiConfig,
    item: StudioData?,
    onClick: (() -> Unit),
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
    modifier: Modifier = Modifier,
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    item?.let {
        dataTypeMap[DataType.SCENE] = item.scene_count
        dataTypeMap[DataType.PERFORMER] = item.performer_count
        dataTypeMap[DataType.GROUP] = item.group_count
        dataTypeMap[DataType.IMAGE] = item.image_count
        dataTypeMap[DataType.GALLERY] = item.gallery_count
        dataTypeMap[DataType.TAG] = item.tags.size
    }

    val title = item?.name ?: ""
    val imageUrl = item?.image_path
    val details =
        if (item?.parent_studio != null) {
            stringResource(R.string.stashapp_part_of, item.parent_studio.name)
        } else {
            ""
        }

    RootCard(
        item = item,
        modifier =
            modifier
                .padding(0.dp),
        onClick = onClick,
        longClicker = longClicker,
        getFilterAndPosition = getFilterAndPosition,
        uiConfig = uiConfig,
        imageWidth = StudioPresenter.CARD_WIDTH.dp / 2,
        imageHeight = StudioPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        imagePadding = 8.dp,
        defaultImageDrawableRes = R.drawable.default_studio,
        videoUrl = null,
        title = title,
        subtitle = {
            Text(details)
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
            item?.let {
                ImageOverlay(
                    uiConfig.ratingAsStars,
                    favorite = item.favorite,
                    rating100 = item.rating100,
                )
            }
        },
    )
}
