package com.github.damontecres.stashapp.ui

import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
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
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.presenters.ImagePresenter
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.presenters.MoviePresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashImageCardView.Companion.ICON_ORDER
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.util.ageInYears
import com.github.damontecres.stashapp.util.asSlimeSceneData
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.getRatingAsDecimalString
import java.util.EnumMap
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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
                        GlideImage(
                            model = imageUrl,
                            contentDescription = "",
                            modifier = Modifier,
                        )
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
                                .then(
                                    if (focused) {
                                        Modifier.basicMarquee(initialDelayMillis = 250)
                                    } else {
                                        Modifier
                                    },
                                ),
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
fun StashCard(item: Any) {
    val context = LocalContext.current
    // TODO need to navigate instead
    val clicker = StashItemViewClickListener(context)
    when (item) {
        is SlimSceneData -> SceneCard(item, onClick = { clicker.onItemClicked(item) })
        is FullSceneData ->
            SceneCard(
                item.asSlimeSceneData,
                onClick = { clicker.onItemClicked(item) },
            )
        is PerformerData -> PerformerCard(item, onClick = { clicker.onItemClicked(item) })
        is ImageData -> ImageCard(item, onClick = { clicker.onItemClicked(item) })
        is GalleryData -> GalleryCard(item, onClick = { clicker.onItemClicked(item) })
        is MarkerData -> MarkerCard(item, onClick = { clicker.onItemClicked(item) })
        is MovieData -> MovieCard(item, onClick = { clicker.onItemClicked(item) })
        is StudioData -> StudioCard(item, onClick = { clicker.onItemClicked(item) })
        is TagData -> TagCard(item, onClick = { clicker.onItemClicked(item) })
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun SceneCard(
    item: SlimSceneData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.TAG] = item.tags.size
    dataTypeMap[DataType.PERFORMER] = item.performers.size
    dataTypeMap[DataType.MOVIE] = item.movies.size
    dataTypeMap[DataType.MARKER] = item.scene_markers.size
    dataTypeMap[DataType.GALLERY] = item.galleries.size

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(ScenePresenter.CARD_WIDTH.dp / 2),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
        imageWidth = ScenePresenter.CARD_WIDTH.dp / 2,
        imageHeight = ScenePresenter.CARD_HEIGHT.dp / 2,
        imageUrl = item.paths.screenshot,
        videoUrl = item.paths.preview,
        title = item.titleOrFilename ?: "",
        subtitle = { Text(item.date ?: "") },
        description = {
            IconRowText(dataTypeMap, item.o_counter ?: -1)
        },
        imageOverlay = {
            ImageOverlay(item.rating100) {
                val videoFile = item.files.firstOrNull()?.videoFileData
                if (videoFile != null) {
                    val duration = durationToString(videoFile.duration)
                    Text(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp),
                        text = duration,
                    )
                    Text(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp),
                        style = TextStyle(fontWeight = FontWeight.Bold),
                        text = videoFile.resolutionName().toString(),
                    )
                    if (item.resume_time != null) {
                        val percentWatched = item.resume_time / videoFile.duration
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .background(
                                        androidx.compose.ui.graphics.Color.White,
                                    )
                                    .clip(RectangleShape)
                                    .height(4.dp)
                                    .width((ScenePresenter.CARD_WIDTH * percentWatched).dp / 2),
                        )
                    }
                }
            }
        },
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun PerformerCard(
    item: PerformerData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.SCENE] = item.scene_count
    dataTypeMap[DataType.TAG] = item.tags.size
    dataTypeMap[DataType.MOVIE] = item.movie_count
    dataTypeMap[DataType.IMAGE] = item.image_count
    dataTypeMap[DataType.GALLERY] = item.gallery_count

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(PerformerPresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        imageWidth = PerformerPresenter.CARD_WIDTH.dp / 2,
        imageHeight = PerformerPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = item.image_path,
        title = item.name,
        subtitle = {
            if (item.birthdate != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val yearsOldStr = stringResource(R.string.stashapp_years_old)
                Text(text = "${item.ageInYears} $yearsOldStr")
            } else {
                Text("")
            }
        },
        description = {
            IconRowText(dataTypeMap, item.o_counter ?: -1)
        },
        imageOverlay = {
            ImageOverlay(favorite = item.favorite)
        },
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun ImageCard(
    item: ImageData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.TAG] = item.tags.size
    dataTypeMap[DataType.PERFORMER] = item.performers.size
    dataTypeMap[DataType.GALLERY] = item.galleries.size

    val imageUrl =
        if (item.paths.thumbnail.isNotNullOrBlank()) {
            item.paths.thumbnail
        } else if (item.paths.image.isNotNullOrBlank() && !item.isImageClip) {
            item.paths.image
        } else {
            null
        }

    val details = mutableListOf<String?>()
    details.add(item.studio?.studioData?.name)
    details.add(item.date)

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(ImagePresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        imageWidth = ImagePresenter.CARD_WIDTH.dp / 2,
        imageHeight = ImagePresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        videoUrl = item.paths.preview,
        title = item.title ?: "",
        subtitle = {
            Text(concatIfNotBlank(" - ", details))
        },
        description = {
            IconRowText(dataTypeMap, item.o_counter ?: -1)
        },
        imageOverlay = {
            ImageOverlay(rating100 = item.rating100)
        },
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun GalleryCard(
    item: GalleryData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.TAG] = item.tags.size
    dataTypeMap[DataType.PERFORMER] = item.performers.size
    dataTypeMap[DataType.SCENE] = item.scenes.size
    dataTypeMap[DataType.IMAGE] = item.image_count

    val imageUrl = item.cover?.paths?.thumbnail
    val videoUrl = item.cover?.paths?.preview

    val details = mutableListOf<String?>()
    details.add(item.studio?.name)
    details.add(item.date)

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(GalleryPresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        imageWidth = GalleryPresenter.CARD_WIDTH.dp / 2,
        imageHeight = GalleryPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        title = item.title ?: "",
        subtitle = {
            Text(concatIfNotBlank(" - ", details))
        },
        description = {
            IconRowText(dataTypeMap, null)
        },
        imageOverlay = {
            ImageOverlay(rating100 = item.rating100)
        },
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun MarkerCard(
    item: MarkerData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.TAG] = item.tags.size

    val title =
        item.title.ifBlank {
            item.primary_tag.tagData.name
        } + " - ${item.seconds.toInt().toDuration(DurationUnit.SECONDS)}"

    val imageUrl = item.screenshot
    val videoUrl = item.preview

    val details = if (item.title.isNotBlank()) item.primary_tag.tagData.name else ""

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(MarkerPresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        imageWidth = MarkerPresenter.CARD_WIDTH.dp / 2,
        imageHeight = MarkerPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        title = title,
        subtitle = {
            Text(details)
        },
        description = {
            IconRowText(dataTypeMap, null)
        },
        imageOverlay = {},
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun MovieCard(
    item: MovieData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.SCENE] = item.scene_count

    val title = item.name
    val imageUrl = item.front_image_path
    val details = item.date ?: ""

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(MoviePresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        imageWidth = MoviePresenter.CARD_WIDTH.dp / 2,
        imageHeight = MoviePresenter.CARD_HEIGHT.dp / 2,
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
            ImageOverlay(rating100 = item.rating100)
        },
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun StudioCard(
    item: StudioData,
    onClick: (() -> Unit),
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.SCENE] = item.scene_count
    dataTypeMap[DataType.PERFORMER] = item.performer_count
    dataTypeMap[DataType.MOVIE] = item.movie_count
    dataTypeMap[DataType.IMAGE] = item.image_count
    dataTypeMap[DataType.GALLERY] = item.gallery_count

    val title = item.name
    val imageUrl = item.image_path
    val details =
        if (item.parent_studio != null) {
            stringResource(R.string.stashapp_part_of, item.parent_studio.name)
        } else {
            ""
        }

    RootCard(
        modifier =
            Modifier
                .padding(0.dp)
                .width(StudioPresenter.CARD_WIDTH.dp / 2),
        onClick = onClick,
        imageWidth = StudioPresenter.CARD_WIDTH.dp / 2,
        imageHeight = StudioPresenter.CARD_HEIGHT.dp / 2,
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
            ImageOverlay(rating100 = item.rating100)
        },
    )
}

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
