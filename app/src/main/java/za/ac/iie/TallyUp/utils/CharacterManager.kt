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
    private const val SELECTED_CHARACTER_KEY = "selected_character"
    private const val EQUIPPED_CHARACTER_KEY = "equipped_character"  // NEW: For equipped accessories
    private const val LAST_ACTIVE_DATE_KEY = "last_active_date"
    private const val USER_COINS_KEY = "user_coins"

    // Default to Max if no selection made
    private const val DEFAULT_CHARACTER = "max"

    // Character selection methods (base character: max or luna)
    fun saveSelectedCharacter(context: Context, character: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(SELECTED_CHARACTER_KEY, character)
            putLong(LAST_ACTIVE_DATE_KEY, Date().time)
            apply()
        }
    }

    fun getSelectedCharacter(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(SELECTED_CHARACTER_KEY, DEFAULT_CHARACTER) ?: DEFAULT_CHARACTER
    }

    // NEW: Equipped character methods (for shop accessories)
    fun saveEquippedCharacter(context: Context, characterId: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(EQUIPPED_CHARACTER_KEY, characterId)
            apply()
        }
    }

    fun getEquippedCharacter(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(EQUIPPED_CHARACTER_KEY, null)
    }

    // NEW: Get current active character (equipped takes priority)
    fun getCurrentCharacterId(context: Context): String {
        val equippedCharacter = getEquippedCharacter(context)
        return equippedCharacter ?: getSelectedCharacter(context)
    }

    // Drawable and display methods - UPDATED to use equipped character
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

            else -> R.drawable.character_happy // Default fallback
        }
    }

    fun getCharacterName(context: Context): String {
        val currentCharacter = getCurrentCharacterId(context)

        return when (currentCharacter) {
            // Base characters
            "luna" -> "Luna"
            "max" -> "Max"

            // Shop accessories with custom names
            "luna_gamer" -> "Gamer Luna"
            "luna_goddess" -> "Light Luna"
            "luna_gothic" -> "Gothic Luna"
            "luna_strawberry" -> "Strawberry Luna"
            "max_light" -> "Light Max"
            "max_villain" -> "Villain Max"

            else -> "Max" // Default fallback
        }
    }

    fun getCharacterType(context: Context): CharacterType {
        val currentCharacter = getCurrentCharacterId(context)

        // Determine if character is Luna or Max variant
        return when {
            currentCharacter.startsWith("luna") -> CharacterType.FEMALE
            currentCharacter.startsWith("max") -> CharacterType.MALE
            else -> CharacterType.MALE // Default fallback
        }
    }

    // Activity and mood tracking methods
    fun getLastActiveDate(context: Context): Date {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastActiveMillis = prefs.getLong(LAST_ACTIVE_DATE_KEY, Date().time)
        return Date(lastActiveMillis)
    }

    fun updateLastActiveDate(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(LAST_ACTIVE_DATE_KEY, Date().time).apply()
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

    // Character instance creation
    fun createUserCharacter(context: Context, accessories: List<CharacterAccessory> = emptyList()): Character {
        return Character(
            type = getCharacterType(context),
            mood = getCurrentMood(context),
            accessories = accessories,
            equippedAccessories = emptyList(),
            lastActiveDate = getLastActiveDate(context)
        )
    }

    // Coin management methods
    fun getCoins(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(USER_COINS_KEY, 100) // Default 100 coins
    }

    fun addCoins(context: Context, amount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCoins = getCoins(context)
        prefs.edit().putInt(USER_COINS_KEY, currentCoins + amount).apply()
    }

    fun spendCoins(context: Context, amount: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCoins = getCoins(context)
        if (currentCoins >= amount) {
            prefs.edit().putInt(USER_COINS_KEY, currentCoins - amount).apply()
            return true
        }
        return false
    }

    fun setCoins(context: Context, amount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(USER_COINS_KEY, amount).apply()
    }

    // NEW: Purchase tracking
    fun isPurchased(context: Context, accessoryId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("purchased_$accessoryId", false)
    }

    fun setPurchased(context: Context, accessoryId: String, purchased: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("purchased_$accessoryId", purchased).apply()
    }

    // Tutorial completion tracking
    fun setTutorialCompleted(context: Context, completed: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("tutorial_completed", completed).apply()
    }

    fun isTutorialCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("tutorial_completed", false)
    }
}