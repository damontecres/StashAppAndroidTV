package com.github.damontecres.stashapp.ui.components.scene

import androidx.annotation.StringRes
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.util.resume_position

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayButtons(
    scene: FullSceneData,
    oCount: Int,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    editOnClick: () -> Unit,
    moreOnClick: () -> Unit,
    oCounterOnClick: () -> Unit,
    oCounterOnLongClick: () -> Unit,
    buttonOnFocusChanged: (FocusState) -> Unit,
    focusRequester: FocusRequester,
    alwaysStartFromBeginning: Boolean,
    showEditButton: Boolean,
    modifier: Modifier = Modifier,
) {
    val firstFocus = remember { FocusRequester() }
    val resume = scene.resume_position ?: 0
    LazyRow(
        modifier =
            modifier
                .padding(top = 24.dp, bottom = 24.dp)
                .focusGroup()
                .focusRestorer { firstFocus },
    ) {
        if (resume > 0 && !alwaysStartFromBeginning) {
            item {
//                LaunchedEffect(Unit) { firstFocus.tryRequestFocus() }
                PlayButton(
                    R.string.resume,
                    resume,
                    Icons.Default.PlayArrow,
                    PlaybackMode.Choose,
                    playOnClick,
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged)
                        .focusRequester(firstFocus)
                        .focusRequester(focusRequester),
                )
            }
            item {
                PlayButton(
                    R.string.restart,
                    0L,
                    Icons.Default.Refresh,
                    PlaybackMode.Choose,
                    playOnClick,
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged),
                )
            }
        } else {
            item {
//                LaunchedEffect(Unit) { firstFocus.tryRequestFocus() }
                PlayButton(
                    R.string.play_scene,
                    0L,
                    Icons.Default.PlayArrow,
                    PlaybackMode.Choose,
                    playOnClick,
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged)
                        .focusRequester(firstFocus)
                        .focusRequester(focusRequester),
                )
            }
        }
        // O-Counter
        item {
            Button(
                onClick = oCounterOnClick,
                onLongClick = oCounterOnLongClick,
                enabled = showEditButton,
                modifier =
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    painter = painterResource(R.drawable.sweat_drops),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = oCount.toString(),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        // Edit button
        if (showEditButton) {
            item {
                Button(
                    onClick = editOnClick,
                    onLongClick = {},
                    modifier =
                        Modifier
                            .padding(start = 8.dp, end = 8.dp)
                            .onFocusChanged(buttonOnFocusChanged),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.stashapp_actions_edit),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        // More button
        item {
            Button(
                onClick = moreOnClick,
                onLongClick = {},
                modifier =
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .onFocusChanged(buttonOnFocusChanged),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.more),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
fun PlayButton(
    @StringRes title: Int,
    resume: Long,
    icon: ImageVector,
    mode: PlaybackMode,
    playOnClick: (position: Long, mode: PlaybackMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { playOnClick.invoke(resume, mode) },
        modifier = modifier,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
