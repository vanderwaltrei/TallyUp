package za.ac.iie.TallyUp.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import za.ac.iie.TallyUp.models.BudgetCategory
import java.util.Calendar
import java.util.Date

class BudgetDashboardFragment : Fragment() {

    private var _binding: FragmentBudgetDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: AppRepository
    private lateinit var appDatabase: AppDatabase
    private var selectedTimePeriod = "All" // Default selection

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
        setupTimePeriodSpinner()
        loadBudgetDashboardData()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupTimePeriodSpinner() {
        // Create adapter using the string array from resources
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            za.ac.iie.TallyUp.R.array.time_filter_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.timePeriodSpinner.adapter = adapter

        // Set spinner selection listener
        binding.timePeriodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedTimePeriod = parent.getItemAtPosition(position).toString()
                Log.d(TAG, "Time period selected: $selectedTimePeriod")
                loadBudgetDashboardData()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun loadBudgetDashboardData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = repository.loadAppState()
                val userId = getCurrentUserId()

                // Get filtered transactions based on selected time period
                val filteredTransactions = getFilteredTransactions(userId, selectedTimePeriod)

                // Get ALL categories from database (not just hardcoded ones)
                val allCategories = appDatabase.categoryDao().getCategoriesForUser(userId)

                // Convert database categories to BudgetCategory objects
                val dynamicBudgetCategories = convertToBudgetCategories(allCategories, state, filteredTransactions)

                withContext(Dispatchers.Main) {
                    updateBudgetHealthCard(dynamicBudgetCategories, filteredTransactions)
                    updatePerformanceSummary(dynamicBudgetCategories, filteredTransactions)
                    setupCategoryBreakdown(dynamicBudgetCategories, filteredTransactions)
                    updateSmartRecommendations(dynamicBudgetCategories, filteredTransactions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading budget dashboard data: ${e.message}", e)
            }
        }
    }

    /**
     * Convert database categories to BudgetCategory objects
     * Uses existing budget amounts from AppState if available, otherwise sets default budget
     */
    private fun convertToBudgetCategories(
        dbCategories: List<za.ac.iie.TallyUp.data.Category>,
        state: za.ac.iie.TallyUp.models.AppState,
        transactions: List<za.ac.iie.TallyUp.data.Transaction>
    ): List<BudgetCategory> {
        return dbCategories.map { dbCategory ->
            // Check if this category exists in the AppState budget categories
            val existingBudgetCategory = state.budgetCategories.find { it.name == dbCategory.name }

            if (existingBudgetCategory != null) {
                // Use the existing budget amount
                existingBudgetCategory
            } else {
                // Create new BudgetCategory with default budget
                // Calculate spent amount from transactions
                val spent = transactions
                    .filter { it.type == "Expense" && it.category == dbCategory.name }
                    .sumOf { it.amount }

                // Set default budget based on category type or use a sensible default
                val defaultBudget = when (dbCategory.type) {
                    "Income" -> 0.0 // Income categories don't need budgets
                    else -> 100.0 // Default budget for new expense categories
                }

                BudgetCategory(
                    name = dbCategory.name,
                    budgeted = defaultBudget,
                    spent = spent
                )
            }
        }.filter { it.budgeted > 0 } // Only show categories that have a budget
    }

    private suspend fun getFilteredTransactions(userId: String, timePeriod: String): List<za.ac.iie.TallyUp.data.Transaction> {
        val allTransactions = appDatabase.transactionDao().getTransactionsForUser(userId)

        if (timePeriod == "All") {
            return allTransactions
        }

        val calendar = Calendar.getInstance()
        val now = Date().time

        return when (timePeriod) {
            "Today" -> {
                calendar.time = Date(now)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis

                allTransactions.filter { it.date >= startOfDay }
            }
            "This Week" -> {
                calendar.time = Date(now)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfWeek = calendar.timeInMillis

                allTransactions.filter { it.date >= startOfWeek }
            }
            "This Month" -> {
                calendar.time = Date(now)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis

                allTransactions.filter { it.date >= startOfMonth }
            }
            else -> allTransactions
        }
    }

    private fun updateBudgetHealthCard(categories: List<BudgetCategory>, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        // Calculate total budget from all categories
        val totalBudget = categories.sumOf { it.budgeted }

        // Calculate total spent from filtered transactions
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

        Log.d(TAG, "Budget Health Updated - Period: $selectedTimePeriod, Spent: R$totalSpent, Budget: R$totalBudget, Progress: $progressPercentage%")
    }

