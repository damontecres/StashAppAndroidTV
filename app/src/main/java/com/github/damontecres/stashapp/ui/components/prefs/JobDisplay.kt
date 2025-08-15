package com.github.damontecres.stashapp.ui.components.prefs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.StashJob
import com.github.damontecres.stashapp.api.type.JobStatus

@Composable
fun JobDisplay(
    job: StashJob,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                contentDescription = null,
                imageVector =
                    when (job.status) {
                        JobStatus.READY -> Icons.Default.Add
                        JobStatus.RUNNING -> Icons.Default.PlayArrow
                        JobStatus.FINISHED -> Icons.Default.Done
                        JobStatus.STOPPING -> Icons.Default.Info
                        JobStatus.CANCELLED -> Icons.Default.Done
                        JobStatus.FAILED -> Icons.Default.Warning
                        JobStatus.UNKNOWN__ -> Icons.Default.Warning
                    },
                tint =
                    when (job.status) {
                        JobStatus.READY -> MaterialTheme.colorScheme.border
                        JobStatus.RUNNING -> Color(0xFF006800)
                        JobStatus.FINISHED -> Color(0xFF006800)
                        JobStatus.STOPPING -> MaterialTheme.colorScheme.border
                        JobStatus.CANCELLED -> MaterialTheme.colorScheme.error
                        JobStatus.FAILED -> MaterialTheme.colorScheme.error
                        JobStatus.UNKNOWN__ -> MaterialTheme.colorScheme.error
                    },
            )
            Text(
                text = job.description,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            job.progress?.let {
                val percentage = (job.progress * 1000).toInt() / 10.0
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        job.subTasks?.let {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier,
            ) {
                it.forEach { subTask ->
                    Text(
                        text = subTask,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
