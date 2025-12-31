package com.github.damontecres.stashapp.ui.components.scene

import androidx.annotation.StringRes
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.ui.PreviewTheme
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.ui.components.EditButton
import com.github.damontecres.stashapp.ui.components.OCounterButton

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayButtons(
    sfwMode: Boolean,
    resumePosition: Long,
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
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(8.dp),
        modifier =
            modifier
                .focusGroup()
                .focusRestorer(firstFocus),
    ) {
        if (resumePosition > 0 && !alwaysStartFromBeginning) {
            item {
//                LaunchedEffect(Unit) { firstFocus.tryRequestFocus() }
                PlayButton(
                    R.string.resume,
                    resumePosition,
                    Icons.Default.PlayArrow,
                    PlaybackMode.Choose,
                    playOnClick,
                    Modifier
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
                        .onFocusChanged(buttonOnFocusChanged)
                        .focusRequester(firstFocus)
                        .focusRequester(focusRequester),
                )
            }
        }
        // O-Counter
        item {
            OCounterButton(
                sfwMode = sfwMode,
                oCount = oCount,
                onClick = oCounterOnClick,
                onLongClick = oCounterOnLongClick,
                modifier =
                    Modifier
                        .onFocusChanged(buttonOnFocusChanged),
                enabled = showEditButton,
            )
        }
        // Edit button
        if (showEditButton) {
            item {
                EditButton(
                    onClick = editOnClick,
                    modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
                )
            }
        }

        // More button
        item {
            Button(
                onClick = moreOnClick,
                onLongClick = {},
                modifier =
                    Modifier
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

@Preview(widthDp = 800)
@Composable
private fun PlayButtonsPreview() {
    PreviewTheme {
        PlayButtons(
            resumePosition = 1000L,
            oCount = 10,
            playOnClick = { _, _ -> },
            editOnClick = {},
            moreOnClick = { },
            oCounterOnClick = { },
            oCounterOnLongClick = {},
            buttonOnFocusChanged = {},
            focusRequester = remember { FocusRequester() },
            alwaysStartFromBeginning = false,
            showEditButton = true,
            sfwMode = false,
            modifier = Modifier,
        )
    }
}
