package com.github.damontecres.stashapp.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Icon

@Composable
fun StarRating(
    rating100: Int,
    onRatingChange: (Int) -> Unit,
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
            val icon = if (isRated) Icons.Filled.Star else Icons.Filled.Star
            val color = if (isRated) Color(0xFFFFC700) else Color(0x40FFC700)
            Icon(
                imageVector = icon,
                tint = color,
                contentDescription = null,
                modifier =
                    if (enabled) {
                        Modifier
                            .selectable(
                                selected = isRated,
                                onClick = { onRatingChange(i * 20) },
                            )
                    } else {
                        Modifier
                    }.fillMaxHeight().aspectRatio(1f),
            )
        }
    }
}
