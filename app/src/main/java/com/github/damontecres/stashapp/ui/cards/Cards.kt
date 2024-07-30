package com.github.damontecres.stashapp.ui.cards

import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import androidx.tv.material3.Card
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardGlow
import androidx.tv.material3.CardScale
import androidx.tv.material3.CardShape
import androidx.tv.material3.ClassicCard
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.StashImageCardView.Companion.ICON_ORDER
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.asSlimeSceneData
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.getRatingAsDecimalString
import java.util.EnumMap

@Suppress("ktlint:standard:function-naming")
@Composable
fun ImageOverlay(
    rating100: Int? = null,
    favorite: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val showRatings =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.pref_key_show_rating), true)

    Box(modifier = Modifier.fillMaxSize()) {
        if (showRatings && rating100 != null && rating100 >= 0) {
            val ratingText = getRatingAsDecimalString(context, rating100)
            val text = context.getString(R.string.stashapp_rating) + ": $ratingText"
            val ratingColors = context.resources.obtainTypedArray(R.array.rating_colors)
            val bgColor = ratingColors.getColor(rating100 / 5, Color.WHITE)
            ratingColors.recycle()

            Text(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .background(color = androidx.compose.ui.graphics.Color(bgColor))
                        .padding(4.dp),
                style = TextStyle(fontWeight = FontWeight.Bold),
                text = text,
            )
        }
        if (favorite) {
            val faFontFamily =
                FontFamily(
                    Font(
                        resId = R.font.fa_solid_900,
                    ),
                )
            Text(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                color = colorResource(android.R.color.holo_red_light),
                text = stringResource(R.string.fa_heart),
                fontSize = 20.sp,
                fontFamily = faFontFamily,
            )
        }
        content.invoke(this)
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun IconRowText(
    iconMap: EnumMap<DataType, Int>,
    oCounter: Int?,
) {
    val faFontFamily =
        FontFamily(
            Font(
                resId = R.font.fa_solid_900,
            ),
        )

    val annotatedString =
        buildAnnotatedString {
            ICON_ORDER.forEach {
                val count = iconMap[it]
                if (count != null && count > 0) {
                    withStyle(SpanStyle(fontFamily = faFontFamily)) {
                        append(stringResource(it.iconStringId))
                    }
                    append(" $count")
                    append("  ")
                }
            }
            if (oCounter != null && oCounter > 0) {
                appendInlineContent(id = "ocounter", "O")
                append(" $oCounter")
            }
        }
    val fontSize = MaterialTheme.typography.bodySmall.fontSize
    val inlineContentMap =
        mapOf(
            "ocounter" to
                InlineTextContent(
                    Placeholder(fontSize, fontSize, PlaceholderVerticalAlign.TextCenter),
                ) {
                    Image(
                        painterResource(id = R.drawable.sweat_drops),
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "",
                    )
                },
        )
    Text(
        annotatedString,
        inlineContent = inlineContentMap,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Main card based on [ClassicCard]
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun RootCard(
    onClick: () -> Unit,
    title: String,
    imageWidth: Dp,
    imageHeight: Dp,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    imageContent: @Composable BoxScope.() -> Unit = {},
    videoUrl: String? = null,
    onLongClick: (() -> Unit)? = null,
    imageOverlay: @Composable BoxScope.() -> Unit = {},
    subtitle: @Composable () -> Unit = {},
    description: @Composable () -> Unit = {},
    shape: CardShape = CardDefaults.shape(),
    colors: CardColors = CardDefaults.colors(),
    scale: CardScale = CardDefaults.scale(),
    border: CardBorder = CardDefaults.border(),
    glow: CardGlow = CardDefaults.glow(),
    contentPadding: PaddingValues = PaddingValues(),
    interactionSource: MutableInteractionSource? = null,
) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }

    val playVideoPreviews =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("playVideoPreviews", true)

    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier =
            modifier
                .onFocusChanged { focusState ->
                    focused = focusState.isFocused
                }
                .padding(0.dp)
                .width(imageWidth),
        interactionSource = interactionSource,
        shape = shape,
        colors = colors,
        scale = scale,
        border = border,
        glow = glow,
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            // Image/Video
            Box(
                modifier = modifier.size(imageWidth, imageHeight),
                contentAlignment = Alignment.Center,
            ) {
                if (playVideoPreviews && focused && videoUrl.isNotNullOrBlank()) {
                    AndroidView(factory = {
                        PlayerView(context).apply {
                            hideController()
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                            val exoPlayer = StashExoPlayer.getInstance(context)
                            player = exoPlayer

                            val mediaItem =
                                MediaItem.Builder()
                                    .setUri(Uri.parse(videoUrl))
                                    .setMimeType(MimeTypes.VIDEO_MP4)
                                    .build()

                            exoPlayer.setMediaItem(mediaItem, C.TIME_UNSET)
                            if (PreferenceManager.getDefaultSharedPreferences(context)
                                    .getBoolean("videoPreviewAudio", false)
                            ) {
                                exoPlayer.volume = 1f
                            } else {
                                exoPlayer.volume = 0f
                            }
                            exoPlayer.prepare()
                            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                            exoPlayer.playWhenReady = true
                            exoPlayer.seekToDefaultPosition()
                        }
                    })
                } else {
                    Box(
                        modifier =
                            Modifier
                                .width(imageWidth)
                                .height(imageHeight)
                                .padding(0.dp),
                    ) {
                        if (imageUrl.isNotNullOrBlank()) {
                            GlideImage(
                                model = imageUrl,
                                contentDescription = "",
                                contentScale = ContentScale.Crop, // TODO or ContentScale.Fit ?
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        imageContent.invoke(this)
                        if (!focused) {
                            imageOverlay.invoke(this)
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(6.dp)) {
                // Title
                ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                    Text(
                        title,
                        maxLines = 1,
                        modifier =
                            Modifier
                                .enableMarquee(focused),
                    )
                }
                // Subtitle
                ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                    Box(Modifier.graphicsLayer { alpha = 0.6f }) { subtitle.invoke() }
                }
                // Description
                ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                    Box(
                        Modifier.graphicsLayer {
                            alpha = 0.8f
                        },
                    ) { description.invoke() }
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun StashCard(
    item: Any,
    itemOnClick: (item: Any) -> Unit,
) {
    when (item) {
        is SlimSceneData -> SceneCard(item, onClick = { itemOnClick(item) })
        is FullSceneData ->
            SceneCard(
                item.asSlimeSceneData,
                onClick = { itemOnClick(item) },
            )

        is PerformerData -> PerformerCard(item, onClick = { itemOnClick(item) })
        is ImageData -> ImageCard(item, onClick = { itemOnClick(item) })
        is GalleryData -> GalleryCard(item, onClick = { itemOnClick(item) })
        is MarkerData -> MarkerCard(item, onClick = { itemOnClick(item) })
        is MovieData -> MovieCard(item, onClick = { itemOnClick(item) })
        is StudioData -> StudioCard(item, onClick = { itemOnClick(item) })
        is TagData -> TagCard(item, onClick = { itemOnClick(item) })
        else -> throw UnsupportedOperationException("Item with class ${item.javaClass} not supported.")
    }
}
