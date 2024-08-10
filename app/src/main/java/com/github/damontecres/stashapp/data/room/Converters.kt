package com.github.damontecres.stashapp.data.room

import androidx.room.TypeConverter
import com.github.damontecres.stashapp.data.DataType
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromDataType(value: Int?): DataType? {
        return value?.let { DataType.entries[it] }
    }

    @TypeConverter
    fun dataTypeToInt(value: DataType?): Int? {
        return value?.ordinal
    }
}
