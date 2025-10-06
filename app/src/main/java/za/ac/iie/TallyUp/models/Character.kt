@file:Suppress("PackageName")

package za.ac.iie.TallyUp.models

import java.util.Date

data class Character(
    val type: CharacterType,
    var mood: Mood = Mood.HAPPY,
    val accessories: List<CharacterAccessory> = emptyList(),
    val equippedAccessories: List<String> = emptyList(),
    val lastActiveDate: Date = Date()
)