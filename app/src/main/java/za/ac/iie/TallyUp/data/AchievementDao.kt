package za.ac.iie.TallyUp.data

import androidx.room.*
import za.ac.iie.TallyUp.models.Achievement

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Update
    suspend fun updateAchievement(achievement: Achievement)

    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY isUnlocked DESC, rarity DESC")
    suspend fun getAchievementsForUser(userId: String): List<Achievement>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND isUnlocked = 1 ORDER BY unlockedAt DESC")
    suspend fun getUnlockedAchievements(userId: String): List<Achievement>

    @Query("SELECT * FROM achievements WHERE id = :achievementId AND userId = :userId LIMIT 1")
    suspend fun getAchievement(achievementId: String, userId: String): Achievement?

    @Query("SELECT COUNT(*) FROM achievements WHERE userId = :userId AND isUnlocked = 1")
    suspend fun getUnlockedCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM achievements WHERE userId = :userId")
    suspend fun getTotalCount(userId: String): Int

    @Query("SELECT SUM(coinReward) FROM achievements WHERE userId = :userId AND isUnlocked = 1")
    suspend fun getTotalCoinsEarned(userId: String): Int?
}