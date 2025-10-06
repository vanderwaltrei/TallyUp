@file:Suppress("PackageName")

package za.ac.iie.TallyUp.models

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    var hasCompletedTutorial: Boolean = false,
    val character: Character,
    var coins: Int = 100
)