@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.utils.CharacterManager

class ChooseCharacterTutorialFragment : Fragment(R.layout.fragment_choose_character_tutorial) {

    private var selectedCharacter: String = "max" // Default selection

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val characterLuna = view.findViewById<MaterialCardView>(R.id.character_luna)
        val characterMax = view.findViewById<MaterialCardView>(R.id.character_max)
        val continueButton = view.findViewById<Button>(R.id.continue_button)

        // Character click listeners
        characterLuna.setOnClickListener {
            selectCharacter("luna", characterLuna, characterMax)
            updateContinueButtonText("luna")
        }

        characterMax.setOnClickListener {
            selectCharacter("max", characterLuna, characterMax)
            updateContinueButtonText("max")
        }

        // Continue button click listener
        continueButton.setOnClickListener {
            saveSelectedCharacter()
            markTutorialCompleted()
            navigateToDashboard()
        }
    }

    private fun selectCharacter(
        character: String,
        lunaCard: MaterialCardView,
        maxCard: MaterialCardView
    ) {
        selectedCharacter = character

        // Reset both cards
        lunaCard.setCardBackgroundColor(requireContext().getColor(R.color.card_background))
        lunaCard.strokeColor = requireContext().getColor(R.color.muted_foreground)
        lunaCard.findViewById<View>(R.id.check_luna).visibility = View.INVISIBLE

        maxCard.setCardBackgroundColor(requireContext().getColor(R.color.card_background))
        maxCard.strokeColor = requireContext().getColor(R.color.muted_foreground)
        maxCard.findViewById<View>(R.id.check_max).visibility = View.INVISIBLE

        // Highlight selected card
        when (character) {
            "luna" -> {
                lunaCard.setCardBackgroundColor(requireContext().getColor(R.color.accent_light))
                lunaCard.strokeColor = requireContext().getColor(R.color.accent)
                lunaCard.findViewById<View>(R.id.check_luna).visibility = View.VISIBLE
            }
            "max" -> {
                maxCard.setCardBackgroundColor(requireContext().getColor(R.color.accent_light))
                maxCard.strokeColor = requireContext().getColor(R.color.accent)
                maxCard.findViewById<View>(R.id.check_max).visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateContinueButtonText(character: String) {
        val continueButton = view?.findViewById<Button>(R.id.continue_button)
        when (character) {
            "luna" -> continueButton?.text = "Continue with Luna"
            "max" -> continueButton?.text = "Continue with Max"
        }
    }

    private fun saveSelectedCharacter() {
        // Use the CharacterManager to save the selection
        CharacterManager.saveSelectedCharacter(requireContext(), selectedCharacter)
    }

    private fun markTutorialCompleted() {
        CharacterManager.setTutorialCompleted(requireContext(), true)
    }

    private fun navigateToDashboard() {
        // Navigate to main dashboard
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DashboardFragment())
            .addToBackStack("character_to_dashboard")
            .commit()
    }
}