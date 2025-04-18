package com.github.damontecres.stashapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.isNotNullOrBlank

@Composable
fun ItemDetails(
    uiConfig: ComposeUiConfig,
    imageUrl: String?,
    tableRows: List<TableRow>,
    modifier: Modifier = Modifier,
    favorite: Boolean? = null,
    favoriteClick: (() -> Unit)? = null,
    rating100: Int? = null,
    rating100Click: ((rating100: Int) -> Unit)? = null,
    basicItemInfo: BasicItemInfo? = null,
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
                    StarRating(
                        rating100 = rating100 ?: 0,
                        precision = uiConfig.starPrecision,
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

@Composable
fun TableRowComposable(
    row: TableRow,
    modifier: Modifier = Modifier,
    keyWeight: Float = .3f,
    valueWeight: Float = .7f,
    focusable: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var background =
        if (isFocused) MaterialTheme.colorScheme.border.copy(alpha = .5f) else Color.Unspecified
    Row(modifier) {
        val keyModifier =
            Modifier
                .weight(keyWeight)
        val valueModifier =
            Modifier
                .weight(valueWeight)
                .focusable(
                    enabled = focusable || row.onClick != null, // TODO, this allows scrolling, but is difficult to see
                    interactionSource = interactionSource,
                ).ifElse(
                    row.onClick != null,
                    Modifier
                        .clickable(onClick = { row.onClick?.invoke() })
//                        .onFocusChanged { focused = it.isFocused }
                        .background(background),
                )
        ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground)) {
            Box(modifier = keyModifier) {
                row.key.invoke(this, Modifier.padding(4.dp))
            }
            Box(modifier = valueModifier) {
                row.value.invoke(this, Modifier.padding(4.dp))
            }
        }
    }
}

data class TableRow(
    val key: @Composable BoxScope.(modifier: Modifier) -> Unit,
    val value: @Composable BoxScope.(modifier: Modifier) -> Unit,
    val onClick: (() -> Unit)? = null,
) {
    constructor(key: String, value: String, onClick: (() -> Unit)? = null) : this(
        { modifier: Modifier -> Text(text = "$key:", modifier = modifier) },
        { modifier: Modifier -> Text(text = value, modifier = modifier) },
        onClick,
    )

    companion object {
        @Composable
        fun from(
            @StringRes keyStringId: Int,
            value: String?,
            onClick: (() -> Unit)? = null,
        ): TableRow? =
            if (value.isNotNullOrBlank()) {
                TableRow(stringResource(keyStringId), value, onClick)
            } else {
                null
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
