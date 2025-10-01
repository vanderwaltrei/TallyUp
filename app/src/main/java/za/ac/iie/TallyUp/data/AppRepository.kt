package za.ac.iie.TallyUp.data

import android.content.Context
import za.ac.iie.TallyUp.models.*
import com.google.gson.Gson

class AppRepository(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences("TallyUp", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveAppState(appState: AppState) {
        val json = gson.toJson(appState)
        sharedPreferences.edit().putString("app_state", json).apply()
    }

    fun loadAppState(): AppState {
        val json = sharedPreferences.getString("app_state", null)
        return if (json != null) {
            gson.fromJson(json, AppState::class.java)
        } else {
            AppState()
        }
    }

    fun getDefaultAccessories(): List<CharacterAccessory> = listOf(
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

    fun getRewardAccessories(): List<CharacterAccessory> = listOf(
        CharacterAccessory(
            id = "graduation-cap",
            name = "Graduation Cap",
            type = AccessoryType.HAT,
            unlocked = false,
            rarity = Rarity.RARE,
            cost = 100
        ),
        CharacterAccessory(
            id = "business-suit",
            name = "Business Suit",
            type = AccessoryType.OUTFIT,
            unlocked = false,
            rarity = Rarity.EPIC,
            cost = 250
        ),
        CharacterAccessory(
            id = "party-hat",
            name = "Party Hat",
            type = AccessoryType.HAT,
            unlocked = false,
            rarity = Rarity.COMMON,
            cost = 50
        ),
        CharacterAccessory(
            id = "superhero-cape",
            name = "Superhero Cape",
            type = AccessoryType.ACCESSORY,
            unlocked = false,
            rarity = Rarity.LEGENDARY,
            cost = 500
        ),
        CharacterAccessory(
            id = "beach-background",
            name = "Beach Scene",
            type = AccessoryType.BACKGROUND,
            unlocked = false,
            rarity = Rarity.RARE,
            cost = 150
        ),
        CharacterAccessory(
            id = "winter-coat",
            name = "Winter Coat",
            type = AccessoryType.OUTFIT,
            unlocked = false,
            rarity = Rarity.COMMON,
            cost = 75
        )
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