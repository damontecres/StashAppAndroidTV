package com.github.damontecres.stashapp.ui.cards

import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.util.ageInYears
import java.util.EnumMap

@Composable
fun PerformerCard(
    uiConfig: ComposeUiConfig,
    item: PerformerData,
    onClick: (() -> Unit),
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.SCENE] = item.scene_count
    dataTypeMap[DataType.TAG] = item.tags.size
    dataTypeMap[DataType.GROUP] = item.group_count
    dataTypeMap[DataType.IMAGE] = item.image_count
    dataTypeMap[DataType.GALLERY] = item.gallery_count

    RootCard(
        item = item,
        modifier =
            modifier
                .padding(0.dp)
                .width(PerformerPresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        longClicker = longClicker,
        imageWidth = PerformerPresenter.CARD_WIDTH.dp / 2,
        imageHeight = PerformerPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = item.image_path,
        title = item.name,
        subtitle = {
            if (item.birthdate != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val yearsOldStr = stringResource(R.string.stashapp_years_old)
                Text(text = "${item.ageInYears} $yearsOldStr")
            } else {
                Text("")
            }
        },
        description = {
            IconRowText(dataTypeMap, item.o_counter ?: -1)
        },
        imageOverlay = {
            ImageOverlay(uiConfig.ratingAsStars, favorite = item.favorite)
        },
    )
}
