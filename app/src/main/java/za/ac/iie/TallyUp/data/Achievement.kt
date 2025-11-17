package za.ac.iie.TallyUp.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coinReward: Int,
    val rarity: AchievementRarity,
    val category: AchievementCategory,
    var isUnlocked: Boolean = false,
    var progress: Int = 0,
    var maxProgress: Int = 1,
    val iconResId: Int = 0,
    var unlockedAt: Long = 0L,
    val userId: String
)

enum class AchievementRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

enum class AchievementCategory {
    GETTING_STARTED,
    TRANSACTIONS,
    BUDGET,
    GOALS,
    SPECIAL,
    COLLECTION
}

// Pre-defined achievement IDs for easy reference
object AchievementIds {
    const val TRANSACTION_TRACKER = "transaction_tracker"
    const val PHOTO_PROOF = "photo_proof"
    const val INCOME_BOOST = "income_boost"
    const val CHARACTER_COLLECTOR = "character_collector"
}