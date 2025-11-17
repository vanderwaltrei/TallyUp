package za.ac.iie.TallyUp.utils

import android.content.Context
import android.content.SharedPreferences
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.models.Character
import za.ac.iie.TallyUp.models.CharacterType
import za.ac.iie.TallyUp.models.Mood
import za.ac.iie.TallyUp.models.CharacterAccessory
import java.util.*

object CharacterManager {
    private const val PREFS_NAME = "TallyUpPrefs"
    private const val DEFAULT_CHARACTER = "max"

    // Helper function to get user-specific key
    private fun getUserKey(userId: String, key: String): String {
        return "${userId}_$key"
    }

    // Helper function to get current user ID from SharedPreferences
    private fun getCurrentUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", null)

        // Fallback to loggedInEmail if userId is not set (for backward compatibility)
        if (userId.isNullOrEmpty()) {
            val email = prefs.getString("loggedInEmail", "default")
            return email ?: "default"
        }

        return userId
    }

    // ========== BASE CHARACTER SELECTION (User-Specific) ==========

    fun saveSelectedCharacter(context: Context, character: String) {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(getUserKey(userId, "selected_character"), character)
            putLong(getUserKey(userId, "last_active_date"), Date().time)
            apply()
        }
    }

    fun getSelectedCharacter(context: Context): String {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(getUserKey(userId, "selected_character"), DEFAULT_CHARACTER) ?: DEFAULT_CHARACTER
    }

    // ========== EQUIPPED CHARACTER (Shop Accessories - User-Specific) ==========

    fun saveEquippedCharacter(context: Context, characterId: String?) {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (characterId != null) {
                putString(getUserKey(userId, "equipped_character"), characterId)
            } else {
                remove(getUserKey(userId, "equipped_character"))
            }
            apply()
        }
    }

    fun getEquippedCharacter(context: Context): String? {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(getUserKey(userId, "equipped_character"), null)
    }

    // ========== CURRENT CHARACTER (Priority: Equipped > Selected) ==========

    fun getCurrentCharacterId(context: Context): String {
        val equippedCharacter = getEquippedCharacter(context)
        return equippedCharacter ?: getSelectedCharacter(context)
    }

    // ========== DRAWABLE AND DISPLAY METHODS ==========

    fun getCharacterDrawable(context: Context): Int {
        val currentCharacter = getCurrentCharacterId(context)

        return when (currentCharacter) {
            // Base characters
            "luna" -> R.drawable.character_female
            "max" -> R.drawable.character_happy

            // Shop accessories (character variations)
            "luna_gamer" -> R.drawable.luna_gamer
            "luna_goddess" -> R.drawable.luna_goddess
            "luna_gothic" -> R.drawable.luna_gothic
            "luna_strawberry" -> R.drawable.luna_strawberry
            "max_light" -> R.drawable.max_light
            "max_villain" -> R.drawable.max_villain
            "max_gojo" -> R.drawable.max_gojo
            "max_gamer" -> R.drawable.max_ginger

            else -> R.drawable.character_happy
        }
    }

    fun getCharacterName(context: Context): String {
        val currentCharacter = getCurrentCharacterId(context)

        return when (currentCharacter) {
            "luna" -> "Luna"
            "max" -> "Max"
            "luna_gamer" -> "Gamer Luna"
            "luna_goddess" -> "Light Luna"
            "luna_gothic" -> "Gothic Luna"
            "luna_strawberry" -> "Strawberry Luna"
            "max_light" -> "Light Max"
            "max_villain" -> "Villain Max"
            "max_gojo" -> "Protagonist Max"
            "max_gamer" -> "Gamer Max"
            else -> "Max"
        }
    }

    fun getCharacterType(context: Context): CharacterType {
        val currentCharacter = getCurrentCharacterId(context)
        return when {
            currentCharacter.startsWith("luna") -> CharacterType.FEMALE
            currentCharacter.startsWith("max") -> CharacterType.MALE
            else -> CharacterType.MALE
        }
    }

    // ========== ACTIVITY AND MOOD TRACKING (User-Specific) ==========

    fun getLastActiveDate(context: Context): Date {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastActiveMillis = prefs.getLong(getUserKey(userId, "last_active_date"), Date().time)
        return Date(lastActiveMillis)
    }

    fun updateLastActiveDate(context: Context) {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(getUserKey(userId, "last_active_date"), Date().time).apply()
    }

    fun getCurrentMood(context: Context): Mood {
        val lastActiveDate = getLastActiveDate(context)
        return if (CharacterUtils.shouldCharacterBeSad(lastActiveDate)) {
            Mood.SAD
        } else {
            Mood.HAPPY
        }
    }

    fun updateCharacterMood(context: Context): Mood {
        updateLastActiveDate(context)
        return getCurrentMood(context)
    }

    // ========== CHARACTER INSTANCE CREATION ==========

    fun createUserCharacter(context: Context, accessories: List<CharacterAccessory> = emptyList()): Character {
        return Character(
            type = getCharacterType(context),
            mood = getCurrentMood(context),
            accessories = accessories,
            equippedAccessories = emptyList(),
            lastActiveDate = getLastActiveDate(context)
        )
    }

    // ========== COIN MANAGEMENT (User-Specific) ==========

    fun getCoins(context: Context): Int {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(getUserKey(userId, "coins"), 100)
    }

    fun addCoins(context: Context, amount: Int) {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCoins = getCoins(context)
        prefs.edit().putInt(getUserKey(userId, "coins"), currentCoins + amount).apply()
    }

    fun spendCoins(context: Context, amount: Int): Boolean {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCoins = getCoins(context)
        if (currentCoins >= amount) {
            prefs.edit().putInt(getUserKey(userId, "coins"), currentCoins - amount).apply()
            return true
        }
        return false
    }

    fun setCoins(context: Context, amount: Int) {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(getUserKey(userId, "coins"), amount).apply()
    }

    // ========== PURCHASE TRACKING (User-Specific) ==========

    fun isPurchased(context: Context, accessoryId: String): Boolean {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(getUserKey(userId, "purchased_$accessoryId"), false)
    }

    fun setPurchased(context: Context, accessoryId: String, purchased: Boolean) {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(getUserKey(userId, "purchased_$accessoryId"), purchased).apply()
    }

    // ========== TUTORIAL COMPLETION (User-Specific) ==========

    fun setTutorialCompleted(context: Context, completed: Boolean = true) {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(getUserKey(userId, "tutorial_completed"), completed).apply()
    }

    fun isTutorialCompleted(context: Context): Boolean {
        val userId = getCurrentUserId(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(getUserKey(userId, "tutorial_completed"), false)
    }

    // ========== UTILITY FUNCTIONS ==========

    /**
     * Clear all character data for a specific user (useful for logout/account deletion)
     */
    fun clearUserData(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Remove all user-specific keys
        val keysToRemove = listOf(
            "selected_character",
            "equipped_character",
            "last_active_date",
            "coins",
            "tutorial_completed"
        )

        keysToRemove.forEach { key ->
            editor.remove(getUserKey(userId, key))
        }

        // Remove purchased accessories
        val allKeys = prefs.all.keys
        allKeys.forEach { key ->
            if (key.startsWith("${userId}_purchased_")) {
                editor.remove(key)
            }
        }

        editor.apply()
    }
}