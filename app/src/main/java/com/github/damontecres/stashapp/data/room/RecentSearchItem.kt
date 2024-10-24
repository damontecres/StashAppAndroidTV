package com.github.damontecres.stashapp.data.room

import androidx.room.Entity
import com.github.damontecres.stashapp.data.DataType
import java.util.Date

/**
 * Represents an item that the user recently used in a search-for task
 *
 * Must be identified by server, data type, & ID
 */
@Entity(tableName = "recent_search_items", primaryKeys = ["serverUrl", "id", "dataType"])
data class RecentSearchItem(val serverUrl: String, val id: String, val dataType: DataType, val timestamp: Date = Date())
