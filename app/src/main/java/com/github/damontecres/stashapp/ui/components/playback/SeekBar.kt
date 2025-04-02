package com.github.damontecres.stashapp.ui.components.playback

/*
 * Modified from https://github.com/android/tv-samples
 *
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Scale
import com.github.damontecres.stashapp.ui.util.CoilPreviewTransformation
import com.github.damontecres.stashapp.ui.util.handleDPadKeyEvents
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

@Composable
fun SeekBarImpl(
    progress: Float,
    bufferedProgress: Float,
    duration: Long,
    onSeek: (seekProgress: Float) -> Unit,
    controllerViewState: ControllerViewState,
    modifier: Modifier = Modifier,
    intervals: Int = 10,
    previewImageUrl: String? = null,
    aspectRatio: Float = 16f / 9,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val color = MaterialTheme.colorScheme.border
    val animatedIndicatorHeight by animateDpAsState(
        targetValue = 6.dp.times((if (isFocused) 2f else 1f)),
    )
    var seekProgress by remember { mutableFloatStateOf(progress) }

    val offset = 1f / intervals

    val handleSeekEventModifier =
        Modifier.handleDPadKeyEvents(
            onEnter = {
                controllerViewState.pulseControls()
                onSeek(seekProgress)
            },
            onLeft = {
                controllerViewState.pulseControls()
                seekProgress = (seekProgress - offset).coerceAtLeast(0f)
                onSeek(seekProgress)
            },
            onRight = {
                controllerViewState.pulseControls()
                seekProgress = (seekProgress + offset).coerceAtMost(1f)
                onSeek(seekProgress)
            },
        )
    val context = LocalContext.current
    val imageLoader = SingletonImageLoader.get(LocalPlatformContext.current)
    var imageLoaded by remember { mutableStateOf(false) }
    if (previewImageUrl.isNotNullOrBlank()) {
        LaunchedEffect(previewImageUrl) {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(previewImageUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .scale(Scale.FILL)
                    .build()
            val result = imageLoader.enqueue(request).job.await()
            imageLoaded = result.image != null
        }
    }
    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val height = 160.dp
        val width = height * aspectRatio
        val heightPx = with(LocalDensity.current) { height.toPx().toInt() }
        val widthPx = with(LocalDensity.current) { width.toPx().toInt() }

        if (isFocused) {
            Row {
                if (seekProgress > 0) {
                    Spacer(modifier = Modifier.weight(seekProgress))
                }
                Box(
                    modifier =
                        Modifier
                            .weight(1f),
                ) {
                    Column(
                        modifier =
                        Modifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (imageLoaded) {
                            AsyncImage(
                                modifier =
                                    Modifier
                                        .width(width)
                                        .height(height)
                                        .background(Color.Black)
                                        .border(1.5.dp, color = MaterialTheme.colorScheme.border),
                                model =
                                    ImageRequest
                                        .Builder(context)
                                        .data(previewImageUrl)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .transformations(
                                            CoilPreviewTransformation(
                                                widthPx,
                                                heightPx,
                                                duration,
                                                (duration * seekProgress).toLong(),
                                            ),
                                        ).build(),
                                contentScale = ContentScale.None,
                                imageLoader = imageLoader,
                                contentDescription = null,
                            )
                        }
                        Text(
                            text = (seekProgress * duration / 1000).toLong().seconds.toString(),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                if (seekProgress < 1f) {
                    Spacer(modifier = Modifier.weight(abs(1 - seekProgress)))
                }
            }
        }
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
                    color = color.copy(alpha = 0.50f),
                    start = Offset(x = 0f, y = yOffset),
                    end =
                        Offset(
                            x = size.width.times(bufferedProgress),
                            y = yOffset,
                        ),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(x = 0f, y = yOffset),
                    end =
                        Offset(
//                        x = size.width.times(if (isSelected) seekProgress else progress),
                            x = size.width.times(seekProgress),
                            y = yOffset,
                        ),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
            },
        )
    }
}
