@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.adapters.TransactionAdapter
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.data.AppRepository
import za.ac.iie.TallyUp.data.Transaction
import za.ac.iie.TallyUp.databinding.FragmentDashboardBinding
import za.ac.iie.TallyUp.models.AppState
import za.ac.iie.TallyUp.models.Goal
import za.ac.iie.TallyUp.models.GoalDatabase
import za.ac.iie.TallyUp.utils.CharacterManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: AppRepository
    private lateinit var appState: AppState
    private lateinit var goalDatabase: GoalDatabase
    private lateinit var appDatabase: AppDatabase

    private val goalsList = mutableListOf<Goal>()
    private lateinit var goalAdapter: GoalAdapter

    companion object {
        private const val TAG = "DashboardFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        repository = AppRepository(requireContext())
        goalDatabase = GoalDatabase.getDatabase(requireContext())
        appDatabase = AppDatabase.getDatabase(requireContext())
        appState = repository.loadAppState()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGoalRecyclerView()
        setupUI()
        setupQuickActions()

        refreshDashboard()
    }

    private fun refreshDashboard() {
        loadGoalsFromDatabase()
        loadTransactionsAndUpdateBudget()
        loadRecentTransactions()
    }

    private fun setupGoalRecyclerView() {
        goalAdapter = GoalAdapter(
            goalsList,
            onAddMoneyClicked = { navigateToGoalsFragment() },
            onCompleteGoalClicked = { navigateToGoalsFragment() },
            onEditGoalClicked = { navigateToGoalsFragment() },
            onDeleteGoalClicked = { navigateToGoalsFragment() }
        )

        binding.goalsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalAdapter
        }
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext()
            .getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)

        return prefs.getString("userId", "") ?: ""
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    @SuppressLint("SetTextI18n")
    private fun loadRecentTransactions() {
        val userId = getCurrentUserId()

        lifecycleScope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    appDatabase.transactionDao()
                        .getTransactionsForUser(userId)
                }

                val recent = transactions
                    .sortedByDescending { it.date }
                    .take(3)

                if (recent.isEmpty()) {
                    binding.noTransactionsText.visibility = View.VISIBLE
                    binding.recentTransactionsContainer.visibility = View.GONE
                    return@launch
                }

                binding.noTransactionsText.visibility = View.GONE
                binding.recentTransactionsContainer.visibility = View.VISIBLE
                binding.recentTransactionsContainer.removeAllViews()

                recent.forEach { transaction ->
                    val itemView = layoutInflater.inflate(
                        R.layout.item_transaction,
                        binding.recentTransactionsContainer,
                        false
                    )

                    itemView.findViewById<TextView>(R.id.transaction_description).text =
                        "${transaction.category} • ${transaction.type}"

                    itemView.findViewById<TextView>(R.id.transaction_date).text =
                        formatDate(transaction.date)

                    itemView.findViewById<TextView>(R.id.transaction_amount).text =
                        "R ${"%.2f".format(transaction.amount)}"

                    itemView.findViewById<TextView>(R.id.photo_status).text =
                        if (transaction.photoUris.isNotEmpty())
                            "Photos attached"
                        else
                            "No photos"

                    binding.recentTransactionsContainer.addView(itemView)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load recent transactions", e)
                binding.noTransactionsText.visibility = View.VISIBLE
                binding.recentTransactionsContainer.visibility = View.GONE
            }
        }
    }

    private fun loadGoalsFromDatabase() {
        val userId = getCurrentUserId()

        lifecycleScope.launch {
            val goals = withContext(Dispatchers.IO) {
                goalDatabase.goalDao().getGoalsByUser(userId)
            }

            goalsList.clear()
            goalsList.addAll(goals.take(2))

            goalAdapter.notifyDataSetChanged()
            updateGoalsVisibility()
        }
    }

    private fun updateGoalsVisibility() {
        val hasGoals = goalsList.isNotEmpty()

        binding.noGoalsText.visibility = if (hasGoals) View.GONE else View.VISIBLE
        binding.goalsRecyclerView.visibility = if (hasGoals) View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun loadTransactionsAndUpdateBudget() {
        val userId = getCurrentUserId()

        lifecycleScope.launch {
            val transactions = withContext(Dispatchers.IO) {
                appDatabase.transactionDao()
                    .getTransactionsForUser(userId)
            }

            val totalExpenses =
                transactions.filter { it.type == "Expense" }
                    .sumOf { it.amount }

            val monthlyBudget =
                appState.budgetCategories.sumOf { it.budgeted }

            val available = monthlyBudget - totalExpenses

            val progress = if (monthlyBudget > 0) {
                ((totalExpenses / monthlyBudget) * 100)
                    .toInt()
                    .coerceIn(0, 100)
            } else 0

            updateBudgetUI(available, progress)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBudgetUI(available: Double, progress: Int) {
        binding.availableAmount.text = "R${"%.2f".format(available)}"
        binding.progressText.text = "$progress%"

        val ctx = requireContext()

        when {
            progress < 60 -> {
                binding.statusText.text = "On Track!"
                binding.statusText.setBackgroundColor(ctx.getColor(R.color.success_light))
                binding.statusText.setTextColor(ctx.getColor(R.color.success))
            }
            progress < 80 -> {
                binding.statusText.text = "Watch It!"
                binding.statusText.setBackgroundColor(ctx.getColor(R.color.warning_light))
                binding.statusText.setTextColor(ctx.getColor(R.color.warning))
            }
            else -> {
                binding.statusText.text = "Almost There!"
                binding.statusText.setBackgroundColor(ctx.getColor(R.color.error))
                binding.statusText.setTextColor(ctx.getColor(R.color.error))
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        val prefs = requireContext()
            .getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)

        val firstName = prefs.getString("userFirstName", "User")
        val characterName = CharacterManager.getCharacterName(requireContext())

        binding.welcomeText.text = "Hey $firstName! $characterName is here to help!"
        binding.characterImage.setImageResource(
            CharacterManager.getCharacterDrawable(requireContext())
        )
        binding.coinsText.text =
            CharacterManager.getCoins(requireContext()).toString()
    }

    private fun setupQuickActions() {

        // ADD EXPENSE → Transactions
        binding.addExpenseCard.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddTransactionFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.budgetDashboardCard.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BudgetFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.insightsCard.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, InsightsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.goalsSection.setOnClickListener {
            navigateToGoalsFragment()
        }
    }

    private fun navigateToGoalsFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GoalsFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
