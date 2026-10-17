package com.github.damontecres.stashapp.ui.cards

import android.content.res.Resources
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.actions.StashAction
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
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.CreateNew
import com.github.damontecres.stashapp.util.ageInYears
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.letIfNotBlank
import com.github.damontecres.stashapp.util.name
import com.github.damontecres.stashapp.util.resolutionName
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.util.yearsBetween
import com.github.damontecres.stashapp.views.durationToString
import timber.log.Timber
import java.util.EnumMap
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun cardTitle(
    res: Resources,
    item: Any?,
    cardContext: CardContext? = null,
): AnnotatedString =
    when (item) {
        is SlimSceneData -> {
            cardContext as? CardContext.SceneCardContext?
            AnnotatedString(item.titleOrFilename ?: "")
        }

        is PerformerData -> {
            buildAnnotatedString {
                append(item.name)
                if (item.disambiguation.isNotNullOrBlank()) {
                    withStyle(SpanStyle(fontSize = .75f.em, color = Color.LightGray)) {
                        append(" (")
                        append(item.disambiguation)
                        append(")")
                    }
                }
            }
        }

        is ImageData -> {
            AnnotatedString(item.titleOrFilename ?: "")
        }

        is GalleryData -> {
            AnnotatedString(item.name ?: "")
        }

        is MarkerData -> {
            val title =
                item.title.ifBlank {
                    item.primary_tag.slimTagData.name
                }
            AnnotatedString(title)
        }

        is GroupData -> {
            cardContext as? CardContext.GroupCardContext?
            AnnotatedString(item.name)
        }

        is GroupRelationshipData -> {
            cardContext as? CardContext.GroupCardContext?
            AnnotatedString(item.group.name)
        }

        is StudioData -> {
            AnnotatedString(item.name)
        }

        is TagData -> {
            AnnotatedString(item.name)
        }

        is FilterArgs -> {
            AnnotatedString(res.getString(R.string.stashapp_view_all))
        }

        is CreateNew -> {
            AnnotatedString(StashAction.CREATE_NEW.actionName)
        }

        else -> {
            AnnotatedString("")
        }
    }