    private fun updatePerformanceSummary(categories: List<BudgetCategory>, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        val categoryPerformance = calculateCategoryPerformance(categories, transactions)

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

    private fun calculateCategoryPerformance(categories: List<BudgetCategory>, transactions: List<za.ac.iie.TallyUp.data.Transaction>): List<Triple<String, Double, Double>> {
        return categories.map { category ->
            val spent = transactions
                .filter { it.type == "Expense" && it.category == category.name }
                .sumOf { it.amount }
            Triple(category.name, spent, category.budgeted)
        }
    }

    private fun getCategoriesWithTransactions(allCategories: List<BudgetCategory>, transactions: List<za.ac.iie.TallyUp.data.Transaction>): List<BudgetCategory> {
        // Get unique category names from transactions
        val categoryNamesWithTransactions = transactions
            .filter { it.type == "Expense" }
            .map { it.category }
            .distinct()

        // Return only categories that have transactions in this period
        return allCategories.filter { category ->
            categoryNamesWithTransactions.contains(category.name)
        }
    }

    private fun setupCategoryBreakdown(categories: List<BudgetCategory>, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        binding.categoryRecycler.layoutManager = LinearLayoutManager(requireContext())

        // Only show categories that have transactions in the filtered period
        val categoriesWithTransactions = getCategoriesWithTransactions(categories, transactions)

        if (categoriesWithTransactions.isEmpty()) {
            // Show empty state message
            binding.categoryRecycler.visibility = View.GONE
            // You could add a TextView here to show "No transactions in selected period"
        } else {
            binding.categoryRecycler.visibility = View.VISIBLE
            val adapter = CategoryBreakdownAdapter(categoriesWithTransactions, transactions)

            // Set callback for budget updates
            adapter.onBudgetUpdated = { categoryName, newAmount ->
                updateCategoryBudget(categoryName, newAmount, transactions)
            }

            binding.categoryRecycler.adapter = adapter
        }
    }

    private fun updateCategoryBudget(
        categoryName: String,
        newAmount: Double,
        transactions: List<za.ac.iie.TallyUp.data.Transaction>
    ) {
        Log.d(TAG, "Updating budget for category: $categoryName to R$newAmount")

        // Update the app state
        val state = repository.loadAppState()
        val updatedCategories = state.budgetCategories.toMutableList()

        // Find if category already exists in budget categories
        val existingIndex = updatedCategories.indexOfFirst { it.name == categoryName }

        if (existingIndex != -1) {
            // Update existing category
            updatedCategories[existingIndex] = updatedCategories[existingIndex].copy(budgeted = newAmount)
        } else {
            // Add new category to budget categories
            val spent = transactions
                .filter { it.type == "Expense" && it.category == categoryName }
                .sumOf { it.amount }

            updatedCategories.add(BudgetCategory(categoryName, newAmount, spent))
        }

        // Update the app state
        val updatedState = state.copy(budgetCategories = updatedCategories)
        repository.saveAppState(updatedState)

        Log.d(TAG, "Budget updated and saved to repository")

        // Refresh the dashboard
        loadBudgetDashboardData()
    }

    private fun updateSmartRecommendations(categories: List<BudgetCategory>, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        val totalBudget = categories.sumOf { it.budgeted }
        val totalSpent = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val progressPercentage = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toInt() else 0

        val recommendation = when {
            progressPercentage < 50 -> "• Great job! You're well within your budget"
            progressPercentage < 75 -> "• You're doing well, but keep an eye on your spending"
            progressPercentage < 90 -> "• Watch your spending - you're approaching your budget limit"
            else -> "• Consider reviewing your expenses - you're close to exceeding your budget"
        }

        // Update the recommendation text in the XML
        updateRecommendationText(recommendation)

        Log.d(TAG, "Smart Recommendation: $recommendation")
    }

    private fun updateRecommendationText(recommendation: String) {
        // This will update the recommendation text in the smart recommendations card
        // The TextView in the XML should have an ID for this to work properly
        // For now, we'll log it since the XML doesn't have a specific ID
        Log.d(TAG, "Recommendation: $recommendation")
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