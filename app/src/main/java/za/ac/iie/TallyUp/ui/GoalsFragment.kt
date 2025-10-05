package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.databinding.FragmentGoalsBinding
import za.ac.iie.TallyUp.model.Goal
import za.ac.iie.TallyUp.model.GoalDatabase

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var goalAdapter: GoalAdapter
    private lateinit var database: GoalDatabase
    private var goalsList = mutableListOf<Goal>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)

        // Initialize database
        database = GoalDatabase.getDatabase(requireContext())

        // RecyclerView setup
        goalAdapter = GoalAdapter(goalsList)
        binding.goalsGrid.adapter = goalAdapter
        binding.goalsGrid.layoutManager = LinearLayoutManager(requireContext())

        // Load goals from database
        loadGoalsFromDatabase()

        // ---- Initial visibility ----
        updateEmptyState()

        // ---- Buttons ----
        binding.createFirstGoalButton.setOnClickListener { openCreateGoalPage() }
        binding.addGoalButton.setOnClickListener { openCreateGoalPage(hideGoals = true) }
        binding.backButton.setOnClickListener { handleBackButton() }
        binding.createGoalSubmitButton.setOnClickListener { saveNewGoal() }

        return binding.root
    }

    private fun openCreateGoalPage(hideGoals: Boolean = false) {
        binding.emptyState.visibility = View.GONE
        if (hideGoals) binding.goalsGrid.visibility = View.GONE
        binding.createGoalPage.visibility = View.VISIBLE
    }

    private fun handleBackButton() {
        if (binding.createGoalPage.visibility == View.VISIBLE) {
            binding.createGoalPage.visibility = View.GONE
            binding.emptyState.visibility = if (goalsList.isEmpty()) View.VISIBLE else View.GONE
            binding.goalsGrid.visibility = if (goalsList.isEmpty()) View.GONE else View.VISIBLE
        } else {
            val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.selectedItemId = R.id.navigation_home
        }
    }

    private fun updateEmptyState() {
        if (goalsList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.goalsGrid.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.goalsGrid.visibility = View.VISIBLE
        }
    }

    private fun loadGoalsFromDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val goalsFromDb = database.goalDao().getAllGoals()
            goalsList.clear()
            goalsList.addAll(goalsFromDb)
            withContext(Dispatchers.Main) {
                goalAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }
    }

    private fun saveNewGoal() {
        val nameInput = binding.goalNameInput.text.toString().trim()
        val amountInput = binding.goalAmountInput.text.toString().trim()
        val deadlineInput = binding.goalDeadlineInput.text.toString().trim()

        if (nameInput.isEmpty() || amountInput.isEmpty() || deadlineInput.isEmpty()) return

        val newGoal = Goal(
            name = nameInput,
            description = "", // optional
            target = amountInput.toDoubleOrNull() ?: 0.0,
            current = 0.0,
            deadline = deadlineInput
        )

        CoroutineScope(Dispatchers.IO).launch {
            database.goalDao().insertGoal(newGoal)
            val updatedGoals = database.goalDao().getAllGoals()
            withContext(Dispatchers.Main) {
                goalsList.clear()
                goalsList.addAll(updatedGoals)
                goalAdapter.notifyDataSetChanged()
                binding.createGoalPage.visibility = View.GONE
                binding.goalsGrid.visibility = View.VISIBLE
                updateEmptyState()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}