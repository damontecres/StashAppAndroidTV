package com.github.damontecres.stashapp.ui.cards

import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardGlow
import androidx.tv.material3.CardScale
import androidx.tv.material3.CardShape
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.playback.maybeMuteAudio
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.LocalPlayerContext
import com.github.damontecres.stashapp.ui.compat.Card
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.ui.isDefaultUrl
import com.github.damontecres.stashapp.ui.util.playOnClickSound
import com.github.damontecres.stashapp.util.CreateNew
import com.github.damontecres.stashapp.util.asSlimeSceneData
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.getRatingAsDecimalString
import kotlinx.coroutines.delay
import java.util.EnumMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun ImageOverlay(
    ratingsAsStars: Boolean,
    modifier: Modifier = Modifier,
    rating100: Int? = null,
    favorite: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val showRatings = LocalGlobalContext.current.preferences.interfacePreferences.showRatingOnCards

    Box(modifier = modifier.fillMaxSize()) {
        if (showRatings && rating100 != null && rating100 > 0) {
            val ratingText = getRatingAsDecimalString(rating100, ratingsAsStars)
            val text = context.getString(R.string.stashapp_rating) + ": $ratingText"
            val ratingColors = resources.obtainTypedArray(R.array.rating_colors)
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
    sfwMode: Boolean,
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
                if (sfwMode) {
                    withStyle(SpanStyle(fontFamily = FontAwesome)) {
                        append(stringResource(R.string.fa_thumbs_up))
                    }
                } else {
                    appendInlineContent(id = "ocounter", "O")
                }
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
 * Main card based on [Card]
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
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    extraImageUrls: List<String> = listOf(),
) {
    val context = LocalContext.current
    val videoDelay =
        remember(uiConfig.preferences.interfacePreferences.cardPreviewDelayMs) {
            uiConfig.preferences.interfacePreferences.cardPreviewDelayMs.milliseconds
        }

    val focused = interactionSource.collectIsFocusedAsState().value
    var focusedAfterDelay by remember { mutableStateOf(false) }

    val playVideoPreviews = uiConfig.preferences.interfacePreferences.playVideoPreviews

    if (focused) {
        LaunchedEffect(Unit) {
            if (uiConfig.playSoundOnFocus) playOnClickSound(context)
            delay(videoDelay)
            focusedAfterDelay = true
        }
        if (extraImageUrls.isNotEmpty()) {
            val disposable =
                context.imageLoader.enqueue(
                    ImageRequest.Builder(context).data(extraImageUrls.first()).build(),
                )
            DisposableEffect(Unit) {
                onDispose { disposable.dispose() }
            }
        }
    } else {
        focusedAfterDelay = false
    }

    val height =
        (imageHeight * (1 + (5f - uiConfig.cardSettings.columns) / uiConfig.cardSettings.columns))
            .coerceAtMost(224.dp) // Prevent tall cards from being excessively tall
    val width =
        imageWidth * (1 + (5f - uiConfig.cardSettings.columns) / uiConfig.cardSettings.columns)

    Card(
        onClick = {
            if (uiConfig.playSoundOnFocus) playOnClickSound(context)
            onClick.invoke()
        },
        onLongClick = {
            if (uiConfig.playSoundOnFocus) playOnClickSound(context)
            item?.let {
                longClicker.longClick(
                    item,
                    getFilterAndPosition?.invoke(item),
                )
            }
        },
        modifier =
            modifier
                .padding(0.dp)
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
                val player = LocalPlayerContext.current.player
                LaunchedEffect(player) {
                    player?.let {
                        maybeMuteAudio(uiConfig.preferences, true, player)
                        val mediaItem =
                            MediaItem
                                .Builder()
                                .setUri(videoUrl.toUri())
                                .setMimeType(MimeTypes.VIDEO_MP4)
                                .build()

                        player.setMediaItem(mediaItem, C.TIME_UNSET)
                        player.playWhenReady = true
                        player.prepare()
                    }
                }
                LifecycleStartEffect(Unit) {
                    onStopOrDispose {
                        player?.stop()
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
                        modifier = Modifier.padding(contentPadding),
                    )
                }
            } else {
                var extraImageUrl by remember(imageUrl) { mutableStateOf(imageUrl) }
                if (playVideoPreviews && focusedAfterDelay && extraImageUrls.isNotEmpty()) {
                    var enqueued by remember { mutableStateOf<Disposable?>(null) }
                    LaunchedEffect(Unit) {
                        var idx = 0
                        while (true) {
                            extraImageUrl = extraImageUrls[idx]
                            if (++idx >= extraImageUrls.size) idx = 0
                            // Prefetch next image
                            enqueued =
                                context.imageLoader.enqueue(
                                    ImageRequest.Builder(context).data(extraImageUrls[idx]).build(),
                                )
                            delay(2.seconds)
                        }
                    }
                    DisposableEffect(Unit) {
                        onDispose {
                            extraImageUrl = imageUrl
                            enqueued?.dispose()
                        }
                    }
                }
                CardImage(
                    imageHeight = height,
                    imageUrl = extraImageUrl,
                    defaultImageDrawableRes = defaultImageDrawableRes,
                    imageContent = imageContent,
                    crossFade = false,
                    modifier = Modifier.padding(contentPadding),
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
    crossFade: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .height(imageHeight)
//                .fillMaxHeight()
                .fillMaxWidth(),
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
                        .crossfade(crossFade)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        } else {
            imageContent?.invoke(this)
        }
    }
}

/**
 * Context about where this card is being shown to alter its display
 */
interface CardContext {
    data object None : CardContext

    data class SceneCardContext(
        // If listing scenes of a group, this is the group id
        val sceneInGroupId: String,
    ) : CardContext

    data class GroupCardContext(
        // If listing groups that a scene is in, this is the scene's index
        val indexInGroup: Int?,
    ) : CardContext

    data class PerformerCardContext(
        val ageOnDate: String?,
    ) : CardContext
}

@Composable
fun StashCard(
    uiConfig: ComposeUiConfig,
    item: Any?,
    itemOnClick: (item: Any) -> Unit,
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
    modifier: Modifier = Modifier,
    cardContext: CardContext = CardContext.None,
) {
    val res = LocalResources.current

    val item =
        remember(item) {
            when (item) {
                is FullSceneData -> item.asSlimeSceneData
                else -> item
            }
        }

    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val imageSize = remember(item, cardContext) { imageSize(item, cardContext) }

    RootCard(
        item = item,
        title = remember(res, item, cardContext) { cardTitle(res, item, cardContext) },
        subtitle = {
            CardSubtitle(
                item = item,
                focused = focused,
                modifier = Modifier,
                cardContext = cardContext,
            )
        },
        description = { focused ->
            CardDescription(item, uiConfig, focused, Modifier, cardContext)
        },
        uiConfig = uiConfig,
        imageWidth = imageSize.width,
        imageHeight = imageSize.height,
        imageUrl = remember(item) { imageUrl(item) },
        videoUrl =
            when (item) {
                is SlimSceneData -> item.paths.preview
                is FullSceneData -> item.paths.preview
                is ImageData -> item.paths.preview
                else -> null
            },
        imageContent =
            when (item) {
                is FilterArgs -> {
                    {
                        Image(
                            imageVector = ImageVector.vectorResource(id = R.drawable.baseline_camera_indoor_48),
                            contentDescription = "",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                is CreateNew -> {
                    {
                        Image(
                            painter = painterResource(id = R.drawable.baseline_add_box_24),
                            contentDescription = null,
                        )
                    }
                }

                else -> {
                    null
                }
            },
        imageOverlay = {
            CardImageOverlay(item, uiConfig, Modifier, cardContext)
        },
        onClick = { if (item != null) itemOnClick.invoke(item) },
        longClicker = longClicker,
        getFilterAndPosition = getFilterAndPosition,
        interactionSource = interactionSource,
        contentPadding = if (item is StudioData) PaddingValues(8.dp) else PaddingValues(),
        modifier = modifier,
    )
}
