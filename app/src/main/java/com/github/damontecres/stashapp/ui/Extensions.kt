package com.github.damontecres.stashapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.navigation.Destination

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
        ProvideTextStyle(MaterialTheme.typography.titleMedium) {
            Text(text = label, color = MaterialTheme.colorScheme.onPrimaryContainer)
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

val Destination.supportsCompose: Boolean
    get() = this in setOf(Destination.Main)

/**
 * Makes the current dialog a focus group with a [FocusRequester] and restricts the focus from
 * exiting its bounds while it's visible.
 */
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
fun Modifier.dialogFocusable() =
    composed {
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            focusManager.moveFocus(FocusDirection.Enter)
        }
        this.then(
            Modifier
                .focusRequester(focusRequester)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusGroup(),
        )
    }

fun Modifier.ifElse(
    condition: Boolean,
    ifTrueModifier: Modifier,
    ifFalseModifier: Modifier = Modifier,
): Modifier = then(if (condition) ifTrueModifier else ifFalseModifier)
