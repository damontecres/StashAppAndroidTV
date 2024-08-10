package com.github.damontecres.stashapp.data.room

import androidx.room.Entity

@Entity(tableName = "playback_effects", primaryKeys = ["serverUrl", "id"])
data class PlaybackEffect(val serverUrl: String, val id: String, val rotation: Int)
