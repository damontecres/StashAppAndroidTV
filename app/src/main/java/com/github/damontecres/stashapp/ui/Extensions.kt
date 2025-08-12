package com.github.damontecres.stashapp.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.compat.detectTvDevice
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getFilterArgs
import com.github.damontecres.stashapp.util.name
import com.github.damontecres.stashapp.util.putFilterArgs
import kotlin.time.Duration.Companion.seconds

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

enum class DeviceType {
    TV,
    TOUCH,
}

val LocalDeviceType = compositionLocalOf { if (detectTvDevice) DeviceType.TV else DeviceType.TOUCH }

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

val filterArgsSaver =
    Saver<MutableState<FilterArgs>, Any>(
        save = { value ->
            Bundle().putFilterArgs("filterArgs", value.value)
        },
        restore = { value ->
            mutableStateOf((value as Bundle).getFilterArgs("filterArgs")!!)
        },
    )

fun showShort(message: String) = Toast.makeText(StashApplication.getApplication(), message, Toast.LENGTH_SHORT).show()

fun showAddTag(tag: TagData) = showShort("Added tag '${tag.name}'")

fun showAddPerf(perf: PerformerData) = showShort("Added performer '${perf.name}'")

fun showAddGroup(group: GroupData) = showShort("Added group '${group.name}'")

fun showAddMarker(marker: MarkerData) = showShort("Added marker at ${marker.seconds.seconds}")

fun showSetStudio(studioName: String) = showShort("Set studio to '$studioName'")

fun showSetStudio(studio: StudioData) = showSetStudio(studio.name)

fun showAddGallery(gallery: GalleryData) = showShort("Added group '${gallery.name}'")

inline fun <T> List<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    val index = this.indexOfFirst(predicate)
    return if (index >= 0) index else null
}

/**
 * Whether the [KeyEvent] is a key up event pressing media play or media play/pause
 */
fun isPlayKeyUp(key: KeyEvent) = key.type == KeyEventType.KeyUp && (key.key == Key.MediaPlay || key.key == Key.MediaPlayPause)

/**
 * Whether the [CriterionModifier] is a check for null or not null
 */
val CriterionModifier.nullCheck: Boolean
    get() = this == CriterionModifier.IS_NULL || this == CriterionModifier.NOT_NULL

/**
 * Whether the [CriterionModifier] is between or not between and requires two input values
 */
val CriterionModifier.between: Boolean
    get() = this == CriterionModifier.BETWEEN || this == CriterionModifier.NOT_BETWEEN

val IntCriterionInput.valid: Boolean
    get() {
        if (modifier.between) {
            val val2 = value2.getOrNull()
            return val2 != null && value < val2
        } else {
            return true
        }
    }

val FloatCriterionInput.valid: Boolean
    get() {
        if (modifier.between) {
            val val2 = value2.getOrNull()
            return val2 != null && value < val2
        } else {
            return true
        }
    }

/**
 * Get the gallery ID for the filter iff it has a single gallery associated
 */
val ImageFilterType.galleryId: String?
    get() {
        return galleries.getOrNull()?.let { multi ->
            multi.value.getOrNull()?.let {
                if (multi.modifier != CriterionModifier.EXCLUDES && it.size == 1) it[0] else null
            }
        }
    }
