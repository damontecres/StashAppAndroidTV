package com.github.damontecres.stashapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.ui.pages.DialogParams
import com.github.damontecres.stashapp.ui.pages.maxPlaylistSize
import com.github.damontecres.stashapp.util.resume_position
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

fun interface LongClicker<T> {
    fun longClick(
        item: T,
        filterAndPosition: FilterAndPosition?,
    )
}

class DefaultLongClicker(
    private val nav: NavigationManager,
    private val itemOnClick: ItemOnClicker<Any>,
    private val alwaysStartFromBeginning: Boolean,
    private val onLongClick: (DialogParams) -> Unit,
) : LongClicker<Any> {
    override fun longClick(
        item: Any,
        filterAndPosition: FilterAndPosition?,
    ) {
        item as StashData
        val context = StashApplication.getApplication()
        val title = extractTitle(item) ?: ""
        val items =
            buildList {
                if (item is MarkerData) {
                    add(
                        DialogItem(
                            context.getString(R.string.play_scene),
                            Icons.Default.PlayArrow,
                        ) {
                            itemOnClick.onClick(item, filterAndPosition)
                        },
                    )
                    add(
                        DialogItem(
                            context.getString(R.string.go_to_scene),
                            Icons.AutoMirrored.Default.ArrowForward,
                        ) {
                            nav.navigate(
                                Destination.Item(DataType.SCENE, item.scene.minimalSceneData.id),
                            )
                        },
                    )
                    add(
                        DialogItem(
                            context.getString(R.string.stashapp_details),
                            Icons.Default.Info,
                        ) {
                            nav.navigate(
                                Destination.MarkerDetails(item.id),
                            )
                        },
                    )
                } else {
                    add(
                        DialogItem(context.getString(R.string.go_to), Icons.Default.Info) {
                            itemOnClick.onClick(
                                item,
                                filterAndPosition,
                            )
                        },
                    )
                }
                if (item is SlimSceneData) {
                    if (item.resume_time != null && item.resume_time > 0 && alwaysStartFromBeginning) {
                        add(
                            DialogItem(
                                context.getString(R.string.resume),
                                Icons.Default.PlayArrow,
                            ) {
                                nav.navigate(
                                    Destination.Playback(
                                        item.id,
                                        item.resume_position!!,
                                        PlaybackMode.Choose,
                                    ),
                                )
                            },
                        )
                        add(
                            DialogItem(
                                context.getString(R.string.restart),
                                Icons.Default.Refresh,
                            ) {
                                nav.navigate(
                                    Destination.Playback(
                                        item.id,
                                        0L,
                                        PlaybackMode.Choose,
                                    ),
                                )
                            },
                        )
                    } else {
                        add(
                            DialogItem(
                                context.getString(R.string.play_scene),
                                Icons.Default.PlayArrow,
                            ) {
                                nav.navigate(
                                    Destination.Playback(
                                        item.id,
                                        0L,
                                        PlaybackMode.Choose,
                                    ),
                                )
                            },
                        )
                    }
                }
                if ((item is SlimSceneData || item is MarkerData) &&
                    filterAndPosition != null &&
                    filterAndPosition.position < maxPlaylistSize // TODO
                ) {
                    add(
                        DialogItem(
                            context.getString(R.string.play_from_here),
                            Icons.Default.PlayArrow,
                        ) {
                            nav.navigate(
                                Destination.Playlist(
                                    filterAndPosition.filter,
                                    filterAndPosition.position,
                                    30.seconds.inWholeMilliseconds,
                                ),
                            )
                        },
                    )
                }
            }
        onLongClick.invoke(DialogParams(true, title, items))
    }
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
                nav.navigate(Destination.Playback(item.id, 0L, PlaybackMode.Choose))
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
                        PlaybackMode.Choose,
                    ),
                )
            },
        ),
        LongClickerAction<Any>(
            R.string.restart,
            { it is SlimSceneData && (it.resume_position != null && it.resume_position!! > 0) },
            { item, _ ->
                item as SlimSceneData
                nav.navigate(Destination.Playback(item.id, 0L, PlaybackMode.Choose))
            },
        ),
    )
