package com.github.damontecres.stashapp.ui.components.image

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageControlsOverlay(
    player: Player,
    isImageClip: Boolean,
    oCount: Int,
    onZoom: (Float) -> Unit,
    onRotate: (Int) -> Unit,
    onReset: () -> Unit,
    moreOnClick: () -> Unit,
    oCounterOnClick: () -> Unit,
    oCounterOnLongClick: () -> Unit,
    bringIntoViewRequester: BringIntoViewRequester?,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.tryRequestFocus()
    }
    val onFocused = { focusState: FocusState ->
        if (focusState.isFocused && bringIntoViewRequester != null) {
            scope.launch(StashCoroutineExceptionHandler()) { bringIntoViewRequester.bringIntoView() }
        }
    }
    val playPauseState = rememberPlayPauseButtonState(player)
    LazyRow(
        modifier =
            modifier
                .focusGroup()
                .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isImageClip) {
            item {
                Button(
                    onClick = playPauseState::onClick,
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged(onFocused),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        painter =
                            painterResource(
                                if (playPauseState.showPlay) R.drawable.baseline_play_arrow_24 else R.drawable.baseline_pause_24,
                            ),
                        contentDescription = null,
                    )
                }
            }
        } else {
            // Regular image
            // TODO might be able to apply to the player surface?
            // If enabling these, make sure the focusRequester is updated!
            item {
                ImageControlButton(
                    stringRes = R.string.fa_rotate_left,
                    onClick = { onRotate(-90) },
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged(onFocused),
                )
            }
            item {
                ImageControlButton(
                    stringRes = R.string.fa_rotate_right,
                    onClick = { onRotate(90) },
                    modifier =
                        Modifier
                            .onFocusChanged(onFocused),
                )
            }
            item {
                ImageControlButton(
                    stringRes = R.string.fa_magnifying_glass_plus,
                    onClick = { onZoom(.15f) },
                    modifier =
                        Modifier
                            .onFocusChanged(onFocused),
                )
            }
            item {
                ImageControlButton(
                    stringRes = R.string.fa_magnifying_glass_minus,
                    onClick = { onZoom(-.15f) },
                    modifier =
                        Modifier
                            .onFocusChanged(onFocused),
                )
            }
            item {
                ImageControlButton(
                    drawableRes = R.drawable.baseline_undo_24,
                    onClick = onReset,
                    modifier =
                        Modifier
                            .onFocusChanged(onFocused),
                )
            }
        }
        // O-Counter
        item {
            Button(
                onClick = oCounterOnClick,
                onLongClick = oCounterOnLongClick,
                modifier =
                    Modifier
                        .onFocusChanged(onFocused),
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
        // More button
        item {
            Button(
                onClick = moreOnClick,
                onLongClick = {},
                modifier =
                    Modifier
                        .onFocusChanged(onFocused),
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
fun ImageControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes stringRes: Int = 0,
    @DrawableRes drawableRes: Int = 0,
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(8.dp),
        modifier = modifier,
    ) {
        if (stringRes != 0) {
            Text(
                text = stringResource(stringRes),
                fontFamily = FontAwesome,
                style = MaterialTheme.typography.titleLarge,
            )
        } else {
            Icon(
                painter = painterResource(drawableRes),
                contentDescription = "",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
