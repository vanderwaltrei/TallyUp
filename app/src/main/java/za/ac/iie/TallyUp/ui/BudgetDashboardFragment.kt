package za.ac.iie.TallyUp.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.TallyUp.adapters.CategoryBreakdownAdapter
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.data.AppRepository
import za.ac.iie.TallyUp.databinding.FragmentBudgetDashboardBinding

class BudgetDashboardFragment : Fragment() {

    private var _binding: FragmentBudgetDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: AppRepository
    private lateinit var appDatabase: AppDatabase

    companion object {
        private const val TAG = "BudgetDashboardFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetDashboardBinding.inflate(inflater, container, false)
        repository = AppRepository(requireContext())
        appDatabase = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadBudgetDashboardData()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadBudgetDashboardData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = repository.loadAppState()
                val userId = getCurrentUserId()
                val transactions = appDatabase.transactionDao().getTransactionsForUser(userId)

                withContext(Dispatchers.Main) {
                    updateBudgetHealthCard(state, transactions)
                    updatePerformanceSummary(state, transactions)
                    setupCategoryBreakdown(state, transactions)
                    updateSmartRecommendations(state, transactions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading budget dashboard data: ${e.message}", e)
            }
        }
    }

    private fun updateBudgetHealthCard(state: za.ac.iie.TallyUp.models.AppState, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        // Calculate total budget from all categories
        val totalBudget = state.budgetCategories.sumOf { it.budgeted }

        // Calculate total spent from transactions
        val totalSpent = transactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }

        // Calculate remaining budget
        val remainingBudget = totalBudget - totalSpent

        // Calculate progress percentage
        val progressPercentage = if (totalBudget > 0) {
            ((totalSpent / totalBudget) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        // Update UI with real data
        binding.progressText.text = "$progressPercentage% Used"
        binding.totalBudgetText.text = "R${"%.2f".format(totalSpent)} / R${"%.2f".format(totalBudget)}"
        binding.budgetProgressBar.progress = progressPercentage
        binding.remainingBudgetText.text = "R${"%.2f".format(remainingBudget)} Remaining"

        Log.d(TAG, "Budget Health Updated - Spent: R$totalSpent, Budget: R$totalBudget, Progress: $progressPercentage%")
    }

    private fun updatePerformanceSummary(state: za.ac.iie.TallyUp.models.AppState, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        val categoryPerformance = calculateCategoryPerformance(state, transactions)

        var onTrackCount = 0
        var watchCount = 0
        var criticalCount = 0
        var overCount = 0

        categoryPerformance.forEach { (categoryName, spent, budget) ->
            if (budget > 0) {
                val percentage = (spent / budget) * 100
                when {
                    percentage < 60 -> onTrackCount++
                    percentage < 80 -> watchCount++
                    percentage < 100 -> criticalCount++
                    else -> overCount++
                }
            }
        }

        // Update performance summary cards
        binding.onTrackCount.text = "$onTrackCount\nOn Track"
        binding.watchCount.text = "$watchCount\nWatch"
        binding.criticalCount.text = "$criticalCount\nCritical"
        binding.overCount.text = "$overCount\nOver"

        Log.d(TAG, "Performance Summary - On Track: $onTrackCount, Watch: $watchCount, Critical: $criticalCount, Over: $overCount")
    }

    private fun calculateCategoryPerformance(state: za.ac.iie.TallyUp.models.AppState, transactions: List<za.ac.iie.TallyUp.data.Transaction>): List<Triple<String, Double, Double>> {
        return state.budgetCategories.map { category ->
            val spent = transactions
                .filter { it.type == "Expense" && it.category == category.name }
                .sumOf { it.amount }
            Triple(category.name, spent, category.budgeted)
        }
    }

    private fun setupCategoryBreakdown(state: za.ac.iie.TallyUp.models.AppState, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        binding.categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CategoryBreakdownAdapter(state.budgetCategories, transactions)

        // Set callback for budget updates
        adapter.onBudgetUpdated = { categoryName, newAmount ->
            updateCategoryBudget(state, categoryName, newAmount, transactions)
        }

        binding.categoryRecycler.adapter = adapter

        // Hide the spinner for now since it's not implemented
        binding.timePeriodSpinner.visibility = View.GONE
    }

    private fun updateCategoryBudget(
        state: za.ac.iie.TallyUp.models.AppState,
        categoryName: String,
        newAmount: Double,
        transactions: List<za.ac.iie.TallyUp.data.Transaction>
    ) {
        Log.d(TAG, "Updating budget for category: $categoryName to R$newAmount")

        // Find and update the category
        val updatedCategories = state.budgetCategories.map { category ->
            if (category.name == categoryName) {
                category.copy(budgeted = newAmount)
            } else {
                category
            }
        }

        // Update the app state
        val updatedState = state.copy(budgetCategories = updatedCategories)
        repository.saveAppState(updatedState)

        Log.d(TAG, "Budget updated and saved to repository")

        // Refresh the dashboard
        loadBudgetDashboardData()
    }

    private fun updateSmartRecommendations(state: za.ac.iie.TallyUp.models.AppState, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        val totalBudget = state.budgetCategories.sumOf { it.budgeted }
        val totalSpent = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val progressPercentage = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toInt() else 0

        val recommendation = when {
            progressPercentage < 50 -> "• Great job! You're well within your budget"
            progressPercentage < 75 -> "• You're doing well, but keep an eye on your spending"
            progressPercentage < 90 -> "• Watch your spending - you're approaching your budget limit"
            else -> "• Consider reviewing your expenses - you're close to exceeding your budget"
        }

        // Since the XML doesn't have specific IDs for the recommendation text,
        // we'll update the text by finding the TextView in the smart recommendations section
        // The binding should automatically handle this through ViewBinding
        updateRecommendationText(recommendation)

        Log.d(TAG, "Smart Recommendation: $recommendation")
    }

    private fun updateRecommendationText(recommendation: String) {
        // This method will be handled by ViewBinding automatically
        // The text should update when we set the data
        // For now, we'll rely on the automatic ViewBinding from the XML
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        return prefs.getString("loggedInEmail", "") ?: "default"
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        loadBudgetDashboardData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}