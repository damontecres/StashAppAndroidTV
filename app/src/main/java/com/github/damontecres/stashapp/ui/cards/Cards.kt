package com.github.damontecres.stashapp.ui.cards

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.GroupRelationshipData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.presenters.StashPresenter.Companion.isDefaultUrl
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.LocalPlayerContext
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.CreateNew
import com.github.damontecres.stashapp.util.asSlimeSceneData
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.getRatingAsDecimalString
import kotlinx.coroutines.delay
import java.util.EnumMap

@Composable
fun ImageOverlay(
    ratingsAsStars: Boolean,
    modifier: Modifier = Modifier,
    rating100: Int? = null,
    favorite: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val showRatings =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.pref_key_show_rating), true)

    Box(modifier = modifier.fillMaxSize()) {
        if (showRatings && rating100 != null && rating100 >= 0) {
            val ratingText = getRatingAsDecimalString(rating100, ratingsAsStars)
            val text = context.getString(R.string.stashapp_rating) + ": $ratingText"
            val ratingColors = context.resources.obtainTypedArray(R.array.rating_colors)
            val bgColor = ratingColors.getColor(rating100 / 5, android.graphics.Color.WHITE)
            ratingColors.recycle()

            Text(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .background(
                            color = Color(bgColor),
                        ).padding(4.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                text = text,
            )
        }
        if (favorite) {
            Text(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                color = colorResource(android.R.color.holo_red_light),
                text = stringResource(R.string.fa_heart),
                fontSize = 20.sp,
                fontFamily = FontAwesome,
            )
        }
        content.invoke(this)
    }
}

val iconOrder =
    listOf(
        DataType.SCENE,
        DataType.GROUP,
        DataType.IMAGE,
        DataType.GALLERY,
        DataType.TAG,
        DataType.PERFORMER,
        DataType.MARKER,
        DataType.STUDIO,
    )

@Suppress("ktlint:standard:function-naming")
@Composable
fun IconRowText(
    iconMap: EnumMap<DataType, Int>,
    oCounter: Int?,
    modifier: Modifier = Modifier,
    additionalIcons: (@Composable AnnotatedString.Builder.() -> Unit)? = null,
) {
    val annotatedString =
        buildAnnotatedString {
            iconOrder.forEach {
                val count = iconMap[it]
                if (count != null && count > 0) {
                    withStyle(SpanStyle(fontFamily = FontAwesome)) {
                        append(stringResource(it.iconStringId))
                    }
                    append(" $count")
                    append("  ")
                }
            }
            additionalIcons?.invoke(this)
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
                        painter = painterResource(id = R.drawable.sweat_drops),
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "",
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSecondaryContainer),
                    )
                },
        )
    Text(
        annotatedString,
        inlineContent = inlineContentMap,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        modifier =
            modifier
                .fillMaxWidth(),
    )
}

/**
 * Main card based on [ClassicCard]
 */
@OptIn(UnstableApi::class)
@Composable
fun RootCard(
    item: Any?,
    onClick: () -> Unit,
    title: String,
    uiConfig: ComposeUiConfig,
    imageWidth: Dp,
    imageHeight: Dp,
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    @DrawableRes defaultImageDrawableRes: Int? = null,
    imageContent: @Composable (BoxScope.() -> Unit)? = null,
    videoUrl: String? = null,
    imageOverlay: @Composable AnimatedVisibilityScope.() -> Unit = {},
    subtitle: @Composable (focused: Boolean) -> Unit = {},
    description: @Composable BoxScope.(focused: Boolean) -> Unit = {},
    shape: CardShape = CardDefaults.shape(),
    colors: CardColors = CardDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    scale: CardScale = CardDefaults.scale(),
    border: CardBorder = CardDefaults.border(),
    glow: CardGlow = CardDefaults.glow(),
    contentPadding: PaddingValues = PaddingValues(),
    interactionSource: MutableInteractionSource? = null,
) = RootCard(
    item,
    onClick,
    AnnotatedString(title),
    uiConfig,
    imageWidth,
    imageHeight,
    longClicker,
    getFilterAndPosition,
    modifier,
    imageUrl,
    defaultImageDrawableRes,
    imageContent,
    videoUrl,
    imageOverlay,
    subtitle,
    description,
    shape,
    colors,
    scale,
    border,
    glow,
    contentPadding,
    interactionSource,
)

/**
 * Main card based on [ClassicCard]
 */
