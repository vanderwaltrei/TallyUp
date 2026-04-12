@file:Suppress("PackageName")
package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.TallyUp.adapters.CategoryBreakdownAdapter  // ✅ correct package
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.data.AppRepository
import za.ac.iie.TallyUp.databinding.FragmentBudgetDashboardBinding
import za.ac.iie.TallyUp.firebase.FirebaseRepository
import za.ac.iie.TallyUp.models.BudgetCategory

class BudgetDashboardFragment : Fragment() {

    private var _binding: FragmentBudgetDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: AppRepository
    private lateinit var appDatabase: AppDatabase
    private lateinit var adapter: CategoryBreakdownAdapter
    private val firebaseRepo = FirebaseRepository()

    private var allCategories: List<BudgetCategory> = emptyList()
    private var allTransactions: List<za.ac.iie.TallyUp.data.Transaction> = emptyList()

    private enum class FilterMode { ALL, OVER_BUDGET, ON_TRACK, HIGHEST_SPEND }
    private var currentFilter = FilterMode.ALL

    companion object {
        private const val TAG = "BudgetDashboardFragment"
    }

    // ─────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetDashboardBinding.inflate(inflater, container, false)
        repository = AppRepository(requireContext())
        appDatabase = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackButton()
        setupChips()
        setupSortButton()
        setupFab()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─────────────────────────────────────────────
    // Setup helpers
    // ─────────────────────────────────────────────

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupChips() {
        binding.chipAll.setOnClickListener {
            currentFilter = FilterMode.ALL
            applyFilter()
        }
        binding.chipOverBudget.setOnClickListener {
            currentFilter = FilterMode.OVER_BUDGET
            applyFilter()
        }
        binding.chipOnTrack.setOnClickListener {
            currentFilter = FilterMode.ON_TRACK
            applyFilter()
        }
        binding.chipHighestSpend.setOnClickListener {
            currentFilter = FilterMode.HIGHEST_SPEND
            applyFilter()
        }
    }

    private fun setupSortButton() {
        binding.btnSort.setOnClickListener {
            currentFilter = when (currentFilter) {
                FilterMode.ALL           -> FilterMode.HIGHEST_SPEND
                FilterMode.HIGHEST_SPEND -> FilterMode.OVER_BUDGET
                FilterMode.OVER_BUDGET   -> FilterMode.ON_TRACK
                FilterMode.ON_TRACK      -> FilterMode.ALL
            }
            when (currentFilter) {
                FilterMode.ALL           -> binding.chipAll.isChecked = true
                FilterMode.OVER_BUDGET   -> binding.chipOverBudget.isChecked = true
                FilterMode.ON_TRACK      -> binding.chipOnTrack.isChecked = true
                FilterMode.HIGHEST_SPEND -> binding.chipHighestSpend.isChecked = true
            }
            applyFilter()
        }
    }

