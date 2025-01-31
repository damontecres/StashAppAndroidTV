package com.github.damontecres.stashapp.ui.components

import androidx.annotation.StringRes
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.util.resume_position
import java.util.concurrent.atomic.AtomicInteger

fun interface LongClicker<T> {
    fun longClick(
        item: T,
        filterAndPosition: FilterAndPosition?,
    )
}

data class LongClickerAction<T>(
    @StringRes val title: Int,
    val filter: (T) -> Boolean,
    val action: (T, FilterAndPosition?) -> Unit,
) {
    val id: Int = idCounter.getAndIncrement()

    companion object {
        private val idCounter = AtomicInteger(0)
    }
}

data class LongClickPopup(
    val item: Any,
    val filterAndPosition: FilterAndPosition?,
    val actions: List<LongClickerAction<Any>>,
)

fun buildLongClickActionList(
    nav: NavigationManager,
    itemOnClicker: ItemOnClicker<Any>,
): List<LongClickerAction<Any>> =
    listOf(
        LongClickerAction<Any>(
            R.string.go_to,
            { true },
            { item, fp -> itemOnClicker.onClick(item, fp!!) },
        ),
        LongClickerAction<Any>(
            R.string.play_scene,
            { it is SlimSceneData && (it.resume_position == null || it.resume_position!! <= 0) },
            { item, _ ->
                item as SlimSceneData
                nav.navigate(Destination.Playback(item.id, 0L, PlaybackMode.CHOOSE))
            },
        ),
        LongClickerAction<Any>(
            R.string.resume,
            { it is SlimSceneData && (it.resume_position != null && it.resume_position!! > 0) },
            { item, _ ->
                item as SlimSceneData
                nav.navigate(
                    Destination.Playback(
                        item.id,
                        item.resume_position ?: 0,
                        PlaybackMode.CHOOSE,
                    ),
                )
            },
        ),
        LongClickerAction<Any>(
            R.string.restart,
            { it is SlimSceneData && (it.resume_position != null && it.resume_position!! > 0) },
            { item, _ ->
                item as SlimSceneData
                nav.navigate(Destination.Playback(item.id, 0L, PlaybackMode.CHOOSE))
            },
        ),
    )
