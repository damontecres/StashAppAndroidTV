package com.github.damontecres.stashapp.ui.components.screensaver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.useExistingImageAsPlaceholder
import coil3.request.ImageRequest
import coil3.request.transitionFactory
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.ui.util.CrossFadeFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ScreensaverContent(
    imageData: ImageData?,
    duration: Duration,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(modifier = modifier) {
        AsyncImage(
            contentDescription = null,
            model =
                ImageRequest
                    .Builder(context)
                    .data(imageData?.paths?.image)
                    .transitionFactory(CrossFadeFactory(2000.milliseconds))
                    .useExistingImageAsPlaceholder(true)
                    .build(),
            modifier =
                Modifier
                    .fillMaxSize(),
        )
    }
}