    private fun setupFab() {
        binding.fabAddCategory.setOnClickListener {
            Toast.makeText(requireContext(), "Add category coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────
    // Data loading
    // ─────────────────────────────────────────────

    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = getCurrentUserId()
                Log.d(TAG, "📊 Loading all category data for user: $userId")

                val firebaseResult = firebaseRepo.getBudgetCategories()
                val firebaseBudgets = firebaseResult.getOrNull()

                if (firebaseBudgets != null && firebaseBudgets.isNotEmpty()) {
                    val state = repository.loadAppState()
                    repository.saveAppState(state.copy(budgetCategories = firebaseBudgets))
                    Log.d(TAG, "✅ Firebase sync: ${firebaseBudgets.size} categories")
                } else {
                    val state = repository.loadAppState()
                    if (state.budgetCategories.isNotEmpty()) {
                        firebaseRepo.saveBudgetCategories(state.budgetCategories)
                        Log.d(TAG, "🔄 Pushed local budgets to Firebase")
                    }
                }

                val state = repository.loadAppState()
                val transactions = appDatabase.transactionDao().getTransactionsForUser(userId)
                val dbCategories = appDatabase.categoryDao().getCategoriesForUser(userId)
                val budgetCategories = buildBudgetCategories(dbCategories, state, transactions)

                withContext(Dispatchers.Main) {
                    allCategories   = budgetCategories
                    allTransactions = transactions
                    setupRecyclerView(budgetCategories, transactions)
                    updateSummaryBanner(budgetCategories, transactions)
                    applyFilter()
                    Log.d(TAG, "✅ Dashboard UI updated — ${budgetCategories.size} categories")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading dashboard: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to load categories. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Filter & sort
    // ─────────────────────────────────────────────

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            FilterMode.ALL           -> allCategories
            FilterMode.OVER_BUDGET   -> allCategories.filter { it.spent > it.budgeted }
            FilterMode.ON_TRACK      -> allCategories.filter { it.spent <= it.budgeted }
            FilterMode.HIGHEST_SPEND -> allCategories.sortedByDescending { it.spent }
        }

        adapter.updateCategories(filtered)

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.categoryRecycler.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.categoryRecycler.visibility = View.VISIBLE
        }

        binding.tvCategoryCount.text =
            "${filtered.size} ${if (filtered.size == 1) "category" else "categories"}"
    }

    // ─────────────────────────────────────────────
    // RecyclerView
    // ─────────────────────────────────────────────

    private fun setupRecyclerView(
        categories: List<BudgetCategory>,
        transactions: List<za.ac.iie.TallyUp.data.Transaction>
    ) {
        if (!::adapter.isInitialized) {
            adapter = CategoryBreakdownAdapter(categories, transactions)
            adapter.onBudgetUpdated = { categoryName, newAmount ->
                updateCategoryBudget(categoryName, newAmount, transactions)
            }
            binding.categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
            binding.categoryRecycler.adapter = adapter
        } else {
            adapter.updateTransactions(transactions)
            adapter.updateCategories(categories)
        }
    }

    // ─────────────────────────────────────────────
    // Summary banner
    // ─────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private fun updateSummaryBanner(
        categories: List<BudgetCategory>,
        transactions: List<za.ac.iie.TallyUp.data.Transaction>
    ) {
        val totalBudgeted = categories.sumOf { it.budgeted }
        val totalSpent    = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val remaining     = totalBudgeted - totalSpent
        val percentage    = if (totalBudgeted > 0) ((totalSpent / totalBudgeted) * 100).toInt() else 0

        binding.tvTotalBudgeted.text        = "R ${"%.2f".format(totalBudgeted)}"
        binding.tvTotalSpent.text           = "R ${"%.2f".format(totalSpent)}"
        binding.tvTotalRemaining.text       = "R ${"%.2f".format(remaining)} left"
        binding.tvOverallPercentage.text    = "${percentage.coerceIn(0, 100)}% used"
        binding.overallProgressBar.progress = percentage.coerceIn(0, 100)
    }

    // ─────────────────────────────────────────────
    // Budget update
    // ─────────────────────────────────────────────

    private fun updateCategoryBudget(
        categoryName: String,
        newAmount: Double,
        transactions: List<za.ac.iie.TallyUp.data.Transaction>
    ) {
        val state = repository.loadAppState()
        val updatedCategories = state.budgetCategories.toMutableList()
        val idx = updatedCategories.indexOfFirst { it.name == categoryName }

        if (idx != -1) {
            updatedCategories[idx] = updatedCategories[idx].copy(budgeted = newAmount)
        } else {
            val spent = transactions
                .filter { it.type == "Expense" && it.category == categoryName }
                .sumOf { it.amount }
            updatedCategories.add(BudgetCategory(categoryName, newAmount, spent))
        }

        repository.saveAppState(state.copy(budgetCategories = updatedCategories))
        Log.d(TAG, "📝 Budget updated for '$categoryName': R$newAmount")

        lifecycleScope.launch {
            try {
                firebaseRepo.saveBudgetCategories(updatedCategories)
                Log.d(TAG, "✅ Firebase sync after budget update")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firebase sync failed: ${e.message}", e)
            }
        }

        loadData()
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private fun buildBudgetCategories(
        dbCategories: List<za.ac.iie.TallyUp.data.Category>,
        state: za.ac.iie.TallyUp.models.AppState,
        transactions: List<za.ac.iie.TallyUp.data.Transaction>
    ): List<BudgetCategory> {
        return dbCategories.map { dbCategory ->
            val existing = state.budgetCategories.find { it.name == dbCategory.name }
            existing ?: run {
                val spent = transactions
                    .filter { it.type == "Expense" && it.category == dbCategory.name }
                    .sumOf { it.amount }
                BudgetCategory(
                    name     = dbCategory.name,
                    budgeted = if (dbCategory.type == "Income") 0.0 else 100.0,
                    spent    = spent
                )
            }
        }.filter { it.budgeted > 0 }
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", "") ?: ""
    }
}