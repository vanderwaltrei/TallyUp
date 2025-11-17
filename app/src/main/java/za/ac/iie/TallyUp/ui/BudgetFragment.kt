@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import za.ac.iie.TallyUp.adapters.CategoryBreakdownAdapter
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.data.AppRepository
import za.ac.iie.TallyUp.databinding.FragmentBudgetBinding
import za.ac.iie.TallyUp.models.BudgetCategory

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private lateinit var appDatabase: AppDatabase
    private lateinit var adapter: CategoryBreakdownAdapter

    companion object {
        private const val TAG = "BudgetFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        repository = AppRepository(requireContext())
        appDatabase = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup back button
        setupBackButton()

        loadBudgetData()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            // Navigate to home using bottom navigation (same as GoalsFragment)
            val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.selectedItemId = R.id.navigation_home
        }
    }

    private fun loadBudgetData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = repository.loadAppState()
                val userId = getCurrentUserId()
                val transactions = appDatabase.transactionDao().getTransactionsForUser(userId)

                // Get ALL categories from database (not just hardcoded ones)
                val allCategories = appDatabase.categoryDao().getCategoriesForUser(userId)

                // Convert database categories to BudgetCategory objects
                val dynamicBudgetCategories = convertToBudgetCategories(allCategories, state, transactions)

                withContext(Dispatchers.Main) {
                    setupRecyclerView(dynamicBudgetCategories, transactions)
                    updateBudgetSummary(dynamicBudgetCategories, transactions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading budget data: ${e.message}", e)
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

    private fun setupRecyclerView(categories: List<BudgetCategory>, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        binding.categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CategoryBreakdownAdapter(categories, transactions)

        // Set callback for budget updates
        adapter.onBudgetUpdated = { categoryName, newAmount ->
            updateCategoryBudget(categoryName, newAmount, transactions)
        }

        binding.categoryRecycler.adapter = adapter
    }

    @SuppressLint("SetTextI18n")
    private fun updateBudgetSummary(categories: List<BudgetCategory>, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        // Calculate total budget from all categories
        val totalBudget = calculateTotalBudget(categories)

        // Calculate total spent from actual transactions
        val spent = transactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }

        // Calculate category spending for debugging
        val categorySpending = calculateCategorySpending(transactions)

        // Update the budget amount text view
        binding.tvBudgetAmount.text = "R${"%.2f".format(totalBudget)}"
        binding.tvMonthSpent.text = "R${"%.2f".format(spent)} Spent This Month"

        Log.d(TAG, "Budget Summary Updated - Total: R$totalBudget, Spent: R$spent")

        // Debug category spending
        Log.d(TAG, "=== CATEGORY SPENDING BREAKDOWN ===")
        categorySpending.forEach { (category, amount) ->
            Log.d(TAG, "Category '$category': R$amount")
        }
        Log.d(TAG, "===================================")
    }

    private fun calculateTotalBudget(categories: List<BudgetCategory>): Double {
        // Sum all category budget amounts
        return categories.sumOf { it.budgeted }
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

        // Refresh the display
        loadBudgetData()
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        // Changed "loggedInEmail" to "userId"
        return prefs.getString("userId", "") ?: "default"
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        loadBudgetData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}