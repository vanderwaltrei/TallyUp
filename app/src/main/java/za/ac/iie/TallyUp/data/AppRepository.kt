@file:Suppress("PackageName")

package za.ac.iie.TallyUp.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import za.ac.iie.TallyUp.models.*

class AppRepository(private val context: Context) {

    private val gson = Gson()

    // Helper function to get current user ID from SharedPreferences
    private fun getCurrentUserId(): String {
        val prefs = context.getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", "") ?: ""
        if (userId.isEmpty()) {
            Log.w("AppRepository", "⚠️ No userId found in SharedPreferences")
        }
        return userId
    }

    // Get user-specific SharedPreferences
    // Each user gets their own preference file named "TallyUp_[userId]"
    // This ensures data persistence per user without deletion on logout
    private fun getUserSharedPreferences(): SharedPreferences {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            throw IllegalStateException("❌ No user logged in - cannot access user-specific data")
        }
        return context.getSharedPreferences("TallyUp_$userId", Context.MODE_PRIVATE)
    }

    /**
     * Save app state to user-specific SharedPreferences
     */
    @SuppressLint("UseKtx")
    fun saveAppState(appState: AppState) {
        try {
            val json = gson.toJson(appState)
            getUserSharedPreferences().edit().putString("app_state", json).apply()
            Log.d("AppRepository", "✅ Saved app state for user: ${getCurrentUserId()}")
        } catch (e: IllegalStateException) {
            Log.e("AppRepository", "❌ Cannot save app state: ${e.message}")
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Error saving app state: ${e.message}", e)
        }
    }

    /**
     * Load app state from user-specific SharedPreferences
     */
    fun loadAppState(): AppState {
        return try {
            val json = getUserSharedPreferences().getString("app_state", null)
            if (json != null) {
                gson.fromJson(json, AppState::class.java)
            } else {
                Log.d("AppRepository", "ℹ️ No saved app state found for user: ${getCurrentUserId()}")
                AppState()
            }
        } catch (e: IllegalStateException) {
            Log.e("AppRepository", "❌ Cannot load app state: ${e.message}")
            AppState()
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Error loading app state: ${e.message}", e)
            AppState()
        }
    }

    /**
     * Clear user-specific cached data
     * NOTE: Only call this if the user explicitly requests "Delete Account" or "Reset Data".
     * Do NOT call this on standard Logout if you want data to persist.
     */
    fun clearUserData() {
        val userId = getCurrentUserId()
        if (userId.isNotEmpty()) {
            try {
                context.getSharedPreferences("TallyUp_$userId", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
                Log.d("AppRepository", "✅ Cleared cached data for user: $userId")
            } catch (e: Exception) {
                Log.e("AppRepository", "❌ Error clearing user data: ${e.message}", e)
            }
        } else {
            Log.w("AppRepository", "⚠️ Cannot clear data - no user ID found")
        }
    }

    /**
     * Clear all app data (for complete reset or troubleshooting)
     */
    fun clearAllData() {
        try {
            val prefsDir = context.dataDir.resolve("shared_prefs")
            prefsDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("TallyUp_")) {
                    val prefName = file.nameWithoutExtension
                    context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply()
                }
            }
            Log.d("AppRepository", "✅ Cleared all cached data")
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Error clearing all data: ${e.message}", e)
        }
    }

    private fun getDefaultAccessories(): List<CharacterAccessory> = listOf(
        CharacterAccessory(
            id = "casual-outfit",
            name = "Casual Outfit",
            type = AccessoryType.OUTFIT,
            unlocked = true,
            rarity = Rarity.COMMON,
            cost = 0
        ),
        CharacterAccessory(
            id = "student-backpack",
            name = "Student Backpack",
            type = AccessoryType.ACCESSORY,
            unlocked = true,
            rarity = Rarity.COMMON,
            cost = 0
        )
    )

    private fun getRewardAccessories(): List<CharacterAccessory> = listOf(
        CharacterAccessory("graduation-cap", "Graduation Cap", AccessoryType.HAT, false, Rarity.RARE, 100),
        CharacterAccessory("business-suit", "Business Suit", AccessoryType.OUTFIT, false, Rarity.EPIC, 250),
        CharacterAccessory("party-hat", "Party Hat", AccessoryType.HAT, false, Rarity.COMMON, 50),
        CharacterAccessory("superhero-cape", "Superhero Cape", AccessoryType.ACCESSORY, false, Rarity.LEGENDARY, 500),
        CharacterAccessory("beach-background", "Beach Scene", AccessoryType.BACKGROUND, false, Rarity.RARE, 150),
        CharacterAccessory("winter-coat", "Winter Coat", AccessoryType.OUTFIT, false, Rarity.COMMON, 75)
    )

    fun createCharacter(type: CharacterType): Character {
        val defaultAccessories = getDefaultAccessories()
        val rewardAccessories = getRewardAccessories()

        return Character(
            type = type,
            accessories = defaultAccessories + rewardAccessories,
            equippedAccessories = listOf("casual-outfit", "student-backpack")
        )
    }
}