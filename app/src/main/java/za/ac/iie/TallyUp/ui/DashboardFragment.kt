package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import za.ac.iie.TallyUp.utils.CharacterManager
import za.ac.iie.TallyUp.models.Goal
import za.ac.iie.TallyUp.models.GoalDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private lateinit var appState: AppState
    private lateinit var goalDatabase: GoalDatabase
    private lateinit var appDatabase: AppDatabase
    private var goalsList = mutableListOf<Goal>()
    private lateinit var goalAdapter: GoalAdapter

    companion object {
        private const val TAG = "DashboardFragment"
    }

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
        debugCheckTransactions()
        loadRecentTransactions()
    }

    private fun setupGoalRecyclerView() {
        goalAdapter = GoalAdapter(
            goalsList,
            onAddMoneyClicked = { goal ->
                navigateToGoalsFragment()
            },
            onCompleteGoalClicked = { goal ->
                navigateToGoalsFragment()
            },
            onEditGoalClicked = { goal ->
                // Navigate to goals fragment where they can edit
                navigateToGoalsFragment()
            },
            onDeleteGoalClicked = { goal ->
                // Navigate to goals fragment where they can delete
                navigateToGoalsFragment()
            }
        )

        binding.goalsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.goalsRecyclerView.adapter = goalAdapter
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    @SuppressLint("SetTextI18n")
    private fun loadRecentTransactions() {
        val userId = getCurrentUserId()
        val transactionDao = AppDatabase.getDatabase(requireContext()).transactionDao()

        lifecycleScope.launch {
            val recentTransactions = transactionDao.getTransactionsForUser(userId)
                .sortedByDescending { it.date }
                .take(3)

            if (recentTransactions.isNotEmpty()) {
                binding.noTransactionsText.visibility = View.GONE
                binding.recentTransactionsContainer.visibility = View.VISIBLE
                binding.recentTransactionsContainer.removeAllViews()

                recentTransactions.forEach { transaction ->
                    val itemView = layoutInflater.inflate(R.layout.item_transaction, binding.recentTransactionsContainer, false)

                    itemView.findViewById<TextView>(R.id.transaction_description).text =
                        "${transaction.category} - ${transaction.type}"

                    itemView.findViewById<TextView>(R.id.transaction_date).text =
                        formatDate(transaction.date)

                    itemView.findViewById<TextView>(R.id.transaction_amount).text =
                        "R ${"%.2f".format(transaction.amount)}"

                    itemView.findViewById<TextView>(R.id.photo_status).text =
                        if (transaction.photoUris.isNotEmpty()) "Photos attached" else "No photos"

                    binding.recentTransactionsContainer.addView(itemView)
                }
            } else {
                binding.noTransactionsText.visibility = View.VISIBLE
                binding.recentTransactionsContainer.visibility = View.GONE
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
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

    @SuppressLint("SetTextI18n")
    private fun loadTransactionsAndUpdateBudget() {
        val userId = getCurrentUserId()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transactions = appDatabase.transactionDao().getTransactionsForUser(userId)

                val totalIncome = transactions
                    .filter { it.type == "Income" }
                    .sumOf { it.amount }

                val totalExpenses = transactions
                    .filter { it.type == "Expense" }
                    .sumOf { it.amount }

                val categorySpending = calculateCategorySpending(transactions)

                val monthlyBudget = appState.budgetCategories.sumOf { it.budgeted }

                val availableToSpend = monthlyBudget - totalExpenses

                val progressPercentage = if (monthlyBudget > 0) {
                    ((totalExpenses / monthlyBudget) * 100).toInt().coerceIn(0, 100)
                } else {
                    0
                }

                Log.d(TAG, "=== CATEGORY SPENDING ===")
                categorySpending.forEach { (category, amount) ->
                    Log.d(TAG, "$category: R$amount")
                }
                Log.d(TAG, "Total Expenses: R$totalExpenses")
                Log.d(TAG, "Monthly Budget: R$monthlyBudget")
                Log.d(TAG, "Available to Spend: R$availableToSpend")
                Log.d(TAG, "==========================")

                withContext(Dispatchers.Main) {
                    updateBudgetUI(availableToSpend, totalExpenses, monthlyBudget, progressPercentage, totalIncome)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading transactions: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.availableAmount.text = "R0.00"
                    binding.progressText.text = "0%"
                    binding.statusText.text = "Error loading data"
                }
            }
        }
    }

    private fun calculateCategorySpending(transactions: List<za.ac.iie.TallyUp.data.Transaction>): Map<String, Double> {
        val categorySpending = mutableMapOf<String, Double>()

        transactions
            .filter { it.type == "Expense" }
            .forEach { transaction ->
                val currentAmount = categorySpending[transaction.category] ?: 0.0
                categorySpending[transaction.category] = currentAmount + transaction.amount
            }

        return categorySpending
    }

    @SuppressLint("SetTextI18n")
    private fun updateBudgetUI(
        availableToSpend: Double,
        totalSpent: Double,
        totalBudget: Double,
        progressPercentage: Int,
        totalIncome: Double
    ) {
        "R${"%.2f".format(availableToSpend)}".also { binding.availableAmount.text = it }
        binding.availableSubtitle.text = "Available to Spend"

        binding.progressText.text = "${progressPercentage}%"

        val statusText = when {
            progressPercentage < 60 -> "On Track!"
            progressPercentage < 80 -> "Watch It!"
            else -> "Almost There!"
        }
        binding.statusText.text = statusText

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

        Log.d(TAG, "Budget UI Updated - Available: R$availableToSpend, Spent: R$totalSpent, Budget: R$totalBudget")
    }

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
            .replace(R.id.fragment_container, InsightsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToBudgetDashboard() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, BudgetDashboardFragment())
            .addToBackStack(null)
            .commit()
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        val characterName = CharacterManager.getCharacterName(requireContext())

        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val firstName = prefs.getString("userFirstName", null) ?: "User"

        binding.welcomeText.text = "Hey $firstName! $characterName is here to help!"

        binding.recentSection.visibility = View.VISIBLE

        val characterDrawable = CharacterManager.getCharacterDrawable(requireContext())
        binding.characterImage.setImageResource(characterDrawable)

        val mood = CharacterManager.getCurrentMood(requireContext())
        binding.moodIndicator.visibility = if (mood == za.ac.iie.TallyUp.models.Mood.SAD) View.VISIBLE else View.GONE

        val coins = CharacterManager.getCoins(requireContext())
        binding.coinsText.text = coins.toString()
    }

    override fun onResume() {
        super.onResume()
        loadGoalsFromDatabase()
        loadTransactionsAndUpdateBudget()
        debugCheckTransactions()
    }

    private fun debugCheckTransactions() {
        val userId = getCurrentUserId()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transactions = appDatabase.transactionDao().getTransactionsForUser(userId)
                Log.d(TAG, "=== DEBUG: Found ${transactions.size} transactions ===")
                transactions.forEach { transaction ->
                    Log.d(TAG, "Transaction: ${transaction.type} - R${transaction.amount} - ${transaction.category} - User: ${transaction.userId}")
                }
                Log.d(TAG, "=================================")
            } catch (e: Exception) {
                Log.e(TAG, "DEBUG Error: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}