@OptIn(UnstableApi::class)
@Composable
fun RootCard(
    item: Any?,
    onClick: () -> Unit,
    title: AnnotatedString,
    uiConfig: ComposeUiConfig,
    imageWidth: Dp,
    imageHeight: Dp,
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    @DrawableRes defaultImageDrawableRes: Int? = null,
    imageContent: @Composable (BoxScope.() -> Unit)? = null,
    videoUrl: String? = null,
    imageOverlay: @Composable AnimatedVisibilityScope.() -> Unit = {},
    subtitle: @Composable (focused: Boolean) -> Unit = {},
    description: @Composable BoxScope.(focused: Boolean) -> Unit = {},
    shape: CardShape = CardDefaults.shape(),
    colors: CardColors = CardDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    scale: CardScale = CardDefaults.scale(),
    border: CardBorder = CardDefaults.border(),
    glow: CardGlow = CardDefaults.glow(),
    contentPadding: PaddingValues = PaddingValues(),
    interactionSource: MutableInteractionSource? = null,
) {
    val context = LocalContext.current
    val videoDelay =
        remember {
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .getInt(
                    context.getString(R.string.pref_key_ui_card_overlay_delay),
                    context.resources.getInteger(R.integer.pref_key_ui_card_overlay_delay_default),
                ).toLong()
        }

    var focused by remember { mutableStateOf(false) }
    var focusedAfterDelay by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val playVideoPreviews =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getBoolean("playVideoPreviews", true)

    if (focused) {
        LaunchedEffect(Unit) {
            delay(videoDelay)
            if (focused) {
                focusedAfterDelay = true
            }
        }
    }

    val height =
        (imageHeight * (1 + (5f - uiConfig.cardSettings.columns) / uiConfig.cardSettings.columns))
            .coerceAtMost(224.dp) // Prevent tall cards from being excessively tall
    val width =
        imageWidth * (1 + (5f - uiConfig.cardSettings.columns) / uiConfig.cardSettings.columns)

    Card(
        onClick = onClick,
        onLongClick = {
            item?.let {
                longClicker.longClick(
                    item,
                    getFilterAndPosition?.invoke(item),
                )
            }
        },
        modifier =
            modifier
                .onFocusChanged { focusState ->
                    focused = focusState.isFocused
                    if (!focusState.isFocused) focusedAfterDelay = false
                }.padding(0.dp)
                .width(width),
        interactionSource = interactionSource,
        shape = shape,
        colors = colors,
        scale = scale,
        border = border,
        glow = glow,
    ) {
        // Image/Video
        Box(
            modifier =
                Modifier
                    .height(height)
//                    .fillMaxHeight(.7f)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (focusedAfterDelay && playVideoPreviews && videoUrl.isNotNullOrBlank()) {
                val player =
                    LocalPlayerContext.current.player(
                        context,
                        LocalGlobalContext.current.server,
                    )
                LaunchedEffect(player) {
                    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                    val videoPreviewAudio =
                        prefs.getBoolean("videoPreviewAudio", false) &&
                            !prefs.getBoolean(
                                context.getString(R.string.pref_key_playback_start_muted),
                                false,
                            )
                    if (!videoPreviewAudio && C.TRACK_TYPE_AUDIO !in player.trackSelectionParameters.disabledTrackTypes) {
                        player.trackSelectionParameters =
                            player.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                                .build()
                    }
                    val mediaItem =
                        MediaItem
                            .Builder()
                            .setUri(Uri.parse(videoUrl))
                            .setMimeType(MimeTypes.VIDEO_MP4)
                            .build()

                    player.setMediaItem(mediaItem, C.TIME_UNSET)
                    player.playWhenReady = true
                    player.prepare()
                }
                LifecycleStartEffect(Unit) {
                    onStopOrDispose {
                        player.stop()
                    }
                }
                val contentScale = ContentScale.Fit
                val presentationState = rememberPresentationState(player)
                val scaledModifier =
                    Modifier.resizeWithContentScale(contentScale, presentationState.videoSizeDp)

                PlayerSurface(
                    player = player,
                    surfaceType = SURFACE_TYPE_TEXTURE_VIEW, // TODO more investigation needed for why this works
                    modifier = scaledModifier,
                )
                if (!focusedAfterDelay || presentationState.coverSurface) {
                    CardImage(
                        imageHeight = height,
                        imageUrl = imageUrl,
                        defaultImageDrawableRes = defaultImageDrawableRes,
                        imageContent =
                            imageContent ?: {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .background(Color.Black),
                                )
                            },
                        modifier = Modifier,
                    )
                }
            } else {
                CardImage(
                    imageHeight = height,
                    imageUrl = imageUrl,
                    defaultImageDrawableRes = defaultImageDrawableRes,
                    imageContent = imageContent,
                    modifier = Modifier,
                )
            }
            this@Card.AnimatedVisibility(
                visible = !focusedAfterDelay,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                imageOverlay.invoke(this)
            }
        }
        Column(modifier = Modifier.padding(6.dp)) {
            // Title
            ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .enableMarquee(focusedAfterDelay),
                )
            }
            // Subtitle
            ProvideTextStyle(
                MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Box(Modifier.graphicsLayer { alpha = 0.6f }) { subtitle.invoke(focusedAfterDelay) }
            }
            // Description
            ProvideTextStyle(
                MaterialTheme.typography.bodySmall
                    .copy(color = MaterialTheme.colorScheme.onSecondaryContainer),
            ) {
                Box(
                    Modifier
                        .graphicsLayer {
                            alpha = 0.8f
                        }.fillMaxWidth(),
                ) { description.invoke(this, focusedAfterDelay) }
            }
        }
    }
}

