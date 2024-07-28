package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.TagPresenter
import java.util.EnumMap

@Suppress("ktlint:standard:function-naming")
@Composable
fun TagCard(
    item: TagData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.SCENE] = item.scene_count
    dataTypeMap[DataType.PERFORMER] = item.performer_count
    dataTypeMap[DataType.MARKER] = item.scene_marker_count
    dataTypeMap[DataType.IMAGE] = item.image_count
    dataTypeMap[DataType.GALLERY] = item.gallery_count

    val title = item.name
    val imageUrl = item.image_path
    val details = item.description ?: ""

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(TagPresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        imageWidth = TagPresenter.CARD_WIDTH.dp / 2,
        imageHeight = TagPresenter.CARD_HEIGHT.dp / 2,
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
            ImageOverlay {
                if (item.child_count > 0) {
                    val parentText =
                        stringResource(
                            R.string.stashapp_parent_of,
                            item.child_count.toString(),
                        )
                    Text(
                        modifier = Modifier.align(Alignment.TopStart),
                        text = parentText,
                    )
                }
                if (item.parent_count > 0) {
                    val childText =
                        stringResource(
                            R.string.stashapp_sub_tag_of,
                            item.parent_count.toString(),
                        )
                    Text(
                        modifier = Modifier.align(Alignment.BottomStart),
                        text = childText,
                    )
                }
            }
        },
    )
}
