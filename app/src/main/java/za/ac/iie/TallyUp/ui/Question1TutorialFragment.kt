package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import za.ac.iie.TallyUp.R

class Question1TutorialFragment : Fragment(R.layout.fragment_question1_tutorial) {

    private var selectedOption: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val optionWeekly = view.findViewById<MaterialCardView>(R.id.option_weekly)
        val optionBiweekly = view.findViewById<MaterialCardView>(R.id.option_biweekly)
        val optionMonthly = view.findViewById<MaterialCardView>(R.id.option_monthly)
        val nextButton = view.findViewById<Button>(R.id.next_button)

        // Option click listeners
        optionWeekly.setOnClickListener {
            selectOption("weekly", optionWeekly, optionBiweekly, optionMonthly)
            nextButton.isEnabled = true
        }

        optionBiweekly.setOnClickListener {
            selectOption("biweekly", optionWeekly, optionBiweekly, optionMonthly)
            nextButton.isEnabled = true
        }

        optionMonthly.setOnClickListener {
            selectOption("monthly", optionWeekly, optionBiweekly, optionMonthly)
            nextButton.isEnabled = true
        }

        // Next button click listener - UPDATED to go to Question 2
        nextButton.setOnClickListener {
            // Save the selected option
            saveSelectedOption()

            // Navigate to Question 2 instead of Dashboard
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Question2TutorialFragment())
                .addToBackStack("question1_to_question2")
                .commit()
        }
    }

    private fun selectOption(
        option: String,
        weeklyCard: MaterialCardView,
        biweeklyCard: MaterialCardView,
        monthlyCard: MaterialCardView
    ) {
        selectedOption = option

        // Reset all cards
        weeklyCard.setCardBackgroundColor(requireContext().getColor(R.color.card_background))
        weeklyCard.findViewById<View>(R.id.check_weekly).visibility = View.INVISIBLE

        biweeklyCard.setCardBackgroundColor(requireContext().getColor(R.color.card_background))
        biweeklyCard.findViewById<View>(R.id.check_biweekly).visibility = View.INVISIBLE

        monthlyCard.setCardBackgroundColor(requireContext().getColor(R.color.card_background))
        monthlyCard.findViewById<View>(R.id.check_monthly).visibility = View.INVISIBLE

        // Highlight selected card
        when (option) {
            "weekly" -> {
                weeklyCard.setCardBackgroundColor(requireContext().getColor(R.color.accent_light))
                weeklyCard.findViewById<View>(R.id.check_weekly).visibility = View.VISIBLE
            }
            "biweekly" -> {
                biweeklyCard.setCardBackgroundColor(requireContext().getColor(R.color.accent_light))
                biweeklyCard.findViewById<View>(R.id.check_biweekly).visibility = View.VISIBLE
            }
            "monthly" -> {
                monthlyCard.setCardBackgroundColor(requireContext().getColor(R.color.accent_light))
                monthlyCard.findViewById<View>(R.id.check_monthly).visibility = View.VISIBLE
            }
        }
    }

    private fun saveSelectedOption() {
        // Save to SharedPreferences or ViewModel for later use
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("income_frequency", selectedOption)
        editor.apply()
    }
}