package com.github.damontecres.stashapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.AppTheme

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
fun StarRating(
    rating100: Int,
    onRatingChange: (Int) -> Unit,
    precision: StarRatingPrecision,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    bgColor: Color = AppColors.TransparentBlack75, // MaterialTheme.colorScheme.background,
) {
    var tempRating by remember { mutableIntStateOf(rating100) }
    val percentage = tempRating / 100f
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor),
    ) {
        Row(
            modifier =
                Modifier
                    .selectableGroup()
                    .padding(4.dp)
                    .height(IntrinsicSize.Min)
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
                    },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 1..5) {
                val isRated = tempRating >= (i * 20)
                val icon = Icons.Filled.Star
                var focused by remember { mutableStateOf(false) }

                // TODO this still looks weird with fractional star
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
                                    }.selectable(
                                        selected = isRated,
                                        onClick = {
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
                bgColor = bgColor,
                modifier = Modifier.padding(16.dp),
            )
            var rating2 by remember { mutableIntStateOf(60) }
            StarRating(
                rating100 = rating2,
                precision = StarRatingPrecision.FULL,
                onRatingChange = { rating2 = it },
                enabled = true,
                bgColor = bgColor,
                modifier = Modifier,
            )
            StarRating(
                rating100 = 25,
                precision = StarRatingPrecision.HALF,
                onRatingChange = {},
                enabled = true,
                bgColor = bgColor,
                modifier = Modifier,
            )
            StarRating(
                rating100 = 75,
                precision = StarRatingPrecision.HALF,
                onRatingChange = {},
                enabled = true,
                bgColor = bgColor,
                modifier = Modifier,
            )
        }
    }
}
