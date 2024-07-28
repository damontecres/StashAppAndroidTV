package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.MoviePresenter
import java.util.EnumMap

@Suppress("ktlint:standard:function-naming")
@Composable
fun MovieCard(
    item: MovieData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.SCENE] = item.scene_count

    val title = item.name
    val imageUrl = item.front_image_path
    val details = item.date ?: ""

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(MoviePresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        imageWidth = MoviePresenter.CARD_WIDTH.dp / 2,
        imageHeight = MoviePresenter.CARD_HEIGHT.dp / 2,
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
