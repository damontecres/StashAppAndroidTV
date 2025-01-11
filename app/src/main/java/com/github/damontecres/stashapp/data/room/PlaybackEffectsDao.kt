package com.github.damontecres.stashapp.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.damontecres.stashapp.data.DataType

/**
 * Store [PlaybackEffect]/][VideoFilter]s
 */
@Dao
interface PlaybackEffectsDao {
    @Query("SELECT * FROM playback_effects WHERE serverUrl = :serverUrl AND id = :id AND dataType = :dataType")
    fun getPlaybackEffect(
        serverUrl: String,
        id: String,
        dataType: DataType,
    ): PlaybackEffect?

    @Query("SELECT * FROM playback_effects WHERE serverUrl = :serverUrl")
    fun getPlaybackEffects(serverUrl: String): List<PlaybackEffect>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg items: PlaybackEffect)

    @Query("DELETE FROM playback_effects")
    fun deleteAll()
}
