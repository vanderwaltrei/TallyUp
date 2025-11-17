package za.ac.iie.TallyUp.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.models.*

object AchievementManager {

    private const val TAG = "AchievementManager"

    /**
     * Initialize default achievements for a new user
     */
    suspend fun initializeAchievements(context: Context, userId: String) {
        Log.d(TAG, "🎯 Initializing achievements for userId: $userId")

        if (userId.isEmpty() || userId == "default") {
            Log.e(TAG, "❌ Invalid userId provided: '$userId' - cannot initialize achievements")
            return
        }

        try {
            val database = AppDatabase.getDatabase(context)
            val achievementDao = database.achievementDao()

            // Check if achievements already exist for this user
            val existingAchievements = achievementDao.getAchievementsForUser(userId)
            if (existingAchievements.isNotEmpty()) {
                Log.d(TAG, "✅ User already has ${existingAchievements.size} achievements, skipping initialization")
                return
            }

            val defaultAchievements = listOf(
                Achievement(
                    id = AchievementIds.TRANSACTION_TRACKER,
                    name = "Transaction Tracker",
                    description = "Add your first transaction",
                    coinReward = 50,
                    rarity = AchievementRarity.COMMON,
                    category = AchievementCategory.GETTING_STARTED,
                    maxProgress = 1,
                    iconResId = R.drawable.ic_list,
                    userId = userId
                ),
                Achievement(
                    id = AchievementIds.PHOTO_PROOF,
                    name = "Photo Proof",
                    description = "Attach photos to 5 transactions",
                    coinReward = 75,
                    rarity = AchievementRarity.RARE,
                    category = AchievementCategory.TRANSACTIONS,
                    maxProgress = 5,
                    iconResId = R.drawable.ic_camera,
                    userId = userId
                ),
                Achievement(
                    id = AchievementIds.INCOME_BOOST,
                    name = "Income Boost",
                    description = "Record an income transaction over R500",
                    coinReward = 150,
                    rarity = AchievementRarity.EPIC,
                    category = AchievementCategory.SPECIAL,
                    maxProgress = 1,
                    iconResId = R.drawable.ic_trending_up,
                    userId = userId
                ),
                Achievement(
                    id = AchievementIds.CHARACTER_COLLECTOR,
                    name = "Character Collector",
                    description = "Purchase all shop accessories",
                    coinReward = 500,
                    rarity = AchievementRarity.LEGENDARY,
                    category = AchievementCategory.COLLECTION,
                    maxProgress = 8, // Total shop items
                    iconResId = R.drawable.ic_trophy,
                    userId = userId
                )
            )

            achievementDao.insertAchievements(defaultAchievements)
            Log.d(TAG, "✅ Successfully initialized ${defaultAchievements.size} achievements for user $userId")

            // Verify they were saved
            val verifyAchievements = achievementDao.getAchievementsForUser(userId)
            Log.d(TAG, "✅ Verification: Found ${verifyAchievements.size} achievements in database")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing achievements for user $userId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Check and unlock achievement when a transaction is added
     */
    suspend fun checkTransactionAchievements(
        context: Context,
        userId: String,
        amount: Double,
        type: String,
        hasPhotos: Boolean
    ): List<Achievement> {
        Log.d(TAG, "🔍 Checking transaction achievements for userId: $userId")

        if (userId.isEmpty() || userId == "default") {
            Log.e(TAG, "❌ Invalid userId: '$userId' - skipping achievement check")
            return emptyList()
        }

        val database = AppDatabase.getDatabase(context)
        val achievementDao = database.achievementDao()
        val unlockedAchievements = mutableListOf<Achievement>()

        try {
            // Check Transaction Tracker (first transaction)
            val transactionTracker = achievementDao.getAchievement(AchievementIds.TRANSACTION_TRACKER, userId)
            if (transactionTracker != null && !transactionTracker.isUnlocked) {
                transactionTracker.isUnlocked = true
                transactionTracker.progress = 1
                transactionTracker.unlockedAt = System.currentTimeMillis()
                achievementDao.updateAchievement(transactionTracker)
                CharacterManager.addCoins(context, transactionTracker.coinReward)
                unlockedAchievements.add(transactionTracker)
                Log.d(TAG, "🏆 Unlocked: Transaction Tracker (+${transactionTracker.coinReward} coins)")
            }

            // Check Income Boost (income over R500)
            if (type == "Income" && amount > 500.0) {
                val incomeBoost = achievementDao.getAchievement(AchievementIds.INCOME_BOOST, userId)
                if (incomeBoost != null && !incomeBoost.isUnlocked) {
                    incomeBoost.isUnlocked = true
                    incomeBoost.progress = 1
                    incomeBoost.unlockedAt = System.currentTimeMillis()
                    achievementDao.updateAchievement(incomeBoost)
                    CharacterManager.addCoins(context, incomeBoost.coinReward)
                    unlockedAchievements.add(incomeBoost)
                    Log.d(TAG, "🏆 Unlocked: Income Boost (+${incomeBoost.coinReward} coins)")
                }
            }

            // Check Photo Proof (photos in 5 transactions)
            if (hasPhotos) {
                val photoProof = achievementDao.getAchievement(AchievementIds.PHOTO_PROOF, userId)
                if (photoProof != null && !photoProof.isUnlocked) {
                    photoProof.progress++
                    if (photoProof.progress >= photoProof.maxProgress) {
                        photoProof.isUnlocked = true
                        photoProof.unlockedAt = System.currentTimeMillis()
                        CharacterManager.addCoins(context, photoProof.coinReward)
                        unlockedAchievements.add(photoProof)
                        Log.d(TAG, "🏆 Unlocked: Photo Proof (+${photoProof.coinReward} coins)")
                    } else {
                        Log.d(TAG, "📈 Photo Proof progress: ${photoProof.progress}/${photoProof.maxProgress}")
                    }
                    achievementDao.updateAchievement(photoProof)
                }
            }

            return unlockedAchievements

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking transaction achievements: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Check and unlock achievement when an accessory is purchased
     */
    suspend fun checkCharacterCollectorAchievement(
        context: Context,
        userId: String
    ): Achievement? {
        Log.d(TAG, "🔍 Checking Character Collector achievement for userId: $userId")

        if (userId.isEmpty() || userId == "default") {
            Log.e(TAG, "❌ Invalid userId: '$userId' - skipping achievement check")
            return null
        }

        try {
            val database = AppDatabase.getDatabase(context)
            val achievementDao = database.achievementDao()

            val characterCollector = achievementDao.getAchievement(AchievementIds.CHARACTER_COLLECTOR, userId)
            if (characterCollector != null && !characterCollector.isUnlocked) {
                // Count purchased accessories
                val purchasedCount = countPurchasedAccessories(context)
                characterCollector.progress = purchasedCount

                Log.d(TAG, "📈 Character Collector progress: $purchasedCount/${characterCollector.maxProgress}")

                if (purchasedCount >= characterCollector.maxProgress) {
                    characterCollector.isUnlocked = true
                    characterCollector.unlockedAt = System.currentTimeMillis()
                    CharacterManager.addCoins(context, characterCollector.coinReward)
                    achievementDao.updateAchievement(characterCollector)
                    Log.d(TAG, "🏆 Unlocked: Character Collector (+${characterCollector.coinReward} coins)")
                    return characterCollector
                } else {
                    achievementDao.updateAchievement(characterCollector)
                }
            }

            return null

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking Character Collector achievement: ${e.message}", e)
            return null
        }
    }

    /**
     * Count how many accessories have been purchased
     */
    private fun countPurchasedAccessories(context: Context): Int {
        val accessoryIds = listOf(
            "luna_gamer", "luna_goddess", "luna_gothic", "luna_strawberry",
            "max_light", "max_villain", "max_gojo", "max_gamer"
        )

        return accessoryIds.count { CharacterManager.isPurchased(context, it) }
    }

    /**
     * Get achievement statistics for a user
     */
    suspend fun getAchievementStats(context: Context, userId: String): AchievementStats {
        Log.d(TAG, "📊 Getting achievement stats for userId: $userId")

        if (userId.isEmpty() || userId == "default") {
            Log.w(TAG, "⚠️ Invalid userId: '$userId' - returning empty stats")
            return AchievementStats(0, 0, 0, 0)
        }

        return try {
            val database = AppDatabase.getDatabase(context)
            val achievementDao = database.achievementDao()

            val unlockedCount = achievementDao.getUnlockedCount(userId)
            val totalCount = achievementDao.getTotalCount(userId)
            val totalCoinsEarned = achievementDao.getTotalCoinsEarned(userId) ?: 0

            Log.d(TAG, "✅ Stats: $unlockedCount/$totalCount unlocked, $totalCoinsEarned coins earned")

            AchievementStats(
                unlockedCount = unlockedCount,
                totalCount = totalCount,
                totalCoinsEarned = totalCoinsEarned,
                completionPercentage = if (totalCount > 0) (unlockedCount * 100) / totalCount else 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting achievement stats: ${e.message}", e)
            AchievementStats(0, 0, 0, 0)
        }
    }

    /**
     * Get all achievements for display
     */
    suspend fun getAllAchievements(context: Context, userId: String): List<Achievement> {
        Log.d(TAG, "📋 Getting all achievements for userId: $userId")

        if (userId.isEmpty() || userId == "default") {
            Log.w(TAG, "⚠️ Invalid userId: '$userId' - returning empty list")
            return emptyList()
        }

        return try {
            val database = AppDatabase.getDatabase(context)
            val achievements = database.achievementDao().getAchievementsForUser(userId)
            Log.d(TAG, "✅ Retrieved ${achievements.size} achievements")
            achievements
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting all achievements: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get recently unlocked achievements
     */
    suspend fun getRecentlyUnlocked(context: Context, userId: String, limit: Int = 3): List<Achievement> {
        Log.d(TAG, "📋 Getting recent achievements for userId: $userId (limit: $limit)")

        if (userId.isEmpty() || userId == "default") {
            Log.w(TAG, "⚠️ Invalid userId: '$userId' - returning empty list")
            return emptyList()
        }

        return try {
            val database = AppDatabase.getDatabase(context)
            val recent = database.achievementDao().getUnlockedAchievements(userId).take(limit)
            Log.d(TAG, "✅ Retrieved ${recent.size} recent achievements")
            recent
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting recent achievements: ${e.message}", e)
            emptyList()
        }
    }
}

data class AchievementStats(
    val unlockedCount: Int,
    val totalCount: Int,
    val totalCoinsEarned: Int,
    val completionPercentage: Int
)