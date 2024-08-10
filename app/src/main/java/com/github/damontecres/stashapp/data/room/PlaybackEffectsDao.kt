package com.github.damontecres.stashapp.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaybackEffectsDao {
    @Query("SELECT * FROM playback_effects WHERE serverUrl = :serverUrl AND id = :sceneId")
    fun getPlaybackEffect(
        serverUrl: String,
        sceneId: String,
    ): PlaybackEffect?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg items: PlaybackEffect)

    @Query("DELETE FROM playback_effects")
    fun deleteAll()
}
