package com.github.damontecres.stashapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Icon
import com.github.damontecres.stashapp.ui.MainTheme
import kotlin.math.abs

enum class StarRatingPrecision {
    FULL,
    HALF,
    QUARTER,
    ;

    companion object {
        fun fromFloat(value: Float): StarRatingPrecision =
            if (value <= .25f) {
                QUARTER
            } else if (value < .5f) {
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
) {
    Row(
        modifier =
            modifier
                .selectableGroup()
                .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 1..5) {
            val isRated = rating100 >= (i * 20)
            val icon = Icons.Filled.Star
            val percentage =
                if (rating100 % 20 != 0 && rating100 >= ((i - 1) * 20)) {
                    abs(rating100 - (i * 20)) / 20f
                } else {
                    0f
                }

            Icon(
                imageVector = icon,
                tint =
                    if (isRated && percentage >= 0f) {
                        FilledStarColor
                    } else if (percentage == 0f) {
                        EmptyStarColor
                    } else {
                        Color.Unspecified
                    },
                contentDescription = null,
                modifier =
                    if (enabled) {
                        Modifier
                            .selectable(
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
                                                } else {
                                                    (i) * 20
                                                }
                                            }

                                            StarRatingPrecision.QUARTER -> TODO()
                                        }
                                    onRatingChange(newRating100)
                                },
                            )
                    } else {
                        Modifier
                    }.fillMaxHeight()
                        .aspectRatio(1f)
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                if (percentage > 0) {
                                    drawRect(
                                        color = FilledStarColor,
                                        topLeft = Offset(0f, 0f),
                                        size = Size(size.width * (1 - percentage), size.height),
                                        blendMode = BlendMode.SrcAtop,
                                    )
                                    drawRect(
                                        color = EmptyStarColor,
                                        topLeft = Offset(size.width * (1 - percentage), 0f),
                                        blendMode = BlendMode.SrcAtop,
                                    )
//                                    drawRect(brush, blendMode = BlendMode.SrcAtop)
                                }
                            }
                        },
            )
        }
    }
}

@Preview
@Composable
private fun StarRatingPreview() {
    MainTheme {
        Column {
            var rating by remember { mutableIntStateOf(50) }
            StarRating(
                rating100 = rating,
                precision = StarRatingPrecision.HALF,
                onRatingChange = { rating = it },
                enabled = true,
                modifier = Modifier,
            )
            var rating2 by remember { mutableIntStateOf(60) }
            StarRating(
                rating100 = rating2,
                precision = StarRatingPrecision.FULL,
                onRatingChange = { rating2 = it },
                enabled = true,
                modifier = Modifier,
            )
            StarRating(
                rating100 = 25,
                precision = StarRatingPrecision.HALF,
                onRatingChange = {},
                enabled = true,
                modifier = Modifier,
            )
            StarRating(
                rating100 = 75,
                precision = StarRatingPrecision.HALF,
                onRatingChange = {},
                enabled = true,
                modifier = Modifier,
            )
        }
    }
}
