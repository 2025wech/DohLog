package com.autoledger.data.local.converter

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime): String =
        dateTime.format(formatter)

    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime =
        LocalDateTime.parse(value, formatter)
}