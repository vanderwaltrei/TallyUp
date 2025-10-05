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
    private const val LAST_ACTIVE_DATE_KEY = "last_active_date"
    private const val USER_COINS_KEY = "user_coins"

    // Default to Max if no selection made
    private const val DEFAULT_CHARACTER = "max"

    // Character selection methods
    fun saveSelectedCharacter(context: Context, character: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(SELECTED_CHARACTER_KEY, character)
            putLong(LAST_ACTIVE_DATE_KEY, Date().time) // Update last active date
            apply()
        }
    }

    fun getSelectedCharacter(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(SELECTED_CHARACTER_KEY, DEFAULT_CHARACTER) ?: DEFAULT_CHARACTER
    }

    // Drawable and display methods
    fun getCharacterDrawable(context: Context): Int {
        return when (getSelectedCharacter(context)) {
            "luna" -> R.drawable.character_female
            "max" -> R.drawable.character_happy
            else -> R.drawable.character_happy // Default fallback
        }
    }

    fun getCharacterName(context: Context): String {
        return when (getSelectedCharacter(context)) {
            "luna" -> "Luna"
            "max" -> "Max"
            else -> "Max" // Default fallback
        }
    }

    fun getCharacterType(context: Context): CharacterType {
        return when (getSelectedCharacter(context)) {
            "luna" -> CharacterType.FEMALE
            "max" -> CharacterType.MALE
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