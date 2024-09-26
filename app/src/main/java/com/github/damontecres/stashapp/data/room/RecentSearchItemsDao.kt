package com.github.damontecres.stashapp.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.damontecres.stashapp.data.DataType

/**
 * Retrieve and store [RecentSearchItem]s
 */
@Dao
interface RecentSearchItemsDao {
    @Query("SELECT * FROM recent_search_items WHERE serverUrl = :serverUrl AND dataType = :dataType  ORDER BY timestamp DESC LIMIT :count")
    fun getMostRecent(
        count: Int = 25,
        serverUrl: String,
        dataType: DataType,
    ): List<RecentSearchItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg items: RecentSearchItem)
}
