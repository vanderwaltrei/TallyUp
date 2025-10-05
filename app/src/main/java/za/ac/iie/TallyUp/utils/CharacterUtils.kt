package za.ac.iie.TallyUp.utils

import za.ac.iie.TallyUp.models.Character
import za.ac.iie.TallyUp.models.Mood
import za.ac.iie.TallyUp.models.CharacterAccessory
import za.ac.iie.TallyUp.models.AccessoryType  // <-- ADD THIS IMPORT
import java.util.*

object CharacterUtils {

    /**
     * Check if character should be sad based on last active date
     * Character becomes sad after 3 days of inactivity
     */
    fun shouldCharacterBeSad(lastActiveDate: Date): Boolean {
        val daysSinceLastActive = (Date().time - lastActiveDate.time) / (1000 * 3600 * 24)
        return daysSinceLastActive >= 3
    }

    /**
     * Update character mood based on last activity
     */
    fun updateCharacterMood(character: Character, lastActiveDate: Date? = null): Character {
        val checkDate = lastActiveDate ?: character.lastActiveDate
        val shouldBeSad = shouldCharacterBeSad(checkDate)

        return character.copy(
            mood = if (shouldBeSad) Mood.SAD else Mood.HAPPY,
            lastActiveDate = Date()
        )
    }

    /**
     * Unlock an accessory for the character
     */
    fun unlockAccessory(character: Character, accessoryId: String): Character {
        val updatedAccessories = character.accessories.map { accessory ->
            if (accessory.id == accessoryId) {
                accessory.copy(unlocked = true)
            } else {
                accessory
            }
        }

        return character.copy(accessories = updatedAccessories)
    }

    /**
     * Equip an accessory (only one accessory per type can be equipped)
     */
    fun equipAccessory(character: Character, accessoryId: String): Character {
        val accessory = character.accessories.find { it.id == accessoryId }
        if (accessory == null || !accessory.unlocked) return character

        val filteredEquipped = character.equippedAccessories.filter { equippedId ->
            val equippedAcc = character.accessories.find { it.id == equippedId }
            equippedAcc?.type != accessory.type
        }

        return character.copy(
            equippedAccessories = filteredEquipped + accessoryId
        )
    }

    /**
     * Unequip an accessory
     */
    fun unequipAccessory(character: Character, accessoryId: String): Character {
        return character.copy(
            equippedAccessories = character.equippedAccessories.filter { it != accessoryId }
        )
    }

    /**
     * Check if an accessory is equipped
     */
    fun isAccessoryEquipped(character: Character, accessoryId: String): Boolean {
        return character.equippedAccessories.contains(accessoryId)
    }

    /**
     * Get all equipped accessories
     */
    fun getEquippedAccessories(character: Character): List<CharacterAccessory> {
        return character.equippedAccessories.mapNotNull { accessoryId ->
            character.accessories.find { it.id == accessoryId }
        }
    }

    /**
     * Get accessories by type
     */
    fun getAccessoriesByType(character: Character, type: AccessoryType): List<CharacterAccessory> {
        return character.accessories.filter { it.type == type }
    }

    /**
     * Get unlocked accessories
     */
    fun getUnlockedAccessories(character: Character): List<CharacterAccessory> {
        return character.accessories.filter { it.unlocked }
    }

    /**
     * Get locked accessories
     */
    fun getLockedAccessories(character: Character): List<CharacterAccessory> {
        return character.accessories.filter { !it.unlocked }
    }

    /**
     * Calculate character happiness score (0-100)
     */
    fun calculateHappinessScore(character: Character): Int {
        val baseScore = if (character.mood == Mood.HAPPY) 80 else 30
        val accessoryBonus = character.equippedAccessories.size * 5
        return (baseScore + accessoryBonus).coerceAtMost(100)
    }

    /**
     * Check if character needs attention (sad mood)
     */
    fun needsAttention(character: Character): Boolean {
        return character.mood == Mood.SAD
    }

    /**
     * Reward character for good behavior (completing goals, etc.)
     */
    fun rewardCharacter(character: Character, coinsToAdd: Int = 10): Character {
        // Update last active date to keep character happy
        return character.copy(
            lastActiveDate = Date(),
            mood = Mood.HAPPY
        )
    }
}