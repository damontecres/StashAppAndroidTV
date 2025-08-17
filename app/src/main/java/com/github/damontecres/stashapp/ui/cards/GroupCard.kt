package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.presenters.GroupPresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.enableMarquee
import java.util.EnumMap

@Composable
fun GroupCard(
    uiConfig: ComposeUiConfig,
    item: GroupData?,
    onClick: (() -> Unit),
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    item?.let {
        dataTypeMap[DataType.SCENE] = item.scene_count
        dataTypeMap[DataType.PERFORMER] = item.performer_count
        dataTypeMap[DataType.TAG] = item.tags.size
    }

    val title = item?.name ?: ""
    val imageUrl = item?.front_image_path
    val details = subtitle ?: item?.date ?: ""

    RootCard(
        item = item,
        modifier =
            modifier
                .padding(0.dp),
        onClick = onClick,
        longClicker = longClicker,
        getFilterAndPosition = getFilterAndPosition,
        uiConfig = uiConfig,
        imageWidth = GroupPresenter.CARD_WIDTH.dp / 2,
        imageHeight = GroupPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        defaultImageDrawableRes = R.drawable.default_group,
        videoUrl = null,
        title = AnnotatedString(title),
        subtitle = {
            Text(details)
        },
        description = { focused ->
            item?.let {
                IconRowText(
                    dataTypeMap,
                    null,
                    Modifier
                        .enableMarquee(focused)
                        .align(Alignment.Center),
                ) {
                    if (item.containing_groups.isNotEmpty() || item.sub_group_count > 0) {
                        if (length > 0) {
                            append(" ")
                        }
                        withStyle(SpanStyle(fontFamily = FontAwesome)) {
                            append(stringResource(DataType.GROUP.iconStringId))
                        }
                        append(" ")
                        if (item.containing_groups.isNotEmpty()) {
                            append(item.containing_groups.size.toString())
                            withStyle(SpanStyle(fontFamily = FontAwesome)) {
                                append(stringResource(R.string.fa_arrow_up_long))
                            }
                        }
                        if (item.sub_group_count > 0) {
                            append(item.sub_group_count.toString())
                            withStyle(SpanStyle(fontFamily = FontAwesome)) {
                                append(stringResource(R.string.fa_arrow_down_long))
                            }
                        }
                    }
                }
            }
        },
        imageOverlay = {
            ImageOverlay(uiConfig.ratingAsStars, rating100 = item?.rating100)
        },
    )
}
