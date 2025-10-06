@file:Suppress("PackageName")

package za.ac.iie.TallyUp.data

import androidx.room.TypeConverter
import java.util.Date

class Converters {

    @TypeConverter
    fun fromPhotoUris(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toPhotoUris(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(timestamp: Long): Date {
        return Date(timestamp)
    }
}