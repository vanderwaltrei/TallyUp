package za.ac.iie.TallyUp.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import za.ac.iie.TallyUp.data.AppRepository
import za.ac.iie.TallyUp.databinding.FragmentBudgetDashboardBinding

class BudgetDashboardFragment : Fragment() {

    private var _binding: FragmentBudgetDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: AppRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetDashboardBinding.inflate(inflater, container, false)
        repository = AppRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val state = repository.loadAppState()

        val totalBudget = state.monthlyIncome
        val spent = state.transactions.sumOf { it.amount }
        val remaining = totalBudget - spent
        val progress = if (totalBudget > 0) (spent / totalBudget * 100).toInt() else 0

        binding.progressText.text = "$progress% Used"
        binding.totalBudgetText.text = "R${spent} / R${totalBudget}"
        binding.budgetProgressBar.progress = progress
        binding.remainingBudgetText.text = "R${remaining} Remaining"
        binding.daysLeftText.text = "5 days left" // TODO: calculate from cycle

        // Setup category breakdown recycler
        binding.categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryRecycler.adapter = CategoryBreakdownAdapter(state.budgetCategories, state.transactions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
