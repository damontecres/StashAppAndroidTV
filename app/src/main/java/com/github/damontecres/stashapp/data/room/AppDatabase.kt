package com.github.damontecres.stashapp.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [RecentSearchItem::class, PlaybackEffect::class], version = 5)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentSearchItemsDao(): RecentSearchItemsDao

    abstract fun playbackEffectsDao(): PlaybackEffectsDao
}
