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
import za.ac.iie.TallyUp.firebase.FirebaseRepository
import java.util.Calendar

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private lateinit var appDatabase: AppDatabase
    private lateinit var adapter: CategoryBreakdownAdapter
    private val firebaseRepo = FirebaseRepository()

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
        setupViewAllButton()
        loadBudgetData()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.selectedItemId = R.id.navigation_home
        }
    }

    private fun setupViewAllButton() {
        // Navigate to the Dashboard (Detailed View) when clicked
        binding.btnViewAll.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BudgetDashboardFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadBudgetData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = getCurrentUserId()
                Log.d(TAG, "📊 Loading budget data for user: $userId")

                // ✅ STEP 1: Try to load from Firebase first (source of truth)
                val firebaseResult = firebaseRepo.getBudgetCategories()
                val firebaseBudgets = firebaseResult.getOrNull()

                if (firebaseBudgets != null && firebaseBudgets.isNotEmpty()) {
                    // ✅ Firebase has data - use it and update local cache
                    val state = repository.loadAppState()
                    val updatedState = state.copy(budgetCategories = firebaseBudgets)
                    repository.saveAppState(updatedState)
                    Log.d(TAG, "✅ Loaded ${firebaseBudgets.size} budgets from Firebase")
                } else {
                    // ✅ No Firebase data found
                    Log.d(TAG, "ℹ️ No budgets found in Firebase")

                    // Check if we have local data to sync up
                    val state = repository.loadAppState()
                    if (state.budgetCategories.isNotEmpty()) {
                        // We have local data - sync it to Firebase
                        Log.d(TAG, "🔄 Syncing ${state.budgetCategories.size} local budgets to Firebase...")

                        val syncResult = firebaseRepo.saveBudgetCategories(state.budgetCategories)
                        if (syncResult.isSuccess) {
                            Log.d(TAG, "✅ Successfully synced local budgets to Firebase")
                        } else {
                            Log.e(TAG, "❌ Failed to sync budgets to Firebase: ${syncResult.exceptionOrNull()?.message}")
                        }
                    } else {
                        Log.d(TAG, "ℹ️ No local budgets found either - user has no budget data yet")
                    }
                }

                // ✅ STEP 2: Load from local cache (now user-specific)
                val state = repository.loadAppState()
                val transactions = appDatabase.transactionDao().getTransactionsForUser(userId)
                val allCategories = appDatabase.categoryDao().getCategoriesForUser(userId)

                Log.d(TAG, "📂 Loaded ${transactions.size} transactions and ${allCategories.size} categories")

                // ✅ STEP 3: Convert to budget categories with current spending
                val dynamicBudgetCategories = convertToBudgetCategories(allCategories, state, transactions)

                Log.d(TAG, "💰 Generated ${dynamicBudgetCategories.size} budget categories")

                // ✅ STEP 4: Update UI on main thread
                withContext(Dispatchers.Main) {
                    setupRecyclerView(dynamicBudgetCategories, transactions)
                    updateBudgetSummary(dynamicBudgetCategories, transactions)
                    Log.d(TAG, "✅ Budget UI updated successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading budget data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to load budget data. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun convertToBudgetCategories(
        dbCategories: List<za.ac.iie.TallyUp.data.Category>,
        state: za.ac.iie.TallyUp.models.AppState,
        transactions: List<za.ac.iie.TallyUp.data.Transaction>
    ): List<BudgetCategory> {
        return dbCategories.map { dbCategory ->
            // Check if this category already has a budget set
            val existingBudgetCategory = state.budgetCategories.find { it.name == dbCategory.name }

            if (existingBudgetCategory != null) {
                // Use existing budget amount
                existingBudgetCategory
            } else {
                // Calculate current spending for this category
                val spent = transactions
                    .filter { it.type == "Expense" && it.category == dbCategory.name }
                    .sumOf { it.amount }

                // Assign default budget based on category type
                val defaultBudget = when (dbCategory.type) {
                    "Income" -> 0.0  // Income categories don't need budgets
                    else -> 100.0    // Default R100 for new expense categories
                }

                BudgetCategory(
                    name = dbCategory.name,
                    budgeted = defaultBudget,
                    spent = spent
                )
            }
        }.filter { it.budgeted > 0 }  // Only show categories with budgets > 0
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

        val safeDaysLeft = if (daysLeft < 1) 1 else daysLeft
        val dailyBudget = if (remaining > 0) remaining / safeDaysLeft else 0.0

        // --- UPDATE UI ELEMENTS ---
        binding.tvBudgetAmount.text = "R ${"%.2f".format(totalBudget)}"
        binding.tvMonthSpent.text = "R ${"%.2f".format(spent)}"
        binding.tvBudgetRemaining.text = "R ${"%.2f".format(remaining)}"

        binding.budgetProgressBar.progress = percentage.coerceIn(0, 100)
        binding.tvBudgetPercentage.text = "$percentage%"

        val (statusText, statusColor, statusBg) = when {
            percentage >= 100 -> Triple("Budget exceeded!", R.color.destructive, R.color.error)
            percentage >= 85 -> Triple("Slow down, you're almost out!", R.color.warning, R.color.warning_light)
            else -> Triple("You're on track! Keep it up.", R.color.success, R.color.success_light)
        }

        binding.tvBudgetStatus.text = statusText
        binding.tvBudgetStatus.setTextColor(requireContext().getColor(statusColor))
        binding.tvBudgetStatus.setBackgroundColor(requireContext().getColor(statusBg))

        binding.tvDaysLeft.text = daysLeft.toString()
        binding.tvDailyBudget.text = "R ${"%.0f".format(dailyBudget)}"

        if (remaining < 0) {
            binding.tvBudgetRemaining.setTextColor(requireContext().getColor(R.color.destructive))
        } else {
            binding.tvBudgetRemaining.setTextColor(requireContext().getColor(R.color.success))
        }
    }

    private fun calculateTotalBudget(categories: List<BudgetCategory>): Double {
        return categories.sumOf { it.budgeted }
    }

    private fun updateCategoryBudget(
        categoryName: String,
        newAmount: Double,
        transactions: List<za.ac.iie.TallyUp.data.Transaction>
    ) {
        // ✅ STEP 1: Update local state
        val state = repository.loadAppState()
        val updatedCategories = state.budgetCategories.toMutableList()
        val existingIndex = updatedCategories.indexOfFirst { it.name == categoryName }

        if (existingIndex != -1) {
            // Update existing budget
            updatedCategories[existingIndex] = updatedCategories[existingIndex].copy(budgeted = newAmount)
            Log.d(TAG, "📝 Updated budget for '$categoryName': R$newAmount")
        } else {
            // Create new budget entry
            val spent = transactions
                .filter { it.type == "Expense" && it.category == categoryName }
                .sumOf { it.amount }
            updatedCategories.add(BudgetCategory(categoryName, newAmount, spent))
            Log.d(TAG, "➕ Created new budget for '$categoryName': R$newAmount")
        }

        val updatedState = state.copy(budgetCategories = updatedCategories)
        repository.saveAppState(updatedState)

        // ✅ STEP 2: Sync to Firebase
        lifecycleScope.launch {
            try {
                val result = firebaseRepo.saveBudgetCategories(updatedCategories)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Synced budget update to Firebase")
                } else {
                    Log.e(TAG, "❌ Failed to sync to Firebase: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing budget to Firebase: ${e.message}", e)
            }
        }

        // ✅ STEP 3: Refresh UI
        loadBudgetData()
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", "") ?: ""

        if (userId.isEmpty()) {
            Log.e(TAG, "❌ No userId found in SharedPreferences!")
        }

        return userId
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