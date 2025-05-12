package com.github.damontecres.stashapp.ui.components

import android.widget.Toast
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.playOnClickSound
import com.github.damontecres.stashapp.ui.util.playSoundOnFocus
import com.github.damontecres.stashapp.views.getRatingAsDecimalString

enum class StarRatingPrecision {
    FULL,
    HALF,
    QUARTER,
    ;

    companion object {
        fun fromFloat(value: Float): StarRatingPrecision =
            if (value <= .25f) {
                QUARTER
            } else if (value <= .5f) {
                HALF
            } else {
                FULL
            }
    }
}

val FilledStarColor = Color(0xFFFFC700)
val EmptyStarColor = Color(0x2AFFC700)

@Composable
fun Rating100(
    rating100: Int,
    onRatingChange: (Int) -> Unit,
    uiConfig: ComposeUiConfig,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    bgColor: Color = AppColors.TransparentBlack75, // MaterialTheme.colorScheme.background,
) {
    Rating100(
        rating100,
        onRatingChange,
        uiConfig.ratingAsStars,
        uiConfig.starPrecision,
        enabled && uiConfig.readOnlyModeDisabled,
        uiConfig.playSoundOnFocus,
        modifier,
        bgColor,
    )
}

@Composable
fun Rating100(
    rating100: Int,
    onRatingChange: (Int) -> Unit,
    ratingAsStars: Boolean,
    starPrecision: StarRatingPrecision,
    enabled: Boolean,
    playSoundOnFocus: Boolean,
    modifier: Modifier = Modifier,
    bgColor: Color = AppColors.TransparentBlack75, // MaterialTheme.colorScheme.background,
) {
    if (ratingAsStars) {
        StarRating(
            rating100 = rating100,
            onRatingChange = onRatingChange,
            precision = starPrecision,
            enabled = enabled,
            playSoundOnFocus = playSoundOnFocus,
            modifier = modifier,
            bgColor = bgColor,
        )
    } else {
        DecimalRating(
            rating100 = rating100,
            onRatingChange = onRatingChange,
            enabled = enabled,
            playSoundOnFocus = playSoundOnFocus,
            modifier = modifier,
        )
    }
}

