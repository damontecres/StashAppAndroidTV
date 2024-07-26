package com.github.damontecres.stashapp.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClassicCard
import androidx.tv.material3.Text
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.util.titleOrFilename

@OptIn(ExperimentalGlideComposeApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun SceneCard(
    scene: SlimSceneData,
    onClick: (() -> Unit),
) {
    ClassicCard(
        onClick = onClick,
        image = {
            GlideImage(
                model = scene.paths.screenshot,
                contentDescription = "",
                modifier =
                    Modifier
                        .padding(0.dp)
                        .width(ScenePresenter.CARD_WIDTH.dp)
                        .height(ScenePresenter.CARD_HEIGHT.dp),
            )
        },
        title = { Text(scene.titleOrFilename ?: "") },
    )
}
