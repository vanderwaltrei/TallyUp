package za.ac.iie.TallyUp.utils

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.models.Mood

// Context extensions
fun Context.getUserCharacterDrawable(): Int {
    return CharacterManager.getCharacterDrawable(this)
}

fun Context.getUserCharacterName(): String {
    return CharacterManager.getCharacterName(this)
}

fun Context.getUserCharacterMood(): Mood {
    return CharacterManager.getCurrentMood(this)
}

fun Context.getUserCoins(): Int {
    return CharacterManager.getCoins(this)
}

fun Context.addUserCoins(amount: Int) {
    CharacterManager.addCoins(this, amount)
}

// Fragment extensions
fun Fragment.getUserCharacterDrawable(): Int {
    return requireContext().getUserCharacterDrawable()
}

fun Fragment.getUserCharacterName(): String {
    return requireContext().getUserCharacterName()
}

fun Fragment.getUserCharacterMood(): Mood {
    return requireContext().getUserCharacterMood()
}

fun Fragment.getUserCoins(): Int {
    return requireContext().getUserCoins()
}

fun Fragment.addUserCoins(amount: Int) {
    requireContext().addUserCoins(amount)
}

// ImageView extension for easy character setup
fun ImageView.setUserCharacter(context: Context) {
    val characterDrawable = CharacterManager.getCharacterDrawable(context)
    this.setImageResource(characterDrawable)
}

// TextView extension for showing coins
fun TextView.showUserCoins(context: Context) {
    val coins = CharacterManager.getCoins(context)
    this.text = coins.toString()
}

// Mood display helper
fun getMoodDrawable(mood: Mood): Int {
    return when (mood) {
        Mood.HAPPY -> R.drawable.character_happy
        Mood.SAD -> R.drawable.character_happy // You might want to create a sad version
        else -> R.drawable.character_female
    }
}

fun getMoodDescription(mood: Mood): String {
    return when (mood) {
        Mood.HAPPY -> "Happy"
        Mood.SAD -> "Needs attention"
        else -> "Happy"
    }
}