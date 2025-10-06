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
import za.ac.iie.TallyUp.databinding.FragmentBudgetBinding

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

        loadBudgetData()
    }

    private fun loadBudgetData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = repository.loadAppState()
                val userId = getCurrentUserId()
                val transactions = appDatabase.transactionDao().getTransactionsForUser(userId)

                withContext(Dispatchers.Main) {
                    setupRecyclerView(state, transactions)
                    updateBudgetSummary(state, transactions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading budget data: ${e.message}", e)
            }
        }
    }

    private fun setupRecyclerView(state: za.ac.iie.TallyUp.models.AppState, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        binding.categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CategoryBreakdownAdapter(state.budgetCategories, transactions)

        // Set callback for budget updates
        adapter.onBudgetUpdated = { categoryName, newAmount ->
            updateCategoryBudget(state, categoryName, newAmount, transactions)
        }

        binding.categoryRecycler.adapter = adapter
    }

    private fun updateBudgetSummary(state: za.ac.iie.TallyUp.models.AppState, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        // Calculate total budget from all categories
        val totalBudget = calculateTotalBudget(state)

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

    private fun calculateTotalBudget(state: za.ac.iie.TallyUp.models.AppState): Double {
        // Sum all category budget amounts
        return state.budgetCategories.sumOf { it.budgeted }
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

        // Refresh both the summary and the adapter
        refreshBudgetDisplay(updatedCategories, transactions)
    }

    private fun refreshBudgetDisplay(updatedCategories: List<za.ac.iie.TallyUp.models.BudgetCategory>, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        val state = repository.loadAppState()

        // Update the summary
        updateBudgetSummary(state, transactions)

        // Update the adapter with new categories
        adapter.updateCategories(updatedCategories)

        Log.d(TAG, "Budget display refreshed with ${updatedCategories.size} categories")

        // Debug: Print all category budgets
        updatedCategories.forEach { category ->
            val categorySpent = transactions
                .filter { it.type == "Expense" && it.category == category.name }
                .sumOf { it.amount }
            Log.d(TAG, "Category: ${category.name}, Budget: R${category.budgeted}, Spent: R$categorySpent")
        }
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        return prefs.getString("loggedInEmail", "") ?: "default"
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