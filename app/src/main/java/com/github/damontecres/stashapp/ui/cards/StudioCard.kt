package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.StudioPresenter
import java.util.EnumMap

@Suppress("ktlint:standard:function-naming")
@Composable
fun StudioCard(
    item: StudioData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.SCENE] = item.scene_count
    dataTypeMap[DataType.PERFORMER] = item.performer_count
    dataTypeMap[DataType.MOVIE] = item.movie_count
    dataTypeMap[DataType.IMAGE] = item.image_count
    dataTypeMap[DataType.GALLERY] = item.gallery_count

    val title = item.name
    val imageUrl = item.image_path
    val details =
        if (item.parent_studio != null) {
            stringResource(R.string.stashapp_part_of, item.parent_studio.name)
        } else {
            ""
        }

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(StudioPresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        imageWidth = StudioPresenter.CARD_WIDTH.dp / 2,
        imageHeight = StudioPresenter.CARD_HEIGHT.dp / 2,
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
            ImageOverlay(rating100 = item.rating100)
        },
    )
}
