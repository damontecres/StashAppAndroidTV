package com.github.damontecres.stashapp.ui.components.playback

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.Player
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.PlaylistItem
import com.github.damontecres.stashapp.data.toPlayListItem
import com.github.damontecres.stashapp.ui.Material3MainTheme
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.MappedList

@Composable
fun PlaylistListDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    player: Player,
    pager: ComposePager<StashData>?,
    modifier: Modifier = Modifier,
) {
    if (show && pager != null) {
        Material3MainTheme {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
                dialogWindowProvider?.window?.let { window ->
                    window.setGravity(Gravity.START)
                    window.setDimAmount(0f)
                }
                PlaylistList(
                    mediaItemCount = player.mediaItemCount,
                    currentIndex = player.currentMediaItemIndex,
                    mediaItemCountOffset = 0, // TODO
                    items =
                        MappedList(pager) { index, item ->
                            when (item) {
                                is MarkerData -> item.toPlayListItem(index)
                                is SlimSceneData -> item.toPlayListItem(index)
                                else -> {
                                    PlaylistItem(
                                        index,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                    )
                                }
                            }
                        },
                    title = pager.filter.name ?: stringResource(pager.filter.dataType.pluralStringId),
                    onClick = { index ->
                        player.seekTo(index, 0L)
                        onDismiss.invoke()
                    },
                    modifier =
                        modifier
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                )
            }
        }
    }
}
