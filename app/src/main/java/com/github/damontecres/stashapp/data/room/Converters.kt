package com.github.damontecres.stashapp.data.room

import androidx.room.TypeConverter
import com.github.damontecres.stashapp.data.DataType
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromDataType(value: Int?): DataType? = value?.let { DataType.entries[it] }

    @TypeConverter
    fun dataTypeToInt(value: DataType?): Int? = value?.ordinal
}
