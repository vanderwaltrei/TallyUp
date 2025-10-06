@file:Suppress("PackageName")

package za.ac.iie.TallyUp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlin.math.roundToInt

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val target: Double,
    var current: Double = 0.0,
    val deadline: String,
    val createdAt: String = "",
    val userId: String // Add this field to associate with user
) {
    fun progressPercent(): Int {
        if (target <= 0.0) return 0
        return ((current / target) * 100).coerceIn(0.0, 100.0).roundToInt()
    }
}