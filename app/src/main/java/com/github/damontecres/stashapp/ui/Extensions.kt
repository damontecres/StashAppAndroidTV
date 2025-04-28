package com.github.damontecres.stashapp.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.util.StashServer

data class GlobalContext(
    val server: StashServer,
    val navigationManager: NavigationManager,
)

val LocalGlobalContext =
    compositionLocalOf<GlobalContext> { throw IllegalStateException("Shouldn't call this") }

object PlayerContext {
    fun player(
        context: Context,
        server: StashServer,
    ): Player =
        StashExoPlayer.getInstance(context, server).apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
}

val LocalPlayerContext =
    compositionLocalOf<PlayerContext> { PlayerContext }

fun Modifier.enableMarquee(focused: Boolean) =
    if (focused) {
        basicMarquee(initialDelayMillis = 250, animationMode = MarqueeAnimationMode.Immediately, velocity = 40.dp)
    } else {
        basicMarquee(animationMode = MarqueeAnimationMode.WhileFocused)
    }

/**
 * Try to call [FocusRequester.requestFocus], but catch & log the exception if something is not configured properly
 */
fun FocusRequester.tryRequestFocus(): Boolean =
    try {
        requestFocus()
        true
    } catch (ex: IllegalStateException) {
        Log.w("tryRequestFocus", "Failed to request focus", ex)
        false
    }

fun Modifier.isElementVisible(onVisibilityChanged: (Boolean) -> Unit) =
    composed {
        val isVisible by remember { derivedStateOf { mutableStateOf(false) } }
        LaunchedEffect(isVisible.value) { onVisibilityChanged.invoke(isVisible.value) }
        this.onGloballyPositioned { layoutCoordinates ->
            isVisible.value = layoutCoordinates.parentLayoutCoordinates?.let {
                val parentBounds = it.boundsInWindow()
                val childBounds = layoutCoordinates.boundsInWindow()
                parentBounds.overlaps(childBounds)
            } ?: false
        }
    }
