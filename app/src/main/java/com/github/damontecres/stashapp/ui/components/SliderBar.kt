package com.github.damontecres.stashapp.ui.components

import android.view.KeyEvent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.util.handleDPadKeyEvents

@Composable
fun SliderBar(
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    interval: Int = 1,
    color: Color = MaterialTheme.colorScheme.border,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val animatedIndicatorHeight by animateDpAsState(
        targetValue = 6.dp.times((if (isFocused) 2f else 1f)),
    )
    var currentValue by remember(value) { mutableIntStateOf(value) }
    val percent = currentValue.toFloat() / (max - min)

    val handleSeekEventModifier =
        Modifier.handleDPadKeyEvents(
            triggerOnAction = KeyEvent.ACTION_DOWN,
            onCenter = {
                onChange(currentValue)
            },
            onLeft = {
                if (currentValue <= min) {
                    currentValue = max
                } else {
                    currentValue = (currentValue - interval).coerceAtLeast(min)
                }
                onChange(currentValue)
            },
            onRight = {
                if (currentValue >= max) {
                    currentValue = min
                } else {
                    currentValue = (currentValue + interval).coerceAtMost(max)
                }
                onChange(currentValue)
            },
        )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(animatedIndicatorHeight)
                    .padding(horizontal = 4.dp)
                    .then(handleSeekEventModifier)
                    .focusable(interactionSource = interactionSource),
            onDraw = {
                val yOffset = size.height.div(2)
                drawLine(
                    color = color.copy(alpha = 0.15f),
                    start = Offset(x = 0f, y = yOffset),
                    end = Offset(x = size.width, y = yOffset),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(x = 0f, y = yOffset),
                    end =
                        Offset(
//                        x = size.width.times(if (isSelected) seekProgress else progress),
                            x = size.width.times(percent),
                            y = yOffset,
                        ),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = Color.White,
                    radius = size.height + 2,
                    center = Offset(x = size.width.times(percent), y = yOffset),
                )
            },
        )
    }
}

@Preview
@Composable
private fun SliderBarPreview() {
    AppTheme {
        Column {
            SliderBar(
                value = 40,
                min = 0,
                max = 100,
                onChange = {},
                color = Color.Red,
            )
        }
    }
}
