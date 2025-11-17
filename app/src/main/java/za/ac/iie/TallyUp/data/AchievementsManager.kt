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
        val database = AppDatabase.getDatabase(context)
        val achievementDao = database.achievementDao()

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
        Log.d(TAG, "Initialized ${defaultAchievements.size} achievements for user $userId")
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
        val database = AppDatabase.getDatabase(context)
        val achievementDao = database.achievementDao()
        val unlockedAchievements = mutableListOf<Achievement>()

        // Check Transaction Tracker (first transaction)
        val transactionTracker = achievementDao.getAchievement(AchievementIds.TRANSACTION_TRACKER, userId)
        if (transactionTracker != null && !transactionTracker.isUnlocked) {
            transactionTracker.isUnlocked = true
            transactionTracker.progress = 1
            transactionTracker.unlockedAt = System.currentTimeMillis()
            achievementDao.updateAchievement(transactionTracker)
            CharacterManager.addCoins(context, transactionTracker.coinReward)
            unlockedAchievements.add(transactionTracker)
            Log.d(TAG, "Unlocked: Transaction Tracker (+${transactionTracker.coinReward} coins)")
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
                Log.d(TAG, "Unlocked: Income Boost (+${incomeBoost.coinReward} coins)")
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
                    Log.d(TAG, "Unlocked: Photo Proof (+${photoProof.coinReward} coins)")
                }
                achievementDao.updateAchievement(photoProof)
            }
        }

        return unlockedAchievements
    }

    /**
     * Check and unlock achievement when an accessory is purchased
     */
    suspend fun checkCharacterCollectorAchievement(
        context: Context,
        userId: String
    ): Achievement? {
        val database = AppDatabase.getDatabase(context)
        val achievementDao = database.achievementDao()

        val characterCollector = achievementDao.getAchievement(AchievementIds.CHARACTER_COLLECTOR, userId)
        if (characterCollector != null && !characterCollector.isUnlocked) {
            // Count purchased accessories
            val purchasedCount = countPurchasedAccessories(context)
            characterCollector.progress = purchasedCount

            if (purchasedCount >= characterCollector.maxProgress) {
                characterCollector.isUnlocked = true
                characterCollector.unlockedAt = System.currentTimeMillis()
                CharacterManager.addCoins(context, characterCollector.coinReward)
                achievementDao.updateAchievement(characterCollector)
                Log.d(TAG, "Unlocked: Character Collector (+${characterCollector.coinReward} coins)")
                return characterCollector
            } else {
                achievementDao.updateAchievement(characterCollector)
            }
        }

        return null
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
        val database = AppDatabase.getDatabase(context)
        val achievementDao = database.achievementDao()

        val unlockedCount = achievementDao.getUnlockedCount(userId)
        val totalCount = achievementDao.getTotalCount(userId)
        val totalCoinsEarned = achievementDao.getTotalCoinsEarned(userId) ?: 0

        return AchievementStats(
            unlockedCount = unlockedCount,
            totalCount = totalCount,
            totalCoinsEarned = totalCoinsEarned,
            completionPercentage = if (totalCount > 0) (unlockedCount * 100) / totalCount else 0
        )
    }

    /**
     * Get all achievements for display
     */
    suspend fun getAllAchievements(context: Context, userId: String): List<Achievement> {
        val database = AppDatabase.getDatabase(context)
        return database.achievementDao().getAchievementsForUser(userId)
    }

    /**
     * Get recently unlocked achievements
     */
    suspend fun getRecentlyUnlocked(context: Context, userId: String, limit: Int = 3): List<Achievement> {
        val database = AppDatabase.getDatabase(context)
        return database.achievementDao().getUnlockedAchievements(userId).take(limit)
    }
}

data class AchievementStats(
    val unlockedCount: Int,
    val totalCount: Int,
    val totalCoinsEarned: Int,
    val completionPercentage: Int
)