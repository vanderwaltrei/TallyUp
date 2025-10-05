package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import za.ac.iie.TallyUp.R

class Question2TutorialFragment : Fragment(R.layout.fragment_question2_tutorial) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val savingGoalInput = view.findViewById<TextInputEditText>(R.id.saving_goal_input)
        val startSavingButton = view.findViewById<Button>(R.id.start_saving_button)

        // Enable button only when text is entered
        savingGoalInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrBlank()
                startSavingButton.isEnabled = hasText
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Start Saving button click listener
        startSavingButton.setOnClickListener {
            val savingGoal = savingGoalInput.text.toString().trim()
            if (savingGoal.isNotEmpty()) {
                saveSavingGoal(savingGoal)
                navigateToDashboard()
            }
        }
    }

    private fun saveSavingGoal(goal: String) {
        // Save to SharedPreferences for later use
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("saving_goal", goal)
        editor.apply()
    }

    private fun navigateToDashboard() {
        // Navigate to main dashboard
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DashboardFragment())
            .addToBackStack("question2_to_dashboard")
            .commit()
    }
}