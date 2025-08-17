package com.github.damontecres.stashapp.ui.compat

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardGlow
import androidx.tv.material3.CardScale
import androidx.tv.material3.CardShape
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.ui.DeviceType
import com.github.damontecres.stashapp.ui.LocalDeviceType

@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: CardShape = CardDefaults.shape(),
    colors: CardColors = CardDefaults.colors(),
    scale: CardScale = CardDefaults.scale(),
    border: CardBorder = CardDefaults.border(),
    glow: CardGlow = CardDefaults.glow(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalDeviceType.current == DeviceType.TV) {
        androidx.tv.material3.Card(
            onClick = onClick,
            modifier = modifier,
            onLongClick = onLongClick,
            shape = shape,
            colors = colors,
            scale = scale,
            border = border,
            glow = glow,
            interactionSource = interactionSource,
            content = content,
        )
    } else {
        // TODO this is kind of hack to force tv.Text to use the right color
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        ) {
            androidx.compose.material3.Card(
                modifier =
                    modifier
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            onClick = onClick,
                            onLongClick = onLongClick,
                        ),
                content = content,
            )
        }
    }
}
