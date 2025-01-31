package com.github.damontecres.stashapp.ui.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.presenters.GroupPresenter
import com.github.damontecres.stashapp.presenters.ImagePresenter
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.components.LongClicker

@Composable
fun ViewAllCard(
    filter: FilterArgs,
    itemOnClick: (Any) -> Unit,
    longClicker: LongClicker<Any>,
    getFilterAndPosition: (item: Any) -> FilterAndPosition,
    modifier: Modifier = Modifier,
) {
    val width =
        when (filter.dataType) {
            DataType.GALLERY -> GalleryPresenter.CARD_WIDTH
            DataType.IMAGE -> ImagePresenter.CARD_WIDTH
            DataType.MARKER -> MarkerPresenter.CARD_WIDTH
            DataType.GROUP -> GroupPresenter.CARD_WIDTH
            DataType.PERFORMER -> PerformerPresenter.CARD_WIDTH
            DataType.SCENE -> ScenePresenter.CARD_WIDTH
            DataType.STUDIO -> StudioPresenter.CARD_WIDTH
            DataType.TAG -> TagPresenter.CARD_WIDTH
        }
    val height =
        when (filter.dataType) {
            DataType.GALLERY -> GalleryPresenter.CARD_HEIGHT
            DataType.IMAGE -> ImagePresenter.CARD_HEIGHT
            DataType.MARKER -> MarkerPresenter.CARD_HEIGHT
            DataType.GROUP -> GroupPresenter.CARD_HEIGHT
            DataType.PERFORMER -> PerformerPresenter.CARD_HEIGHT
            DataType.SCENE -> ScenePresenter.CARD_HEIGHT
            DataType.STUDIO -> StudioPresenter.CARD_HEIGHT
            DataType.TAG -> TagPresenter.CARD_HEIGHT
        }

    RootCard(
        item = filter,
        modifier =
            modifier
                .padding(0.dp)
                .width(width.dp / 2),
        onClick = {
            itemOnClick(filter)
        },
        longClicker = longClicker,
        getFilterAndPosition = getFilterAndPosition,
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
        title = stringResource(R.string.stashapp_view_all),
        subtitle = {
            Text("")
        },
        description = {
            Text("")
        },
    )
}
