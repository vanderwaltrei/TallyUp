@file:Suppress("PackageName")

package za.ac.iie.TallyUp.models

data class CharacterAccessory(
    val id: String,
    val name: String,
    val type: AccessoryType,
    var unlocked: Boolean = false,
    val rarity: Rarity,
    val cost: Int
)