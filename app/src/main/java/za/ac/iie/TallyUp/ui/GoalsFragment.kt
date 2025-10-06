@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
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
import za.ac.iie.TallyUp.models.Goal
import za.ac.iie.TallyUp.models.GoalDatabase

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var goalAdapter: GoalAdapter
    private lateinit var database: GoalDatabase
    private var goalsList = mutableListOf<Goal>()
    private var currentGoalForAddingMoney: Goal? = null

    // Add method to get current user ID
    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        return prefs.getString("loggedInEmail", "") ?: "default"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)

        // Initialize database
        database = GoalDatabase.getDatabase(requireContext())

        // RecyclerView setup
        goalAdapter = GoalAdapter(
            goalsList,
            onAddMoneyClicked = { goal -> openAddMoneyMenu(goal) },
            onCompleteGoalClicked = { goal -> completeGoal(goal) }
        )
        binding.goalsGrid.adapter = goalAdapter
        binding.goalsGrid.layoutManager = LinearLayoutManager(requireContext())

        // Load goals from database for current user
        loadGoalsFromDatabase()

        // Initial visibility
        updateEmptyState()

        // Buttons
        binding.createFirstGoalButton.setOnClickListener { openCreateGoalPage() }
        binding.addGoalButton.setOnClickListener { openCreateGoalPage(hideGoals = true) }
        binding.backButton.setOnClickListener { handleBackButton() }
        binding.createGoalSubmitButton.setOnClickListener { saveNewGoal() }
        binding.goalBack.setOnClickListener { closeAddMoneyMenu() }
        binding.btnAddMoney.setOnClickListener { addMoneyToGoal() }

        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadGoalsFromDatabase() {
        val userId = getCurrentUserId()
        CoroutineScope(Dispatchers.IO).launch {
            val goalsFromDb = database.goalDao().getGoalsByUser(userId)
            goalsList.clear()
            goalsList.addAll(goalsFromDb)
            withContext(Dispatchers.Main) {
                goalAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun saveNewGoal() {
        val nameInput = binding.goalNameInput.text.toString().trim()
        val amountInput = binding.goalAmountInput.text.toString().trim()
        val deadlineInput = binding.goalDeadlineInput.text.toString().trim()

        if (nameInput.isEmpty() || amountInput.isEmpty() || deadlineInput.isEmpty()) return

        val newGoal = Goal(
            name = nameInput,
            description = "",
            target = amountInput.toDouble(),
            current = 0.0,
            deadline = deadlineInput,
            userId = getCurrentUserId() // Add user ID
        )

        CoroutineScope(Dispatchers.IO).launch {
            database.goalDao().insertGoal(newGoal)
            val updatedGoals = database.goalDao().getGoalsByUser(getCurrentUserId())
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

    private fun openCreateGoalPage(hideGoals: Boolean = false) {
        binding.emptyState.visibility = View.GONE
        if (hideGoals) binding.goalsGrid.visibility = View.GONE
        binding.createGoalPage.visibility = View.VISIBLE
    }

    @SuppressLint("UseKtx")
    private fun handleBackButton() {
        if (binding.createGoalPage.visibility == View.VISIBLE) {
            binding.createGoalPage.visibility = View.GONE
            updateEmptyState()
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

    @SuppressLint("SetTextI18n")
    private fun openAddMoneyMenu(goal: Goal) {
        currentGoalForAddingMoney = goal
        binding.addMoney.visibility = View.VISIBLE
        binding.goalsGrid.visibility = View.GONE
        binding.tvAddMoney.text = "Add Money To ${goal.name}"
        binding.goalAdd.text?.clear()
    }

    private fun closeAddMoneyMenu() {
        binding.addMoney.visibility = View.GONE
        binding.goalsGrid.visibility = View.VISIBLE
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addMoneyToGoal() {
        val goal = currentGoalForAddingMoney ?: return
        val inputText = binding.goalAdd.text.toString().replace("R", "").trim()
        val amount = inputText.toDoubleOrNull() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            goal.current += amount
            database.goalDao().updateGoal(goal)
            val updatedGoals = database.goalDao().getGoalsByUser(getCurrentUserId())
            withContext(Dispatchers.Main) {
                goalsList.clear()
                goalsList.addAll(updatedGoals)
                goalAdapter.notifyDataSetChanged()
                closeAddMoneyMenu()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun completeGoal(goal: Goal) {
        CoroutineScope(Dispatchers.IO).launch {
            database.goalDao().deleteGoal(goal)
            val updatedGoals = database.goalDao().getGoalsByUser(getCurrentUserId())
            withContext(Dispatchers.Main) {
                goalsList.clear()
                goalsList.addAll(updatedGoals)
                goalAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}