package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.LongClicker

fun dataTypeImageWidth(dataType: DataType) =
    when (dataType) {
        DataType.GALLERY -> 345
        DataType.IMAGE -> 345
        DataType.MARKER -> 345
        DataType.GROUP -> 250
        DataType.PERFORMER -> 254
        DataType.SCENE -> 345
        DataType.STUDIO -> 345
        DataType.TAG -> 250
    }

fun dataTypeImageHeight(dataType: DataType) =
    when (dataType) {
        DataType.GALLERY -> 145
        DataType.IMAGE -> 145
        DataType.MARKER -> 194
        DataType.GROUP -> 250
        DataType.PERFORMER -> 381
        DataType.SCENE -> 194
        DataType.STUDIO -> 194
        DataType.TAG -> 250
    }

@Composable
fun ViewAllCard(
    filter: FilterArgs,
    itemOnClick: (Any) -> Unit,
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    val width = dataTypeImageWidth(filter.dataType)
    val height = dataTypeImageHeight(filter.dataType)

    RootCard(
        item = filter,
        modifier =
            modifier
                .padding(0.dp),
        onClick = {
            itemOnClick(filter)
        },
        longClicker = longClicker,
        getFilterAndPosition = getFilterAndPosition,
        uiConfig = uiConfig,
        imageWidth = width.dp / 2,
        imageContent = {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.baseline_camera_indoor_48),
                contentDescription = "",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        },
        imageHeight = height.dp / 2,
        imageUrl = null,
        videoUrl = null,
        title = AnnotatedString(stringResource(R.string.stashapp_view_all)),
        subtitle = {
            Text("")
        },
        description = {
            Text("")
        },
    )
}
