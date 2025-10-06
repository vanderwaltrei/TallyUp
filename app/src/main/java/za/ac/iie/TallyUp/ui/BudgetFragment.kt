package za.ac.iie.TallyUp.ui

import android.os.Bundle
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

        // Calculate total budget from all categories
        val totalBudget = calculateTotalBudget(state)

        // Setup RecyclerView with CategoryBreakdownAdapter
        binding.categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CategoryBreakdownAdapter(state.budgetCategories, state.transactions)

        // Set callback for budget updates
        adapter.onBudgetUpdated = { categoryName, newAmount ->
            // Update the category budget and refresh the total
            updateCategoryBudget(state, categoryName, newAmount)
            refreshBudgetDisplay()
        }

        binding.categoryRecycler.adapter = adapter

        // Update budget summary with calculated total
        updateBudgetSummary(totalBudget)
    }

    private fun calculateTotalBudget(state: za.ac.iie.TallyUp.models.AppState): Double {
        // Sum all category budget amounts
        return state.budgetCategories.sumOf { it.budgeted }
    }

    private fun updateBudgetSummary(totalBudget: Double) {
        val state = repository.loadAppState()

        // Calculate total spent from transactions
        val spent = state.transactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }

        // Update the budget amount text view
        binding.tvBudgetAmount.text = "R${"%.2f".format(totalBudget)}"
        binding.tvMonthSpent.text = "R${"%.2f".format(spent)} Spent This Month"
    }

    private fun updateCategoryBudget(
        state: za.ac.iie.TallyUp.models.AppState,
        categoryName: String,
        newAmount: Double
    ) {
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
    }

    private fun refreshBudgetDisplay() {
        val state = repository.loadAppState()
        val totalBudget = calculateTotalBudget(state)
        updateBudgetSummary(totalBudget)

        // Refresh the RecyclerView adapter
        binding.categoryRecycler.adapter?.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        val state = repository.loadAppState()
        val totalBudget = calculateTotalBudget(state)
        updateBudgetSummary(totalBudget)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}