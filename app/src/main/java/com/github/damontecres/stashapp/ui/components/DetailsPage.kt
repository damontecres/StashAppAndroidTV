package com.github.damontecres.stashapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.ui.faFontFamily
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.StashRatingBar

@Composable
fun DetailsPage(
    imageUrl: String?,
    tableRows: List<TableRow>,
    modifier: Modifier = Modifier,
    favorite: Boolean? = null,
    favoriteClick: (() -> Unit)? = null,
    rating100: Int? = null,
    rating100Click: ((rating100: Int) -> Unit)? = null,
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
                contentScale = ContentScale.FillHeight,
            )
        }
        val keyModifier =
            Modifier
                .weight(.3f)
        val valueModifier =
            Modifier
                .weight(.7f)
        LazyColumn(modifier = Modifier.padding(12.dp)) {
            if (favorite != null && favoriteClick != null) {
                val color = if (favorite)Color.Red else Color.LightGray
                item {
                    ProvideTextStyle(MaterialTheme.typography.displayLarge.copy(color = color)) {
                        Button(
                            onClick = favoriteClick,
                        ) {
                            Text(text = stringResource(R.string.fa_heart), fontFamily = faFontFamily)
                        }
                    }
                }
            }
            if (rating100Click != null) {
                item {
                    AndroidView(
                        factory = { context ->
                            val ratingBar = StashRatingBar(context)
                            ratingBar.rating100 = rating100 ?: 0
                            ratingBar.setRatingCallback(rating100Click)
                            ratingBar
                        },
                    )
                }
            }
            items(tableRows) { (key, value) ->
                Row(Modifier.fillMaxWidth()) {
                    ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground)) {
                        Box(modifier = keyModifier) {
                            key.invoke(this, Modifier.padding(4.dp))
                        }
                        Box(modifier = valueModifier) {
                            value.invoke(this, Modifier.padding(4.dp))
                        }
                    }
                }
            }
        }
    }
}

data class TableRow(
    val key: @Composable BoxScope.(modifier: Modifier) -> Unit,
    val value: @Composable BoxScope.(modifier: Modifier) -> Unit,
) {
    constructor(key: String, value: String) : this(
        { modifier: Modifier -> Text(text = "$key:", modifier = modifier) },
        { modifier: Modifier -> Text(text = value, modifier = modifier) },
    )

    constructor(
        @StringRes keyStringId: Int,
        value: String,
    ) : this(StashApplication.getApplication().getString(keyStringId), value)

    companion object {
        fun from(
            @StringRes keyStringId: Int,
            value: String?,
        ): TableRow? =
            if (value.isNotNullOrBlank()) {
                TableRow(keyStringId, value)
            } else {
                null
            }
    }
}
