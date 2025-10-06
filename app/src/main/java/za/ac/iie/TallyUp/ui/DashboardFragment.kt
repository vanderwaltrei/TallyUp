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
import za.ac.iie.TallyUp.data.AppDatabase
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
    private lateinit var appDatabase: AppDatabase
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
        appDatabase = AppDatabase.getDatabase(requireContext())
        appState = repository.loadAppState()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGoalRecyclerView()
        setupUI()
        setupQuickActions()
        loadGoalsFromDatabase()
        loadTransactionsAndUpdateBudget()
        debugCheckTransactions() // ← ADD THIS LINE HERE
    }

    // ADD THIS DEBUG METHOD RIGHT AFTER onViewCreated method:
    private fun debugCheckTransactions() {
        val userId = getCurrentUserId()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transactions = appDatabase.transactionDao().getTransactionsForUser(userId)
                println("=== DEBUG: Found ${transactions.size} transactions ===")
                transactions.forEach { transaction ->
                    println("Transaction: ${transaction.type} - R${transaction.amount} - ${transaction.category} - User: ${transaction.userId}")
                }
                println("=================================")
            } catch (e: Exception) {
                println("DEBUG Error: ${e.message}")
            }
        }
    }

    private fun setupGoalRecyclerView() {
        goalAdapter = GoalAdapter(
            goalsList,
            onAddMoneyClicked = { goal ->
                navigateToGoalsFragment()
            },
            onCompleteGoalClicked = { goal ->
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

    private fun loadTransactionsAndUpdateBudget() {
        val userId = getCurrentUserId()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get all transactions for current user
                val transactions = appDatabase.transactionDao().getTransactionsForUser(userId)

                // Calculate total income and expenses
                val totalIncome = transactions
                    .filter { it.type == "Income" }
                    .sumOf { it.amount }

                val totalExpenses = transactions
                    .filter { it.type == "Expense" }
                    .sumOf { it.amount }

                // Get monthly budget from app state (total of budget categories)
                val monthlyBudget = appState.budgetCategories.sumOf { it.budgeted }

                // Calculate available to spend based on budget
                val availableToSpend = monthlyBudget - totalExpenses

                // Calculate progress percentage (how much of budget is spent)
                val progressPercentage = if (monthlyBudget > 0) {
                    ((totalExpenses / monthlyBudget) * 100).toInt().coerceIn(0, 100)
                } else {
                    0
                }

                withContext(Dispatchers.Main) {
                    updateBudgetUI(availableToSpend, totalExpenses, monthlyBudget, progressPercentage, totalIncome)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Show error state
                    binding.availableAmount.text = "R0.00"
                    binding.progressText.text = "0%"
                    binding.statusText.text = "Error loading data"
                }
            }
        }
    }

    private fun updateBudgetUI(
        availableToSpend: Double,
        totalSpent: Double,
        totalBudget: Double,
        progressPercentage: Int,
        totalIncome: Double
    ) {
        // Update available amount - show the actual calculated amount
        binding.availableAmount.text = "R${"%.2f".format(availableToSpend)}"
        binding.availableSubtitle.text = "Available to Spend"

        // Update progress text
        binding.progressText.text = "${progressPercentage}%"

        // Update status indicator based on spending
        val statusText = when {
            progressPercentage < 60 -> "On Track!"
            progressPercentage < 80 -> "Watch It!"
            else -> "Almost There!"
        }
        binding.statusText.text = statusText

        // Update status color based on spending level
        val context = requireContext()
        when {
            progressPercentage < 60 -> {
                binding.statusText.setBackgroundColor(context.getColor(R.color.success_light))
                binding.statusText.setTextColor(context.getColor(R.color.success))
            }
            progressPercentage < 80 -> {
                binding.statusText.setBackgroundColor(context.getColor(R.color.warning_light))
                binding.statusText.setTextColor(context.getColor(R.color.warning))
            }
            else -> {
                binding.statusText.setBackgroundColor(context.getColor(R.color.error))
                binding.statusText.setTextColor(context.getColor(R.color.error))
            }
        }

        // Debug logging
        println("=== BUDGET CALCULATION ===")
        println("Total Budget: R$totalBudget")
        println("Total Spent: R$totalSpent")
        println("Total Income: R$totalIncome")
        println("Available: R$availableToSpend")
        println("Progress: $progressPercentage%")
        println("==========================")
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

        // Recent transactions section visibility
        binding.recentSection.visibility = View.GONE

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
        // Refresh goals and transactions when returning to dashboard
        loadGoalsFromDatabase()
        loadTransactionsAndUpdateBudget()
        debugCheckTransactions() // ← You can also add it here to see updates
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}