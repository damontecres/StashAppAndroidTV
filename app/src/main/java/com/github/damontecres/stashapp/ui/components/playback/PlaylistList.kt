package com.github.damontecres.stashapp.ui.components.playback

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.PlaylistItem
import com.github.damontecres.stashapp.ui.MainTheme
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.isNotNullOrBlank

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistList(
    mediaItemCount: Int,
    mediaItemCountOffset: Int,
    currentIndex: Int,
    title: String,
    items: List<PlaylistItem>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LazyColumn(
        state = state,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.padding(8.dp),
    ) {
        stickyHeader {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(400.dp),
            )
        }
        items(count = mediaItemCount, key = { it }) {
            val index = mediaItemCountOffset + it
            PlaylistItemCompose(
                item = items[index],
                imageWidth = 120.dp,
                onClick = { onClick.invoke(index) },
                modifier =
                    Modifier
                        .size(400.dp, 88.dp)
                        .ifElse(index == currentIndex, Modifier.focusRequester(focusRequester)),
            )
        }
    }
    LaunchedEffect(Unit) {
        state.scrollToItem(currentIndex)
        focusRequester.requestFocus()
    }
}

@Composable
fun PlaylistItemCompose(
    item: PlaylistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageWidth: Dp = 120.dp,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = item.index.toString(),
                color = MaterialTheme.colorScheme.onSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
            if (item.imageUrl.isNotNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(imageWidth),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.default_scene),
                    contentDescription = null,
                    modifier = Modifier.width(imageWidth),
                )
            }
            Column(
                modifier =
                    Modifier
                        .padding(end = 8.dp)
                        .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (item.title.isNotNullOrBlank()) {
                    Text(
                        text = item.title.toString(),
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.subtitle.isNotNullOrBlank()) {
                    Text(
                        text = item.subtitle.toString(),
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.details1.isNotNullOrBlank()) {
                    Text(
                        text = item.details1.toString(),
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.details2.isNotNullOrBlank()) {
                    Text(
                        text = item.details2.toString(),
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 600)
@Composable
private fun PlaylistItemPreview() {
    MainTheme {
        PlaylistList(
            mediaItemCount = 10,
            mediaItemCountOffset = 0,
            currentIndex = 2,
            title = "Playlist Title",
            items =
                List(10) {
                    PlaylistItem(
                        index = it,
                        imageUrl = null,
                        title = "This is the scene title",
                        subtitle = "2024-01-01",
                        details1 = "This is the main scene details and description",
                        details2 = "This is the secondary scene details and description",
                    )
                },
            onClick = {},
            modifier = Modifier,
        )
    }
}
