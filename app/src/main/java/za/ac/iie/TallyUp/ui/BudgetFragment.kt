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

        // Setup RecyclerView with CategoryBreakdownAdapter
        binding.categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CategoryBreakdownAdapter(state.budgetCategories, state.transactions)

        // Set callback for budget updates
        adapter.onBudgetUpdated = { categoryName, newAmount ->
            // TODO: Implement budget update logic
            // repository.updateBudgetCategory(categoryName, newAmount)
        }

        binding.categoryRecycler.adapter = adapter

        // Update budget summary
        updateBudgetSummary(state)
    }

    private fun updateBudgetSummary(state: za.ac.iie.TallyUp.models.AppState) {
        val totalBudget = state.monthlyIncome
        val spent = state.transactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }

        binding.tvBudgetAmount.text = "R${"%.2f".format(totalBudget)}"
        binding.tvMonthSpent.text = "R${"%.2f".format(spent)} Spent This Month"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}