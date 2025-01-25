package com.github.damontecres.stashapp.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Switch
import androidx.tv.material3.Text

fun Modifier.enableMarquee(focused: Boolean) =
    if (focused) {
        basicMarquee(initialDelayMillis = 250, animationMode = MarqueeAnimationMode.Immediately, velocity = 40.dp)
    } else {
        basicMarquee(animationMode = MarqueeAnimationMode.WhileFocused)
    }

@Composable
fun SwitchWithLabel(
    label: String,
    state: Boolean,
    onStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier =
            modifier
                .clip(shape = RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Switch,
                    onClick = {
                        onStateChange(!state)
                    },
                ).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(Color.White)) {
            Text(text = label)
        }
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Switch(
            checked = state,
            onCheckedChange = {
                onStateChange(it)
            },
        )
    }
}