@Composable
fun StarRating(
    rating100: Int,
    onRatingChange: (Int) -> Unit,
    precision: StarRatingPrecision,
    enabled: Boolean,
    playSoundOnFocus: Boolean,
    modifier: Modifier = Modifier,
    bgColor: Color = AppColors.TransparentBlack75, // MaterialTheme.colorScheme.background,
) {
    val context = LocalContext.current
    var tempRating by remember(rating100) { mutableIntStateOf(rating100) }
    val percentage = (if (enabled) tempRating else rating100) / 100f
    val focusRequesters = remember { List(5) { FocusRequester() } }
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor),
    ) {
        LazyRow(
            modifier =
                Modifier
                    .selectableGroup()
                    .padding(4.dp)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            if (percentage in 0f..<1f) {
                                drawRect(
                                    color = bgColor,
                                    topLeft = Offset(size.width * percentage, 0f),
                                    blendMode = BlendMode.SrcAtop,
                                )
                            }
                        }
                    }.focusGroup()
                    .focusProperties {
                        onEnter = {
                            val index =
                                if (rating100 <= 20) {
                                    0
                                } else if (rating100 <= 40) {
                                    1
                                } else if (rating100 <= 60) {
                                    2
                                } else if (rating100 <= 80) {
                                    3
                                } else {
                                    4
                                }
                            focusRequesters[index].tryRequestFocus()
                        }
                    },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            for (i in 1..5) {
                item {
                    val isRated = (if (enabled) tempRating else rating100) >= (i * 20)
                    val icon = Icons.Filled.Star
                    var focused by remember { mutableStateOf(false) }

                    val focusedColor =
                        if (focused) {
                            MaterialTheme.colorScheme.border
                        } else {
                            Color.Unspecified
                        }
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(focusedColor),
                    ) {
                        Icon(
                            imageVector = icon,
                            tint = FilledStarColor,
                            contentDescription = null,
                            modifier =
                                if (enabled) {
                                    Modifier
                                        .onFocusChanged {
                                            focused = it.isFocused
                                            if (it.isFocused) {
                                                tempRating = i * 20
                                            } else {
                                                tempRating = rating100
                                            }
                                        }.playSoundOnFocus(playSoundOnFocus)
                                        .focusRequester(focusRequesters[i - 1])
                                        .focusProperties {
                                            left =
                                                if (i == 1) focusRequesters.last() else FocusRequester.Default
                                            right =
                                                if (i == 5) focusRequesters.first() else FocusRequester.Default
                                        }.selectable(
                                            selected = isRated,
                                            onClick = {
                                                if (playSoundOnFocus) playOnClickSound(context)
                                                val newRating100 =
                                                    when (precision) {
                                                        StarRatingPrecision.FULL -> i * 20
                                                        StarRatingPrecision.HALF -> {
                                                            if (rating100 > i * 20) {
                                                                i * 20
                                                            } else if (rating100 == i * 20) {
                                                                i * 20 - 10
                                                            } else if (rating100 == i * 20 - 10) {
                                                                (i - 1) * 20
                                                            } else if (i == 1 && rating100 < 20) {
                                                                0
                                                            } else {
                                                                (i) * 20
                                                            }
                                                        }

                                                        StarRatingPrecision.QUARTER -> {
                                                            // TODO
                                                            null
                                                        }
                                                    }
                                                if (newRating100 != null) {
                                                    tempRating = newRating100
                                                    onRatingChange(newRating100)
                                                }
                                            },
                                        )
                                } else {
                                    Modifier
                                }.fillMaxHeight()
                                    .aspectRatio(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DecimalRating(
    rating100: Int,
    onRatingChange: (Int) -> Unit,
    enabled: Boolean,
    playSoundOnFocus: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val rating = getRatingAsDecimalString(rating100, false)
    var showDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val background =
        if (focused) {
            MaterialTheme.colorScheme.border.copy(alpha = .75f)
        } else {
            Color.Unspecified
        }
    if (playSoundOnFocus) {
        LaunchedEffect(focused) {
            if (focused) playOnClickSound(context)
        }
    }
    Row(
        modifier =
            modifier
                .padding(4.dp)
                .background(background)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                ) {
                    if (playSoundOnFocus) playOnClickSound(context)
                    showDialog = true
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            tint = FilledStarColor,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
        )
        Text(
            text = "$rating / 10",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
        )
    }
    if (enabled && showDialog) {
        var enteredRating by remember { mutableStateOf(rating) }
        val submit = {
            val newEnteredRating = enteredRating.toDoubleOrNull()
            if (newEnteredRating == null || newEnteredRating !in (0.0..10.0)) {
                Toast
                    .makeText(
                        context,
                        "Invalid rating: $newEnteredRating",
                        Toast.LENGTH_SHORT,
                    ).show()
            } else {
                onRatingChange.invoke((newEnteredRating * 10).toInt())
                showDialog = false
            }
        }
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(),
        ) {
            val elevatedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            LazyColumn(
                modifier =
                    Modifier
                        .graphicsLayer {
                            this.clip = true
                            this.shape = RoundedCornerShape(24.0.dp)
                        }.drawBehind { drawRect(color = elevatedContainerColor) },
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.set_rating),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                item {
                    EditTextBox(
                        value = enteredRating,
                        onValueChange = {
                            enteredRating = it
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = { submit() },
                            ),
                        isInputValid = { input ->
                            input.toDoubleOrNull()?.let { it in (0.0..10.0) } == true
                        },
                    )
                }
                item {
                    Button(
                        onClick = submit,
                    ) {
                        Text(
                            text = stringResource(R.string.stashapp_actions_submit),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun StarRatingPreview() {
    AppTheme {
        val bgColor = AppColors.TransparentBlack75
        Column(modifier = Modifier.background(Color.DarkGray)) {
            var rating by remember { mutableIntStateOf(50) }
            StarRating(
                rating100 = rating,
                precision = StarRatingPrecision.HALF,
                onRatingChange = { rating = it },
                enabled = true,
                playSoundOnFocus = true,
                bgColor = bgColor,
                modifier =
                    Modifier
                        .padding(16.dp)
                        .height(32.dp),
            )
            var rating2 by remember { mutableIntStateOf(60) }
            StarRating(
                rating100 = rating2,
                precision = StarRatingPrecision.FULL,
                onRatingChange = { rating2 = it },
                enabled = true,
                playSoundOnFocus = true,
                bgColor = bgColor,
                modifier = Modifier.height(32.dp),
            )
            StarRating(
                rating100 = 25,
                precision = StarRatingPrecision.HALF,
                onRatingChange = {},
                enabled = true,
                playSoundOnFocus = true,
                bgColor = bgColor,
                modifier = Modifier.height(32.dp),
            )
            StarRating(
                rating100 = 75,
                precision = StarRatingPrecision.HALF,
                onRatingChange = {},
                enabled = true,
                playSoundOnFocus = true,
                bgColor = bgColor,
                modifier = Modifier.height(32.dp),
            )
            Rating100(
                rating100 = 75,
                ratingAsStars = false,
                starPrecision = StarRatingPrecision.HALF,
                onRatingChange = {},
                enabled = true,
                playSoundOnFocus = true,
                bgColor = bgColor,
                modifier = Modifier.height(32.dp),
            )
        }
    }
}
