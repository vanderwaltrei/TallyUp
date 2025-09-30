package za.ac.iie.TallyUp.utils

import za.ac.iie.TallyUp.model.Character
import java.util.*

object CharacterUtils {

    fun shouldCharacterBeSad(lastActiveDate: Date): Boolean {
        val daysSinceLastActive = (Date().time - lastActiveDate.time) / (1000 * 3600 * 24)
        return daysSinceLastActive >= 3
    }

    fun updateCharacterMood(character: Character, lastActiveDate: Date? = null): Character {
        val checkDate = lastActiveDate ?: character.lastActiveDate
        val shouldBeSad = shouldCharacterBeSad(checkDate)

        return character.copy(
            mood = if (shouldBeSad) za.ac.iie.TallyUp.model.Mood.SAD else za.ac.iie.TallyUp.model.Mood.HAPPY,
            lastActiveDate = Date()
        )
    }

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
}