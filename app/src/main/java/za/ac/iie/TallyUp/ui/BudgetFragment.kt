package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import za.ac.iie.TallyUp.adapters.CategoryBreakdownAdapter
import za.ac.iie.TallyUp.data.AppRepository
import za.ac.iie.TallyUp.databinding.FragmentBudgetBinding

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: AppRepository
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val state = repository.loadAppState()

        // Setup RecyclerView with CategoryBreakdownAdapter
        binding.categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CategoryBreakdownAdapter(state.budgetCategories, state.transactions)

        // Set callback for budget updates
        adapter.onBudgetUpdated = { categoryName, newAmount ->
            updateCategoryBudget(state, categoryName, newAmount)
        }

        binding.categoryRecycler.adapter = adapter

        // Update budget summary with calculated total
        updateBudgetSummary()
    }

    private fun updateBudgetSummary() {
        val state = repository.loadAppState()

        // Calculate total budget from all categories
        val totalBudget = calculateTotalBudget(state)

        // Calculate total spent from transactions
        val spent = state.transactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }

        // Update the budget amount text view
        binding.tvBudgetAmount.text = "R${"%.2f".format(totalBudget)}"
        binding.tvMonthSpent.text = "R${"%.2f".format(spent)} Spent This Month"

        Log.d(TAG, "Budget Summary Updated - Total: R$totalBudget, Spent: R$spent")
    }

    private fun calculateTotalBudget(state: za.ac.iie.TallyUp.models.AppState): Double {
        // Sum all category budget amounts
        return state.budgetCategories.sumOf { it.budgeted }
    }

    private fun updateCategoryBudget(
        state: za.ac.iie.TallyUp.models.AppState,
        categoryName: String,
        newAmount: Double
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
        refreshBudgetDisplay(updatedCategories)
    }

    private fun refreshBudgetDisplay(updatedCategories: List<za.ac.iie.TallyUp.models.BudgetCategory>) {
        // Update the summary
        updateBudgetSummary()

        // Update the adapter with new categories
        adapter.updateCategories(updatedCategories)

        Log.d(TAG, "Budget display refreshed with ${updatedCategories.size} categories")

        // Debug: Print all category budgets
        updatedCategories.forEach { category ->
            Log.d(TAG, "Category: ${category.name}, Budget: R${category.budgeted}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        val state = repository.loadAppState()
        updateBudgetSummary()
        adapter.updateCategories(state.budgetCategories)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}