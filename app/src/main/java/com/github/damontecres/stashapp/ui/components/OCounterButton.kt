package com.github.damontecres.stashapp.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R

@Composable
fun OCounterButton(
    oCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.sweat_drops),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = oCount.toString(),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
