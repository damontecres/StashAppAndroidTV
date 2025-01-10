package com.github.damontecres.stashapp.data.room

import androidx.room.Embedded
import androidx.room.Entity
import com.github.damontecres.stashapp.data.VideoFilter

/**
 * Store a [VideoFilter] for a specific scene (by server & scene ID)
 */
@Entity(tableName = "playback_effects", primaryKeys = ["serverUrl", "id"])
data class PlaybackEffect(
    val serverUrl: String,
    val id: String,
    @Embedded val videoFilter: VideoFilter,
)