@Composable
fun CardSubtitle(
    item: Any?,
    focused: Boolean,
    modifier: Modifier = Modifier,
    cardContext: CardContext? = null,
) {
    when (item) {
        is SlimSceneData -> {
            val cc = cardContext as? CardContext.SceneCardContext?
            Column(modifier = modifier) {
                Text(item.date ?: "")
                cardContext?.let {
                    val index = remember(item, cardContext) { item.groups.firstOrNull { cc?.sceneInGroupId == it.group.id }?.scene_index }
                    if (index != null) {
                        Text(
                            text = stringResource(R.string.stashapp_scene) + " #$index",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        is PerformerData -> {
            val res = LocalResources.current
            val cc = cardContext as? CardContext.PerformerCardContext?
            val subtitle =
                remember(item, cardContext, res) {
                    if (cc?.ageOnDate.isNotNullOrBlank() &&
                        item.birthdate.isNotNullOrBlank() &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ) {
                        try {
                            yearsBetween(item.birthdate, cc.ageOnDate)?.let {
                                res.getString(
                                    R.string.stashapp_media_info_performer_card_age_context,
                                    it.toString(),
                                    res.getString(R.string.stashapp_years_old),
                                )
                            }
                        } catch (ex: Exception) {
                            Timber.w(ex, "Exception calculating age")
                            item.birthdate
                        }
                    } else if (item.birthdate.isNotNullOrBlank() &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ) {
                        val yearsOldStr = res.getString(R.string.stashapp_years_old)
                        "${item.ageInYears} $yearsOldStr"
                    } else if (item.birthdate.isNotNullOrBlank()) {
                        item.birthdate
                    } else {
                        ""
                    }
                }
            Text(subtitle ?: "", modifier = modifier)
        }

        is ImageData -> {
            val subtitle =
                remember(item) {
                    buildList {
                        item.studio?.name?.letIfNotBlank(::add)
                        item.date?.letIfNotBlank(::add)
                    }.joinToString(" - ")
                }
            Text(subtitle, modifier = modifier)
        }

        is GalleryData -> {
            val subtitle =
                remember(item) {
                    buildList {
                        item.studio?.name?.letIfNotBlank(::add)
                        item.date?.letIfNotBlank(::add)
                    }.joinToString(" - ")
                }
            Text(subtitle, modifier = modifier)
        }

        is MarkerData -> {
            val subtitle =
                remember(item) {
                    val startTime =
                        item.seconds
                            .toInt()
                            .toDuration(DurationUnit.SECONDS)
                            .toString()
                    if (item.end_seconds != null) {
                        "$startTime - ${item.end_seconds.toInt().toDuration(DurationUnit.SECONDS)}"
                    } else {
                        startTime
                    }
                }
            Column(modifier = modifier) {
                Text(
                    text = subtitle,
                    maxLines = 1,
                    modifier = Modifier.enableMarquee(focused),
                )
                Text(
                    text = remember(item) { item.scene.minimalSceneData.titleOrFilename ?: "" },
                    maxLines = 1,
                    modifier = Modifier.enableMarquee(focused),
                )
            }
        }

        is GroupData -> {
            val cc = cardContext as? CardContext.GroupCardContext?
            Column(modifier = modifier) {
                Text(item.date ?: "")
                cc?.indexInGroup?.let { index ->
                    Text(
                        text = "#$index in group",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        is GroupRelationshipData -> {
            val cc = cardContext as? CardContext.GroupCardContext?
            Column(modifier = modifier) {
                Text(item.description ?: "")
                cc?.indexInGroup?.let { index ->
                    Text(
                        text = "#$index in group",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        is StudioData -> {
            val details =
                if (item.parent_studio != null) {
                    stringResource(R.string.stashapp_part_of, item.parent_studio.name)
                } else {
                    ""
                }
            Text(details, modifier = modifier)
        }

        is TagData -> {
            Text(
                text = item.description ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier,
            )
        }

        is FilterArgs -> {
            // no-op
        }

        is CreateNew -> {
            Text(text = item.name.replaceFirstChar(Char::titlecase), modifier = modifier)
        }
    }
}

fun imageSize(
    item: Any?,
    cardContext: CardContext? = null,
): DpSize =
    when (item) {
        is SlimSceneData -> {
            cardContext as? CardContext.SceneCardContext?
            DpSize(dataTypeImageWidth(DataType.SCENE).dp / 2, dataTypeImageHeight(DataType.SCENE).dp / 2)
        }

        is PerformerData -> {
            DpSize(dataTypeImageWidth(DataType.PERFORMER).dp / 2, dataTypeImageHeight(DataType.PERFORMER).dp / 2)
        }

        is ImageData -> {
            DpSize(dataTypeImageWidth(DataType.IMAGE).dp / 2, dataTypeImageHeight(DataType.IMAGE).dp / 2)
        }

        is GalleryData -> {
            DpSize(dataTypeImageWidth(DataType.GALLERY).dp / 2, dataTypeImageHeight(DataType.GALLERY).dp / 2)
        }

        is MarkerData -> {
            DpSize(dataTypeImageWidth(DataType.MARKER).dp / 2, dataTypeImageHeight(DataType.MARKER).dp / 2)
        }

        is GroupData -> {
            cardContext as? CardContext.GroupCardContext?
            DpSize(dataTypeImageWidth(DataType.GROUP).dp / 2, dataTypeImageHeight(DataType.GROUP).dp / 2)
        }

        is GroupRelationshipData -> {
            cardContext as? CardContext.GroupCardContext?
            DpSize(dataTypeImageWidth(DataType.GROUP).dp / 2, dataTypeImageHeight(DataType.GROUP).dp / 2)
        }

        is StudioData -> {
            DpSize(dataTypeImageWidth(DataType.STUDIO).dp / 2, dataTypeImageHeight(DataType.STUDIO).dp / 2)
        }

        is TagData -> {
            DpSize(dataTypeImageWidth(DataType.TAG).dp / 2, dataTypeImageHeight(DataType.TAG).dp / 2)
        }

        is FilterArgs -> {
            DpSize(dataTypeImageWidth(item.dataType).dp / 2, dataTypeImageHeight(item.dataType).dp / 2)
        }

        is CreateNew -> {
            DpSize(dataTypeImageWidth(item.dataType).dp / 2, dataTypeImageHeight(item.dataType).dp / 2)
        }

        else -> {
            DpSize(dataTypeImageWidth(DataType.SCENE).dp / 2, dataTypeImageHeight(DataType.SCENE).dp / 2)
        }
    }

fun imageUrl(item: Any?): String? =
    when (item) {
        is SlimSceneData -> {
            item.paths.screenshot
        }

        is PerformerData -> {
            item.image_path
        }

        is ImageData -> {
            if (item.paths.thumbnail.isNotNullOrBlank()) {
                item.paths.thumbnail
            } else if (item.paths.image.isNotNullOrBlank() && !item.isImageClip) {
                item.paths.image
            } else {
                null
            }
        }

        is GalleryData -> {
            item.paths.cover
        }

        is MarkerData -> {
            item.screenshot
        }

        is GroupData -> {
            item.front_image_path
        }

        is GroupRelationshipData -> {
            item.group.front_image_path
        }

        is StudioData -> {
            item.image_path
        }

        is TagData -> {
            item.image_path
        }

        is FilterArgs -> {
            null
        }

        is CreateNew -> {
            null
        }

        else -> {
            null
        }
    }

@Composable
fun BoxScope.CardDescription(
    item: Any?,
    uiConfig: ComposeUiConfig,
    focused: Boolean,
    modifier: Modifier = Modifier,
    cardContext: CardContext? = null,
) {
    when (item) {
        is SlimSceneData -> {
            cardContext as? CardContext.SceneCardContext?
            val dataTypeMap =
                remember(item) {
                    EnumMap<DataType, Int>(DataType::class.java).apply {
                        this[DataType.TAG] = item.tags.size
                        this[DataType.PERFORMER] = item.performers.size
                        this[DataType.GROUP] = item.groups.size
                        this[DataType.MARKER] = item.scene_markers.size
                        this[DataType.GALLERY] = item.galleries.size
                    }
                }
            IconRowText(
                sfwMode = uiConfig.sfwMode,
                dataTypeMap,
                item.o_counter ?: -1,
                Modifier
                    .enableMarquee(focused)
                    .align(Alignment.Center),
            )
        }

        is PerformerData -> {
            val dataTypeMap =
                remember(item) {
                    EnumMap<DataType, Int>(DataType::class.java).apply {
                        this[DataType.SCENE] = item.scene_count
                        this[DataType.TAG] = item.tags.size
                        this[DataType.GROUP] = item.group_count
                        this[DataType.IMAGE] = item.image_count
                        this[DataType.GALLERY] = item.gallery_count
                    }
                }

            IconRowText(
                sfwMode = uiConfig.sfwMode,
                dataTypeMap,
                item.o_counter ?: -1,
                Modifier
                    .enableMarquee(focused)
                    .align(Alignment.Center),
            )
        }

        is ImageData -> {
            val dataTypeMap =
                remember(item) {
                    EnumMap<DataType, Int>(DataType::class.java).apply {
                        this[DataType.TAG] = item.tags.size
                        this[DataType.PERFORMER] = item.performers.size
                        this[DataType.GALLERY] = item.galleries.size
                    }
                }
            IconRowText(
                sfwMode = uiConfig.sfwMode,
                dataTypeMap,
                item.o_counter ?: -1,
                Modifier
                    .enableMarquee(focused)
                    .align(Alignment.Center),
            )
        }

        is GalleryData -> {
            val dataTypeMap =
                remember(item) {
                    EnumMap<DataType, Int>(DataType::class.java).apply {
                        this[DataType.TAG] = item.tags.size
                        this[DataType.PERFORMER] = item.performers.size
                        this[DataType.SCENE] = item.scenes.size
                        this[DataType.IMAGE] = item.image_count
                    }
                }
            IconRowText(
                sfwMode = uiConfig.sfwMode,
                dataTypeMap,
                null,
                Modifier
                    .enableMarquee(focused)
                    .align(Alignment.Center),
            )
        }

        is MarkerData -> {
            val dataTypeMap =
                remember(item) {
                    EnumMap<DataType, Int>(DataType::class.java).apply {
                        this[DataType.TAG] = item.tags.size
                    }
                }
            IconRowText(
                sfwMode = uiConfig.sfwMode,
                dataTypeMap,
                null,
                Modifier
                    .enableMarquee(focused)
                    .align(Alignment.Center),
            )
        }

        is GroupData -> {
            cardContext as? CardContext.GroupCardContext?
            val dataTypeMap =
                remember(item) {
                    EnumMap<DataType, Int>(DataType::class.java).apply {
                        this[DataType.SCENE] = item.scene_count
                        this[DataType.PERFORMER] = item.performer_count
                        this[DataType.TAG] = item.tags.size
                    }
                }
            IconRowText(
                sfwMode = uiConfig.sfwMode,
                dataTypeMap,
                item.o_counter,
                Modifier
                    .enableMarquee(focused)
                    .align(Alignment.Center),
            ) {
                if (item.containing_groups.isNotEmpty() || item.sub_group_count > 0) {
                    if (length > 0) {
                        append(" ")
                    }
                    withStyle(SpanStyle(fontFamily = FontAwesome)) {
                        append(stringResource(DataType.GROUP.iconStringId))
                    }
                    append(" ")
                    if (item.containing_groups.isNotEmpty()) {
                        append(item.containing_groups.size.toString())
                        withStyle(SpanStyle(fontFamily = FontAwesome)) {
                            append(stringResource(R.string.fa_arrow_up_long))
                        }
                    }
                    if (item.sub_group_count > 0) {
                        append(item.sub_group_count.toString())
                        withStyle(SpanStyle(fontFamily = FontAwesome)) {
                            append(stringResource(R.string.fa_arrow_down_long))
                        }
                    }
                    if (item.o_counter != null && item.o_counter > 0) {
                        append(" ")
                    }
                }
            }
        }

        is GroupRelationshipData -> {
            cardContext as? CardContext.GroupCardContext?
            CardDescription(item.group, uiConfig, focused, modifier)
        }

        is StudioData -> {
            val dataTypeMap =
                remember(item) {
                    EnumMap<DataType, Int>(DataType::class.java).apply {
                        this[DataType.SCENE] = item.scene_count
                        this[DataType.PERFORMER] = item.performer_count
                        this[DataType.GROUP] = item.group_count
                        this[DataType.IMAGE] = item.image_count
                        this[DataType.GALLERY] = item.gallery_count
                        this[DataType.TAG] = item.tags.size
                    }
                }
            IconRowText(
                sfwMode = uiConfig.sfwMode,
                dataTypeMap,
                item.o_counter,
                Modifier
                    .enableMarquee(focused)
                    .align(Alignment.Center),
            )
        }

        is TagData -> {
            val dataTypeMap =
                remember(item) {
                    EnumMap<DataType, Int>(DataType::class.java).apply {
                        this[DataType.SCENE] = item.scene_count
                        this[DataType.PERFORMER] = item.performer_count
                        this[DataType.MARKER] = item.scene_marker_count
                        this[DataType.IMAGE] = item.image_count
                        this[DataType.GALLERY] = item.gallery_count
                    }
                }
            IconRowText(
                sfwMode = uiConfig.sfwMode,
                dataTypeMap,
                null,
                Modifier
                    .enableMarquee(focused)
                    .align(Alignment.Center),
            )
        }

        is FilterArgs -> {
            // no-op
        }

        is CreateNew -> {
            // no-op
        }
    }
}

@Composable
fun CardImageOverlay(
    item: Any?,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
    cardContext: CardContext? = null,
) {
    when (item) {
        is SlimSceneData -> {
            cardContext as? CardContext.SceneCardContext?
            ImageOverlay(uiConfig.ratingAsStars, rating100 = item.rating100, modifier = modifier) {
                val videoFile = item.files.firstOrNull()?.videoFile
                if (videoFile != null) {
                    val duration = remember(videoFile) { durationToString(videoFile.duration) }
                    Text(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp),
                        text = duration,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        text = videoFile.resolutionName().toString(),
                    )
                    if (item.resume_time != null && uiConfig.showCardProgress) {
                        val percentWatched = item.resume_time / videoFile.duration
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .background(
                                        MaterialTheme.colorScheme.tertiary,
                                    ).clip(RectangleShape)
                                    .height(4.dp)
                                    .width((dataTypeImageWidth(DataType.SCENE) * percentWatched).dp / 2),
                        )
                    }
                }
                if (item.studio != null) {
                    val imageUrl =
                        remember(item) {
                            item.studio.image_path.takeIf { it.isNotNullOrBlank() && !it.contains("default=true") }
                        }
                    if (!uiConfig.showStudioAsText && imageUrl != null) {
                        AsyncImage(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .fillMaxWidth(.4f),
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            text = item.studio.name,
                        )
                    }
                }
            }
        }

        is PerformerData -> {
            ImageOverlay(
                uiConfig.ratingAsStars,
                favorite = item.favorite,
                rating100 = item.rating100,
            )
        }

        is ImageData -> {
            ImageOverlay(
                uiConfig.ratingAsStars,
                rating100 = item.rating100,
            )
        }

        is GalleryData -> {
            ImageOverlay(
                uiConfig.ratingAsStars,
                rating100 = item.rating100,
            )
        }

        is MarkerData -> {
        }

        is GroupData -> {
            cardContext as? CardContext.GroupCardContext
            ImageOverlay(uiConfig.ratingAsStars, rating100 = item.rating100)
        }

        is GroupRelationshipData -> {
            cardContext as? CardContext.GroupCardContext
            ImageOverlay(uiConfig.ratingAsStars, rating100 = item.group.rating100)
        }

        is StudioData -> {
            ImageOverlay(
                uiConfig.ratingAsStars,
                favorite = item.favorite,
                rating100 = item.rating100,
            )
        }

        is TagData -> {
            ImageOverlay(uiConfig.ratingAsStars, favorite = item.favorite) {
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
        }

        is FilterArgs -> {
            // no-op
        }

        is CreateNew -> {
        }
    }
}
