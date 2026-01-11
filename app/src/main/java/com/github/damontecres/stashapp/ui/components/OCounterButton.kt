package com.github.damontecres.stashapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.ui.components.playback.PlaybackButton
import com.github.damontecres.stashapp.ui.components.playback.PlaybackFaButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun OCounterButton(
    sfwMode: Boolean,
    oCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OCounterButtonWrapper(
        sfwMode = sfwMode,
        originalOnClick = onClick,
        modifier = modifier,
    ) { newOnClick ->
        Button(
            onClick = newOnClick,
            onLongClick = onLongClick,
            enabled = enabled,
            modifier = Modifier,
        ) {
            if (sfwMode) {
                Text(
                    text = stringResource(R.string.fa_thumbs_up),
                    fontSize = 20.sp,
                    fontFamily = FontAwesome,
                    style = MaterialTheme.typography.titleSmall,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.sweat_drops),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = oCount.toString(),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
fun PlaybackOCountButton(
    sfwMode: Boolean,
    onClick: () -> Unit,
    onControllerInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OCounterButtonWrapper(
        sfwMode = sfwMode,
        originalOnClick = onClick,
        modifier = modifier,
    ) { newOnClick ->
        if (sfwMode) {
            PlaybackFaButton(
                iconRes = R.string.fa_thumbs_up,
                onClick = newOnClick,
                enabled = enabled,
                onControllerInteraction = onControllerInteraction,
            )
        } else {
            PlaybackButton(
                iconRes = R.drawable.sweat_drops,
                onClick = newOnClick,
                enabled = enabled,
                onControllerInteraction = onControllerInteraction,
            )
        }
    }
}

// Based on https://gist.github.com/ardakazanci/1297a44276d6a0be06c1227cb446dead
@Composable
private fun OCounterButtonWrapper(
    sfwMode: Boolean,
    originalOnClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (
        onClick: () -> Unit,
    ) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val floatingList = remember { mutableStateListOf<Int>() }
    val fraction = 0.25f
    val count = 5

    val config =
        CountConfig(
            radius = lerp(0.5f, 2f, fraction),
            delay = lerp(100L, 600L, fraction),
        )

    val scale = remember { Animatable(1f) }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        if (!sfwMode) {
            floatingList.forEach { id ->
                FloatingIcon(
                    key = id,
                    config = config,
                    onAnimationEnd = { floatingList.remove(id) },
                )
            }
        }
        content.invoke {
            if (!sfwMode) {
                scope.launch {
                    scale.animateTo(0.85f, tween(100, easing = LinearEasing))
                    scale.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioHighBouncy),
                    )
                }
                repeat(count) { index ->
                    scope.launch {
                        delay(index * config.delay)
                        floatingList.add(index + Random.nextInt(1000))
                    }
                }
            }
            originalOnClick.invoke()
        }
    }
}

data class CountConfig(
    val radius: Float,
    val delay: Long,
)

@Composable
private fun FloatingIcon(
    key: Int,
    config: CountConfig,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val angle = remember { Random.nextDouble(-90.0, 180.0) }
    val baseRadius = remember { Random.nextDouble(100.0, 500.0).toFloat() }
    val radius = baseRadius * config.radius

    val xOffset = remember { Animatable(0f) }
    val yOffset = remember { Animatable(0f) }
    val alpha = remember { Animatable(0.75f) }

    LaunchedEffect(key, onAnimationEnd) {
        launch {
            xOffset.animateTo(
                targetValue = (radius * cos(Math.toRadians(angle))).toFloat(),
                animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
            )
        }
        launch {
            yOffset.animateTo(
                targetValue = (radius * -sin(Math.toRadians(angle))).toFloat() - 250f,
                animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1400),
            )
            onAnimationEnd()
        }
    }

    Icon(
        painter = painterResource(R.drawable.sweat_drops),
        contentDescription = null,
        tint = Color.LightGray.copy(alpha = alpha.value),
        modifier =
            modifier
                .offset {
                    IntOffset(xOffset.value.roundToInt(), yOffset.value.roundToInt() - 40)
                }.size(32.dp),
    )
}
