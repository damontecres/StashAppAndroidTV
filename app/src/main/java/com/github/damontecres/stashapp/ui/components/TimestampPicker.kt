package com.github.damontecres.stashapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.compat.isNotTvDevice
import com.github.damontecres.stashapp.ui.compat.isTvDevice
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.views.DurationPicker2
import com.sd.lib.compose.wheel_picker.FVerticalWheelPicker
import com.sd.lib.compose.wheel_picker.FWheelPickerState
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class TimestampPickerState private constructor(
    val hours: FWheelPickerState,
    val minutes: FWheelPickerState,
    val seconds: FWheelPickerState,
    val milliseconds: FWheelPickerState,
) {
    val duration: Duration
        get() =
            hours.currentIndex.coerceAtLeast(0).hours +
                minutes.currentIndex.coerceAtLeast(0).minutes +
                seconds.currentIndex.coerceAtLeast(0).seconds +
                (milliseconds.currentIndex.coerceAtLeast(0) * 50).milliseconds

    companion object {
        @Composable
        fun rememberTimestampPickerState(timestamp: Duration): TimestampPickerState =
            TimestampPickerState(
                rememberFWheelPickerState(
                    DurationPicker2.getHours(timestamp.inWholeMilliseconds).coerceAtMost(25),
                ),
                rememberFWheelPickerState(
                    DurationPicker2.getMinutes(timestamp.inWholeMilliseconds).coerceAtMost(60),
                ),
                rememberFWheelPickerState(
                    DurationPicker2.getSeconds(timestamp.inWholeMilliseconds).coerceAtMost(60),
                ),
                rememberFWheelPickerState(
                    DurationPicker2
                        .getMilliseconds(timestamp.inWholeMilliseconds)
                        .div(50)
                        .coerceAtMost(20),
                ),
            )
    }
}

@Composable
fun TimestampPicker(
    timestamp: Duration,
    maxDuration: Duration,
    onValueChange: (Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hourState =
        rememberFWheelPickerState(
            DurationPicker2.getHours(timestamp.inWholeMilliseconds).coerceAtMost(25),
        )
    val minuteState =
        rememberFWheelPickerState(
            DurationPicker2.getMinutes(timestamp.inWholeMilliseconds).coerceAtMost(60),
        )
    val secondState =
        rememberFWheelPickerState(
            DurationPicker2.getSeconds(timestamp.inWholeMilliseconds).coerceAtMost(60),
        )
    val milliSecondState =
        rememberFWheelPickerState(
            DurationPicker2.getMilliseconds(timestamp.inWholeMilliseconds).div(50).coerceAtMost(20),
        )

    LaunchedEffect(hourState, minuteState, secondState, milliSecondState) {
        combine(
            snapshotFlow { hourState.currentIndex },
            snapshotFlow { minuteState.currentIndex },
            snapshotFlow { secondState.currentIndex },
            snapshotFlow { milliSecondState.currentIndex },
        ) { ints ->
            ints[0].coerceAtLeast(0).hours +
                ints[1].coerceAtLeast(0).minutes +
                ints[2].coerceAtLeast(0).seconds +
                (ints[3].coerceAtLeast(0) * 50).milliseconds
        }.collect {
            onValueChange.invoke(it)
        }
    }

    var focused by remember { mutableStateOf(false) }
    var activated by remember { mutableStateOf(false) }
    val activateOnClick = {
        if (!activated) {
            hourState.currentIndex.coerceAtLeast(0).hours +
                minuteState.currentIndex.coerceAtLeast(0).minutes +
                hourState.currentIndex.coerceAtLeast(0).seconds +
                (milliSecondState.currentIndex.coerceAtLeast(0) * 50).milliseconds
        }
        activated = !activated
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .background(
                    if (focused) {
                        MaterialTheme.colorScheme.border.copy(alpha = .33f)
                    } else {
                        Color.Unspecified
                    },
                ).onFocusChanged { focused = it.hasFocus },
    ) {
        if (maxDuration.inWholeHours > 0) {
            WheelPicker(
                state = hourState,
                activated = activated,
                count = maxDuration.inWholeHours.toInt() + 1,
                indexTransformer = { stringResource(R.string.value_hours, it.toString()) },
                onClick = { activated = !activated },
                modifier = Modifier.weight(1f),
            )
        }

        if (maxDuration.inWholeMinutes > 0) {
            WheelPicker(
                state = minuteState,
                activated = activated,
                count = maxDuration.inWholeMinutes.toInt().coerceIn(2, 60),
                indexTransformer = { stringResource(R.string.value_minutes, it.toString()) },
                onClick = { activated = !activated },
                modifier = Modifier.weight(1f),
            )
        }

        if (maxDuration.inWholeSeconds > 0) {
            WheelPicker(
                state = secondState,
                activated = activated,
                count = maxDuration.inWholeSeconds.toInt().coerceIn(2, 60),
                indexTransformer = { stringResource(R.string.value_seconds, it.toString()) },
                onClick = { activated = !activated },
                modifier = Modifier.weight(1f),
            )
        }

        WheelPicker(
            state = milliSecondState,
            activated = activated,
            count = maxDuration.inWholeMilliseconds.toInt().coerceIn(2, 21),
            indexTransformer = {
                stringResource(
                    R.string.value_milliseconds,
                    (it * 50).toString(),
                )
            },
            onClick = { activated = !activated },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun WheelPicker(
    state: FWheelPickerState,
    activated: Boolean,
    count: Int,
    indexTransformer: @Composable (Int) -> String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    FVerticalWheelPicker(
        modifier =
            modifier
                .ifElse(
                    isTvDevice,
                    Modifier
                        .onFocusChanged { focused = it.isFocused }
                        .focusable()
                        .onKeyEvent {
                            var result = false
                            if (it.type == KeyEventType.KeyUp && it.key == Key.Enter) {
                                onClick.invoke()
                                result = true
                            } else if (activated && it.key != Key.Back) {
                                if (it.type == KeyEventType.KeyDown) {
                                    if (it.key == Key.DirectionUp) {
                                        scope.launch(StashCoroutineExceptionHandler()) {
                                            state.scrollToIndex(
                                                state.currentIndex - 1 - it.nativeKeyEvent.repeatCount,
                                            )
                                        }
                                        result = true
                                    } else if (it.key == Key.DirectionDown) {
                                        scope.launch(StashCoroutineExceptionHandler()) {
                                            state.scrollToIndex(
                                                state.currentIndex + 1 + it.nativeKeyEvent.repeatCount,
                                            )
                                        }
                                        result = true
                                    }
                                }
                            }
                            result
                        },
                ),
        unfocusedCount = if (isNotTvDevice || activated && focused) 1 else 0,
        count = count,
        state = state,
    ) { index ->
        Text(
            text = indexTransformer.invoke(index),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
