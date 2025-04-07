package com.github.damontecres.stashapp.ui.components.main

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.components.DotSeparatedRow
import com.github.damontecres.stashapp.ui.components.StarRating
import com.github.damontecres.stashapp.ui.components.TitleValueText
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.listOfNotNullOrBlank
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun MainPagePerformerDetails(
    perf: PerformerData,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        // Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                modifier = Modifier,
                text = perf.name,
                color = Color.LightGray,
                style =
                    MaterialTheme.typography.displayMedium.copy(
                        shadow =
                            Shadow(
                                color = Color.DarkGray,
                                offset = Offset(5f, 2f),
                                blurRadius = 2f,
                            ),
                    ),
                maxLines = 1,
            )
            if (perf.disambiguation.isNotNullOrBlank()) {
                Text(
                    modifier = Modifier,
                    text = "(${perf.disambiguation})",
                    color = Color.DarkGray,
                    style =
                        MaterialTheme.typography.displaySmall,
                    maxLines = 1,
                )
            }
        }

        Column(
            modifier = Modifier.alpha(0.75f),
        ) {
            // Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.fa_heart),
                    color = if (perf.favorite) Color.Red else Color.LightGray,
                    fontFamily = FontAwesome,
                )
                StarRating(
                    rating100 = perf.rating100 ?: 0,
                    precision = uiConfig.starPrecision,
                    onRatingChange = {},
                    enabled = false,
                    modifier =
                        Modifier
                            .height(24.dp),
                )
            }
            val ageString =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && perf.birthdate.isNotNullOrBlank()) {
                    val date =
                        perf.death_date?.let {
                            LocalDate.parse(
                                perf.death_date,
                                DateTimeFormatter.ISO_LOCAL_DATE,
                            )
                        } ?: LocalDate.now()
                    val age =
                        Period
                            .between(
                                LocalDate.parse(perf.birthdate, DateTimeFormatter.ISO_LOCAL_DATE),
                                date,
                            ).years
                    age.toString() + " " + stringResource(R.string.stashapp_years_old)
                } else {
                    null
                }
            val heightStr =
                perf.height_cm?.let {
                    val feet = floor(perf.height_cm / 30.48).toInt()
                    val inches = (perf.height_cm / 2.54 - feet * 12).roundToInt()
                    "${perf.height_cm} cm ($feet'$inches\")"
                }
            val weightStr =
                perf.weight?.let {
                    val pounds = (perf.weight * 2.2).roundToInt()
                    "${perf.weight} kg ($pounds lbs)"
                }
            DotSeparatedRow(
                modifier = Modifier.padding(top = 4.dp),
                textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                texts =
                    listOfNotNullOrBlank(
                        ageString,
                        heightStr,
                        weightStr,
                    ),
            )
            // Description
            if (perf.details.isNotNullOrBlank()) {
                Text(
                    text = perf.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            // Key-Values
            Row(
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (perf.country.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_country),
                        perf.country,
                    )
                }
                if (perf.hair_color.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_hair_color),
                        perf.hair_color,
                        modifier = Modifier.widthIn(max = 64.dp),
                    )
                }
                if (perf.eye_color.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_eye_color),
                        perf.eye_color,
                        modifier = Modifier.widthIn(max = 64.dp),
                    )
                }
                if (perf.career_length.isNotNullOrBlank()) {
                    TitleValueText(
                        stringResource(R.string.stashapp_career_length),
                        perf.career_length,
                    )
                }
                if (perf.penis_length != null) {
                    val inches = round(perf.penis_length / 2.54 * 100) / 100
                    TitleValueText(
                        stringResource(R.string.stashapp_penis_length),
                        "${perf.penis_length} cm ($inches\")",
                    )
                }
            }
        }
    }
}
