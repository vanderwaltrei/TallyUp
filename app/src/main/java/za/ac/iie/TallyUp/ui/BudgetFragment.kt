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
import java.util.Calendar // Required for date calculations

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
        setupBackButton()
        loadBudgetData()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
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
                val allCategories = appDatabase.categoryDao().getCategoriesForUser(userId)

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

    private fun convertToBudgetCategories(
        dbCategories: List<za.ac.iie.TallyUp.data.Category>,
        state: za.ac.iie.TallyUp.models.AppState,
        transactions: List<za.ac.iie.TallyUp.data.Transaction>
    ): List<BudgetCategory> {
        return dbCategories.map { dbCategory ->
            val existingBudgetCategory = state.budgetCategories.find { it.name == dbCategory.name }

            if (existingBudgetCategory != null) {
                existingBudgetCategory
            } else {
                val spent = transactions
                    .filter { it.type == "Expense" && it.category == dbCategory.name }
                    .sumOf { it.amount }

                val defaultBudget = when (dbCategory.type) {
                    "Income" -> 0.0
                    else -> 100.0
                }

                BudgetCategory(
                    name = dbCategory.name,
                    budgeted = defaultBudget,
                    spent = spent
                )
            }
        }.filter { it.budgeted > 0 }
    }

    private fun setupRecyclerView(categories: List<BudgetCategory>, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        binding.categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CategoryBreakdownAdapter(categories, transactions)

        adapter.onBudgetUpdated = { categoryName, newAmount ->
            updateCategoryBudget(categoryName, newAmount, transactions)
        }

        binding.categoryRecycler.adapter = adapter
    }

    @SuppressLint("SetTextI18n")
    private fun updateBudgetSummary(categories: List<BudgetCategory>, transactions: List<za.ac.iie.TallyUp.data.Transaction>) {
        // 1. Calculate Totals
        val totalBudget = calculateTotalBudget(categories)
        val spent = transactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }
        val remaining = totalBudget - spent

        // 2. Calculate Percentage
        val percentage = if (totalBudget > 0) ((spent / totalBudget) * 100).toInt() else 0

        // 3. Calculate Date/Daily Stats
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = lastDay - currentDay

        // Avoid division by zero if it's the last day of the month
        val safeDaysLeft = if (daysLeft < 1) 1 else daysLeft
        val dailyBudget = if (remaining > 0) remaining / safeDaysLeft else 0.0

        // --- UPDATE UI ELEMENTS ---

        // Main Amounts
        binding.tvBudgetAmount.text = "R ${"%.2f".format(totalBudget)}"
        binding.tvMonthSpent.text = "R ${"%.2f".format(spent)}"
        binding.tvBudgetRemaining.text = "R ${"%.2f".format(remaining)}"

        // Progress Bar
        binding.budgetProgressBar.progress = percentage.coerceIn(0, 100)
        binding.tvBudgetPercentage.text = "$percentage%"

        // Status Text & Color
        val (statusText, statusColor, statusBg) = when {
            percentage >= 100 -> Triple("Budget exceeded!", R.color.destructive, R.color.error) // Red
            percentage >= 85 -> Triple("Slow down, you're almost out!", R.color.warning, R.color.warning_light) // Orange
            else -> Triple("You're on track! Keep it up.", R.color.success, R.color.success_light) // Green
        }

        binding.tvBudgetStatus.text = statusText
        binding.tvBudgetStatus.setTextColor(requireContext().getColor(statusColor))
        binding.tvBudgetStatus.setBackgroundColor(requireContext().getColor(statusBg))

        // Quick Stats
        binding.tvDaysLeft.text = daysLeft.toString()
        binding.tvDailyBudget.text = "R ${"%.0f".format(dailyBudget)}" // Rounded to whole number for cleanliness

        // Formatting remaining text color based on positive/negative
        if (remaining < 0) {
            binding.tvBudgetRemaining.setTextColor(requireContext().getColor(R.color.destructive))
        } else {
            binding.tvBudgetRemaining.setTextColor(requireContext().getColor(R.color.success))
        }

        Log.d(TAG, "Budget Summary Updated - Total: R$totalBudget, Spent: R$spent, Rem: R$remaining, Daily: R$dailyBudget")
    }

    private fun calculateTotalBudget(categories: List<BudgetCategory>): Double {
        return categories.sumOf { it.budgeted }
    }

    private fun updateCategoryBudget(
        categoryName: String,
        newAmount: Double,
        transactions: List<za.ac.iie.TallyUp.data.Transaction>
    ) {
        val state = repository.loadAppState()
        val updatedCategories = state.budgetCategories.toMutableList()
        val existingIndex = updatedCategories.indexOfFirst { it.name == categoryName }

        if (existingIndex != -1) {
            updatedCategories[existingIndex] = updatedCategories[existingIndex].copy(budgeted = newAmount)
        } else {
            val spent = transactions
                .filter { it.type == "Expense" && it.category == categoryName }
                .sumOf { it.amount }
            updatedCategories.add(BudgetCategory(categoryName, newAmount, spent))
        }

        val updatedState = state.copy(budgetCategories = updatedCategories)
        repository.saveAppState(updatedState)
        loadBudgetData()
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", "") ?: "default"
    }

    override fun onResume() {
        super.onResume()
        loadBudgetData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}