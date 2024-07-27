package com.github.damontecres.stashapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClassicCard
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashImageCardView.Companion.ICON_ORDER
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import java.util.EnumMap

@Suppress("ktlint:standard:function-naming")
@Composable
fun IconRowText(
    iconMap: EnumMap<DataType, Int>,
    oCounter: Int,
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
            if (oCounter > 0) {
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

@Suppress("ktlint:standard:function-naming")
@Composable
fun StashCard(item: Any) {
    val context = LocalContext.current
    val clicker = StashItemViewClickListener(context)
    when (item) {
        is SlimSceneData -> SceneCard(item, onClick = { clicker.onItemClicked(item) })
        is PerformerData -> PerformerCard(item, onClick = { clicker.onItemClicked(item) })
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

    ClassicCard(
        modifier =
            Modifier
                .width(ScenePresenter.CARD_WIDTH.dp / 2)
                .padding(4.dp),
        contentPadding = PaddingValues(4.dp),
        onClick = onClick,
        image = {
            GlideImage(
                model = item.paths.screenshot,
                contentDescription = "",
                modifier =
                    Modifier
                        .padding(0.dp)
                        .width(ScenePresenter.CARD_WIDTH.dp / 2)
                        .height(ScenePresenter.CARD_HEIGHT.dp / 2),
            )
        },
        title = {
            Text(
                item.titleOrFilename ?: "",
                modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.WhileFocused),
            )
        },
        subtitle = { Text(item.date ?: "") },
        description = {
            IconRowText(dataTypeMap, item.o_counter ?: -1)
        },
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun PerformerCard(
    item: PerformerData,
    onClick: (() -> Unit),
) {
//    val presenter = ScenePresenter()
//
//    AndroidView(factory = { context ->
//        val cardView = StashImageCardView(context)
//        cardView.isFocusable = true
//        cardView.isFocusableInTouchMode = true
//        cardView.updateCardBackgroundColor(cardView, false)
//        cardView.onFocusChangeListener = StashOnFocusChangeListener(context)
//        cardView
//    }) { view ->
//        presenter.onBindViewHolder(Presenter.ViewHolder(view), scene)
//    }
    ClassicCard(
        onClick = onClick,
        image = {
            GlideImage(
                model = item.image_path,
                contentDescription = "",
                modifier =
                    Modifier
                        .padding(0.dp)
                        .width(PerformerPresenter.CARD_WIDTH.dp / 2)
                        .height(PerformerPresenter.CARD_HEIGHT.dp / 2),
            )
        },
        title = { Text(item.name) },
    )
}
