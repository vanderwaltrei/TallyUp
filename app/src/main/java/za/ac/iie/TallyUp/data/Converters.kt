package za.ac.iie.TallyUp.data

import androidx.room.TypeConverter

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
    fun fromList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toList(data: String): List<String> =
        if (data.isEmpty()) emptyList() else data.split(",")

}
