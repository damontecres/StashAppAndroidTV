package com.github.damontecres.stashapp.ui.cards

import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.util.ageInYears
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.EnumMap

private const val TAG = "PerformerCard"

@Composable
fun PerformerCard(
    uiConfig: ComposeUiConfig,
    item: PerformerData,
    onClick: (() -> Unit),
    longClicker: LongClicker<Any>,
    getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
    modifier: Modifier = Modifier,
    ageOnDate: String? = null,
) {
    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
    dataTypeMap[DataType.SCENE] = item.scene_count
    dataTypeMap[DataType.TAG] = item.tags.size
    dataTypeMap[DataType.GROUP] = item.group_count
    dataTypeMap[DataType.IMAGE] = item.image_count
    dataTypeMap[DataType.GALLERY] = item.gallery_count

    val title =
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

    val subtitle =
        if (ageOnDate.isNotNullOrBlank() &&
            item.birthdate.isNotNullOrBlank() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            val ctx = LocalContext.current
            try {
                val ageInScene =
                    Period
                        .between(
                            LocalDate.parse(item.birthdate, DateTimeFormatter.ISO_LOCAL_DATE),
                            LocalDate.parse(ageOnDate, DateTimeFormatter.ISO_LOCAL_DATE),
                        ).years
                ctx.getString(
                    R.string.stashapp_media_info_performer_card_age_context,
                    ageInScene.toString(),
                    ctx.getString(R.string.stashapp_years_old),
                )
            } catch (ex: Exception) {
                Log.w(TAG, "Exception calculating age", ex)
                item.birthdate
            }
        } else if (item.birthdate.isNotNullOrBlank() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            val yearsOldStr = stringResource(R.string.stashapp_years_old)
            "${item.ageInYears} $yearsOldStr"
        } else if (item.birthdate.isNotNullOrBlank()) {
            item.birthdate
        } else {
            ""
        }

    RootCard(
        item = item,
        modifier =
            modifier
                .padding(0.dp),
        onClick = onClick,
        longClicker = longClicker,
        getFilterAndPosition = getFilterAndPosition,
        uiConfig = uiConfig,
        imageWidth = PerformerPresenter.CARD_WIDTH.dp / 2,
        imageHeight = PerformerPresenter.CARD_HEIGHT.dp / 2,
        imageUrl = item.image_path,
        title = title,
        subtitle = { Text(subtitle) },
        description = {
            IconRowText(
                dataTypeMap,
                item.o_counter ?: -1,
                Modifier
                    .enableMarquee(it)
                    .align(Alignment.Center),
            )
        },
        imageOverlay = {
            ImageOverlay(
                uiConfig.ratingAsStars,
                favorite = item.favorite,
                rating100 = item.rating100,
            )
        },
    )
}
