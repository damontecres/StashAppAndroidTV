package com.github.damontecres.stashapp.ui.components.prefs

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
                .height(80.dp)
                .fillMaxWidth(),
    ) {
        PreferenceTitle(title, color = MaterialTheme.colorScheme.onSurface)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
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
                    steps = (preference.max - preference.min) / preference.interval,
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
                PreferenceSummary(summary, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (summaryBelow) {
            PreferenceSummary(summary, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
