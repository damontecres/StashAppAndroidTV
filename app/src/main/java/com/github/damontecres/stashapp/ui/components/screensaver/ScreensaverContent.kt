package com.github.damontecres.stashapp.ui.components.screensaver

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transitionFactory
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.ui.util.CrossFadeFactory
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ScreensaverContent(
    imageData: ImageData,
    duration: Duration,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(modifier = modifier) {
        key(imageData) {
            var started by remember(imageData) { mutableStateOf(false) }
            val scale by animateFloatAsState(
                if (started) 1.10f else 1f,
                animationSpec =
                    tween(
                        durationMillis = duration.inWholeMilliseconds.toInt(),
                        delayMillis = 0,
                        LinearEasing,
                    ),
            )
            LaunchedEffect(Unit) {
                delay(100)
                started = true
            }
            AsyncImage(
                contentDescription = null,
                model =
                    ImageRequest
                        .Builder(context)
                        .data(imageData.paths.image)
                        .transitionFactory(CrossFadeFactory(750.milliseconds))
                        .build(),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
            )
        }
    }
}
