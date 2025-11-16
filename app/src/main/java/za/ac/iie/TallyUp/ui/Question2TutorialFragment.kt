@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.models.Goal
import za.ac.iie.TallyUp.models.GoalDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class Question2TutorialFragment : Fragment(R.layout.fragment_question2_tutorial) {

    private lateinit var goalDatabase: GoalDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goalDatabase = GoalDatabase.getDatabase(requireContext())
    }

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
                createGoalFromInput(savingGoal)
            }
        }
    }

    @SuppressLint("UseKtx")
    private fun saveSavingGoal(goal: String) {
        // Save to SharedPreferences for later use
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("saving_goal", goal)
        editor.apply()
    }

    private fun createGoalFromInput(goalName: String) {
        val userId = getCurrentUserId()

        // Create a new goal with default values
        val newGoal = Goal(
            id = UUID.randomUUID().toString(),
            name = goalName,
            description = "My savings goal",
            target = 1000.0, // Default target amount
            current = 0.0,
            deadline = "3 Months", // Default deadline
            createdAt = getCurrentDate(),
            userId = userId
        )

        // Save the goal to database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                goalDatabase.goalDao().insertGoal(newGoal)

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Goal '$goalName' created!", Toast.LENGTH_SHORT).show()

                    // Navigate to Character Selection
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ChooseCharacterTutorialFragment())
                        .addToBackStack("question2_to_character")
                        .commit()
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Error creating goal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", "") ?: "default"
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
}