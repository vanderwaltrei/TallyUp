package za.ac.iie.TallyUp.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.databinding.FragmentDashboardBinding
import za.ac.iie.TallyUp.models.AppState
import za.ac.iie.TallyUp.data.AppRepository
import za.ac.iie.TallyUp.ui.BudgetDashboardFragment
import za.ac.iie.TallyUp.ui.insights.InsightsFragment
import za.ac.iie.TallyUp.utils.CharacterManager
import za.ac.iie.TallyUp.model.Goal
import za.ac.iie.TallyUp.model.GoalDatabase

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private lateinit var appState: AppState
    private lateinit var goalDatabase: GoalDatabase
    private var goalsList = mutableListOf<Goal>()
    private lateinit var goalAdapter: GoalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        repository = AppRepository(requireContext())
        goalDatabase = GoalDatabase.getDatabase(requireContext())
        appState = repository.loadAppState()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGoalRecyclerView()
        setupUI()
        setupQuickActions()
        loadGoalsFromDatabase()
    }

    private fun setupGoalRecyclerView() {
        goalAdapter = GoalAdapter(
            goalsList,
            onAddMoneyClicked = { goal ->
                // Navigate to GoalsFragment to add money
                navigateToGoalsFragment()
            },
            onCompleteGoalClicked = { goal ->
                // Navigate to GoalsFragment to complete goal
                navigateToGoalsFragment()
            }
        )

        binding.goalsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.goalsRecyclerView.adapter = goalAdapter
    }

    private fun loadGoalsFromDatabase() {
        val userId = getCurrentUserId()
        CoroutineScope(Dispatchers.IO).launch {
            val goalsFromDb = goalDatabase.goalDao().getGoalsByUser(userId)
            goalsList.clear()
            goalsList.addAll(goalsFromDb.take(2))

            withContext(Dispatchers.Main) {
                goalAdapter.notifyDataSetChanged()
                updateGoalsVisibility()
            }
        }
    }

    // Add this method to DashboardFragment
    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        return prefs.getString("loggedInEmail", "") ?: "default"
    }

    private fun updateGoalsVisibility() {
        if (goalsList.isEmpty()) {
            binding.noGoalsText.visibility = View.VISIBLE
            binding.goalsRecyclerView.visibility = View.GONE
        } else {
            binding.noGoalsText.visibility = View.GONE
            binding.goalsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupQuickActions() {
        binding.budgetDashboardCard.setOnClickListener {
            navigateToBudgetDashboard()
        }

        binding.insightsCard.setOnClickListener {
            navigateToInsights()
        }

        // Add click listener to goals section to navigate to full goals page
        binding.goalsSection.setOnClickListener {
            navigateToGoalsFragment()
        }
    }

    private fun navigateToGoalsFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GoalsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToInsights() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, InsightsComingSoonFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToBudgetDashboard() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, BudgetDashboardFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun setupUI() {
        // Welcome message with character name
        val characterName = CharacterManager.getCharacterName(requireContext())
        val firstName = appState.user?.firstName ?: "there"
        binding.welcomeText.text = "Hey $firstName! Say hi to $characterName!"

        // Available to spend
        val totalBudget = appState.budgetCategories.sumOf { it.budgeted }
        val totalSpent = appState.budgetCategories.sumOf { it.spent }
        val availableToSpend = totalBudget - totalSpent

        binding.availableAmount.text = "R${"%.2f".format(availableToSpend)}"
        binding.availableSubtitle.text = "Available to Spend"

        // Progress circle (simplified)
        val spentPercentage =
            if (totalBudget > 0.0) ((totalSpent / totalBudget) * 100.0).toInt() else 0
        binding.progressText.text = "${spentPercentage}%"

        // Status indicator
        val statusText = when {
            spentPercentage < 60 -> "On Track!"
            spentPercentage < 80 -> "Watch It!"
            else -> "Almost There!"
        }
        binding.statusText.text = statusText

        // Recent transactions
        val recentTransactions = appState.transactions.take(3)
        binding.recentSection.visibility = if (recentTransactions.isEmpty()) View.GONE else View.VISIBLE

        // Character display using CharacterManager
        val characterDrawable = CharacterManager.getCharacterDrawable(requireContext())
        binding.characterImage.setImageResource(characterDrawable)

        // Mood indicator
        val mood = CharacterManager.getCurrentMood(requireContext())
        binding.moodIndicator.visibility = if (mood == za.ac.iie.TallyUp.models.Mood.SAD) View.VISIBLE else View.GONE

        // Coins display
        val coins = CharacterManager.getCoins(requireContext())
        binding.coinsText.text = coins.toString()
    }

    override fun onResume() {
        super.onResume()
        // Refresh goals when returning to dashboard
        loadGoalsFromDatabase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}