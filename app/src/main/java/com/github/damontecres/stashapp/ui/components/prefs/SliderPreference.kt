package com.github.damontecres.stashapp.ui.components.prefs

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.ui.compat.isTvDevice
import com.github.damontecres.stashapp.ui.components.SliderBar
import kotlin.math.roundToInt

@Composable
fun SliderPreference(
    preference: StashSliderPreference,
    title: String,
    summary: String?,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    summaryBelow: Boolean = false,
) {
    val focused = interactionSource.collectIsFocusedAsState().value
    val background =
        if (focused) {
            MaterialTheme.colorScheme.onBackground
        } else {
            Color.Unspecified
        }
    val contentColor =
        if (focused) {
            Color.Unspecified
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
//                .height(80.dp) // not dense
                .height(72.dp) // dense
                .fillMaxWidth()
                .background(background, shape = RoundedCornerShape(8.dp))
                .padding(PaddingValues(horizontal = 12.dp, vertical = 10.dp)), // dense,
    ) {
        PreferenceTitle(title, color = contentColor)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            if (isTvDevice) {
                SliderBar(
                    value = value,
                    min = preference.min,
                    max = preference.max,
                    interval = preference.interval,
                    onChange = onChange,
                    color = MaterialTheme.colorScheme.border,
                    interactionSource = interactionSource,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Slider(
                    value = value.toFloat(),
                    valueRange = preference.min.toFloat()..preference.max.toFloat(),
                    steps = (preference.max - preference.min) / preference.interval - 1,
                    onValueChange = { onChange.invoke(it.roundToInt()) },
                    onValueChangeFinished = { onChange.invoke(value) },
                    colors =
                        SliderDefaults
                            .colors()
                            .copy(
                                activeTrackColor = MaterialTheme.colorScheme.border,
                                inactiveTrackColor = MaterialTheme.colorScheme.border.copy(alpha = .15f),
                            ),
                    interactionSource = interactionSource,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!summaryBelow) {
                PreferenceSummary(summary, color = contentColor)
            }
        }
        if (summaryBelow) {
            PreferenceSummary(summary, color = contentColor)
        }
    }
}
