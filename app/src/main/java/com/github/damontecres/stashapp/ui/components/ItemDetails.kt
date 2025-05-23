package com.github.damontecres.stashapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.titleCount
import com.github.damontecres.stashapp.util.isNotNullOrBlank

@Composable
fun ItemDetails(
    uiConfig: ComposeUiConfig,
    imageUrl: String?,
    tableRows: List<TableRow>,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    favorite: Boolean? = null,
    favoriteClick: (() -> Unit)? = null,
    rating100: Int? = null,
    rating100Click: ((rating100: Int) -> Unit)? = null,
    basicItemInfo: BasicItemInfo? = null,
    tags: List<TagData>? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        if (imageUrl.isNotNullOrBlank()) {
            AsyncImage(
                modifier =
                    Modifier
                        .padding(12.dp)
                        .fillMaxWidth(.5f)
                        .fillMaxHeight(),
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
        LazyColumn(modifier = Modifier.padding(12.dp)) {
            if (favorite != null && favoriteClick != null) {
                val color = if (favorite)Color.Red else Color.LightGray
                item {
                    ProvideTextStyle(MaterialTheme.typography.displayLarge.copy(color = color)) {
                        Button(
                            onClick = favoriteClick,
                        ) {
                            Text(text = stringResource(R.string.fa_heart), fontFamily = FontAwesome)
                        }
                    }
                }
            }
            if (rating100Click != null) {
                item {
                    Rating100(
                        rating100 = rating100 ?: 0,
                        uiConfig = uiConfig,
                        onRatingChange = rating100Click,
                        enabled = true,
                        modifier =
                            Modifier
                                .height(32.dp)
                                .padding(start = 12.dp),
                    )
                }
            }
            items(tableRows) { row ->
                TableRowComposable(row)
            }
            if (!tags.isNullOrEmpty()) {
                item {
                    ItemsRow(
                        title = titleCount(R.string.stashapp_tags, tags),
                        items = tags,
                        uiConfig = uiConfig,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            basicItemInfo?.let {
                item {
                    ItemDetailsFooter(
                        id = it.id,
                        createdAt = it.createdAt?.toString(),
                        updatedAt = it.updatedAt?.toString(),
                        modifier =
                            Modifier
                                .padding(top = 32.dp)
                                .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

data class BasicItemInfo(
    val id: String,
    val createdAt: Any?,
    val updatedAt: Any?,
)

@Composable
fun ItemDetailsFooter(
    id: String,
    createdAt: String?,
    updatedAt: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.Start),
    ) {
        if (createdAt != null && createdAt.length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_created_at),
                createdAt.substring(0..<10),
            )
        }
        if (updatedAt != null && updatedAt.length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_updated_at),
                updatedAt.substring(0..<10),
            )
        }
        TitleValueText(stringResource(R.string.id), id)
    }
}