@Composable
fun CardImage(
    imageHeight: Dp,
    imageUrl: String?,
    @DrawableRes defaultImageDrawableRes: Int?,
    imageContent: @Composable (BoxScope.() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(imageHeight)
//                .fillMaxHeight()
                .fillMaxWidth()
                .padding(0.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isDefaultUrl && defaultImageDrawableRes != null) {
            Image(
                painter = painterResource(id = defaultImageDrawableRes),
                contentDescription = null,
            )
        } else if (imageUrl.isNotNullOrBlank()) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        } else {
            imageContent?.invoke(this)
        }
    }
}

@Composable
fun StashCard(
    uiConfig: ComposeUiConfig,
    item: Any,
    itemOnClick: (item: Any) -> Unit,
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is SlimSceneData ->
            SceneCard(
                uiConfig,
                item,
                onClick = { itemOnClick(item) },
                longClicker,
                getFilterAndPosition,
                modifier,
            )
        is FullSceneData ->
            SceneCard(
                uiConfig,
                item.asSlimeSceneData,
                onClick = { itemOnClick(item) },
                longClicker,
                getFilterAndPosition,
                modifier,
            )

        is PerformerData ->
            PerformerCard(
                uiConfig,
                item,
                onClick = { itemOnClick(item) },
                longClicker,
                getFilterAndPosition,
                modifier,
            )

        is ImageData ->
            ImageCard(
                uiConfig,
                item,
                onClick = { itemOnClick(item) },
                longClicker,
                getFilterAndPosition,
                modifier,
            )

        is GalleryData ->
            GalleryCard(
                uiConfig,
                item,
                onClick = { itemOnClick(item) },
                longClicker,
                getFilterAndPosition,
                modifier,
            )

        is MarkerData ->
            MarkerCard(
                uiConfig,
                item,
                onClick = { itemOnClick(item) },
                longClicker,
                getFilterAndPosition,
                modifier,
            )

        is GroupData ->
            GroupCard(
                uiConfig,
                item,
                onClick = { itemOnClick(item) },
                longClicker,
                getFilterAndPosition,
                modifier,
            )

        is GroupRelationshipData -> {
            GroupCard(
                uiConfig,
                item.group,
                onClick = { itemOnClick(item) },
                longClicker,
                getFilterAndPosition,
                modifier,
                subtitle = item.description,
            )
        }

        is StudioData ->
            StudioCard(
                uiConfig,
                item,
                onClick = { itemOnClick(item) },
                longClicker,
                getFilterAndPosition,
                modifier,
            )

        is TagData ->
            TagCard(
                uiConfig,
                item,
                onClick = { itemOnClick(item) },
                longClicker,
                getFilterAndPosition,
                modifier,
            )
        is FilterArgs -> {
            ViewAllCard(
                filter = item,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                getFilterAndPosition = getFilterAndPosition,
                uiConfig = uiConfig,
                modifier = modifier,
            )
        }

        is CreateNew -> {
            RootCard(
                item = item,
                title = StashAction.CREATE_NEW.actionName,
                subtitle = {
                    Text(text = item.name.replaceFirstChar(Char::titlecase))
                },
                uiConfig = uiConfig,
                imageWidth = dataTypeImageWidth(item.dataType).dp / 2,
                imageHeight = dataTypeImageHeight(item.dataType).dp / 2,
                imageContent = {
                    Image(
                        painter = painterResource(id = R.drawable.baseline_add_box_24),
                        contentDescription = null,
                    )
                },
                onClick = { itemOnClick.invoke(item) },
                longClicker = longClicker,
                getFilterAndPosition = getFilterAndPosition,
                modifier = modifier,
            )
        }

        else -> throw UnsupportedOperationException("Item with class ${item.javaClass} not supported.")
    }
}

@Composable
fun LoadingCard(
    dataType: DataType,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    RootCard(
        item = null,
        title = "Loading...",
        subtitle = {},
        uiConfig = uiConfig,
        imageWidth = dataTypeImageWidth(dataType).dp / 2,
        imageHeight = dataTypeImageHeight(dataType).dp / 2,
        imageContent = {
            CircularProgress(modifier = Modifier.fillMaxSize(.8f))
        },
        onClick = {},
        longClicker = { _, _ -> },
        getFilterAndPosition = null,
        modifier = modifier,
    )
}
